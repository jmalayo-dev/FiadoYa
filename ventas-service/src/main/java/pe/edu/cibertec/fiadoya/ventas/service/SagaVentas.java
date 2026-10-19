package pe.edu.cibertec.fiadoya.ventas.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.ventas.client.ClientesClient;
import pe.edu.cibertec.fiadoya.ventas.client.InventarioClient;
import pe.edu.cibertec.fiadoya.ventas.dto.ItemInput;
import pe.edu.cibertec.fiadoya.ventas.dto.VentaDto;
import pe.edu.cibertec.fiadoya.ventas.dto.VentaInput;
import pe.edu.cibertec.fiadoya.ventas.entity.EstadoVenta;
import pe.edu.cibertec.fiadoya.ventas.entity.TipoPago;
import pe.edu.cibertec.fiadoya.ventas.entity.Venta;

/**
 * Flujo A: saga orquestada de una venta (Anexo §10.3).
 * <ol>
 *   <li>Idempotency-Key: misma clave y mismo cuerpo → misma respuesta; cuerpo distinto → IDEMPOTENCY_CONFLICT.</li>
 *   <li>FIADO: valida al cliente antes de tocar stock.</li>
 *   <li>Crea la venta PENDIENTE (total 0).</li>
 *   <li>Descuenta el stock por lote en inventario (Feign + circuit breaker) y congela los precios.</li>
 *   <li>FIADO: carga la deuda en clientes-fiado. Si falla, compensa devolviendo el stock y la venta queda RECHAZADA.</li>
 *   <li>Marca CONFIRMADA y publica venta.registrada.</li>
 * </ol>
 */
@Service
public class SagaVentas {

    private static final Logger log = LoggerFactory.getLogger(SagaVentas.class);

    private final PasosVenta pasos;
    private final LlamadasRemotas remotas;

    public SagaVentas(PasosVenta pasos, LlamadasRemotas remotas) {
        this.pasos = pasos;
        this.remotas = remotas;
    }

    public VentaDto registrar(String clave, VentaInput in) {
        validarClave(clave);
        validarEntrada(in);
        List<InventarioClient.Item> items = agrupar(in.items());
        String hash = hash(in, items);

        Optional<Venta> previa = pasos.buscarPorClave(clave);
        if (previa.isPresent()) {
            return repetir(previa.get(), hash);
        }

        String nombreCliente = in.tipoPago() == TipoPago.FIADO ? validarCliente(in.clienteId()) : null;

        Venta venta;
        try {
            venta = pasos.crearPendiente(in.clienteId(), nombreCliente, in.tipoPago(), clave, hash);
        } catch (DataIntegrityViolationException carrera) {
            // Otra petición con la misma clave ganó la carrera (doble clic): se responde lo mismo que a ella.
            return repetir(pasos.buscarPorClave(clave).orElseThrow(() -> carrera), hash);
        }
        Long ventaId = venta.getId();

        InventarioClient.Resultado descuento;
        try {
            descuento = remotas.descontar(ventaId, items);
        } catch (ApiException e) {
            if (e.getCode() == ErrorCode.SERVICIO_NO_DISPONIBLE) {
                // Pudo descontar antes de caerse: se intenta devolver; si no se puede, el job lo resolverá.
                compensarYRechazar(ventaId, false, e);
                throw conVenta(e, ventaId);
            }
            pasos.borrar(ventaId);
            throw e;
        }

        Venta conDetalle = pasos.registrarDetalle(ventaId, descuento);

        if (in.tipoPago() == TipoPago.FIADO) {
            try {
                remotas.cargar(in.clienteId(), new ClientesClient.CargoInput(ventaId, conDetalle.getTotal(),
                        describir(descuento), descuento.items().size()));
            } catch (ApiException e) {
                // Con 503 el cargo pudo guardarse antes del timeout: se intenta anularlo también.
                compensarYRechazar(ventaId, e.getCode() == ErrorCode.SERVICIO_NO_DISPONIBLE, e);
                throw conVenta(e, ventaId);
            }
        }
        return VentaDto.de(pasos.confirmar(ventaId));
    }

    /**
     * Compensación: devuelve el stock y, si corresponde, intenta anular el cargo (idempotente).
     * Solo si el stock volvió la venta pasa a RECHAZADA; si no, queda PENDIENTE y el job la cierra después.
     */
    void compensarYRechazar(Long ventaId, boolean intentarAnularCargo, ApiException motivo) {
        try {
            remotas.devolver(ventaId);
        } catch (ApiException compensacion) {
            log.warn("No se pudo devolver el stock de la venta {}; queda PENDIENTE para el job de limpieza", ventaId);
            return;
        }
        if (intentarAnularCargo) {
            try {
                remotas.anularCargo(ventaId);
            } catch (ApiException noAnulado) {
                log.warn("No se pudo anular el cargo de la venta {}: {}", ventaId, noAnulado.getMessage());
            }
        }
        pasos.rechazar(ventaId, motivo.getCode().name(), motivoTexto(motivo));
    }

    private VentaDto repetir(Venta previa, String hash) {
        if (!previa.getHashCuerpo().equals(hash)) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_CONFLICT, "Esta Idempotency-Key ya se usó con otros datos.",
                    Map.of("ventaId", previa.getId()));
        }
        if (previa.getEstado() == EstadoVenta.RECHAZADA) {
            ErrorCode code = codigoGuardado(previa.getCodigoRechazo());
            throw new ApiException(code, previa.getMotivoRechazo(), Map.of("ventaId", previa.getId()));
        }
        return VentaDto.de(previa);
    }

    private String validarCliente(Long clienteId) {
        ClientesClient.Validacion v = remotas.validarCliente(clienteId);
        if (!v.existe()) {
            throw ApiException.noEncontrado("Cliente", clienteId);
        }
        if (!v.activo()) {
            throw new ApiException(ErrorCode.CLIENTE_INACTIVO, v.nombre() + " está desactivado.");
        }
        if (!v.consentimiento()) {
            throw new ApiException(ErrorCode.CLIENTE_SIN_CONSENTIMIENTO, v.nombre() + " no autorizó el registro de su deuda.");
        }
        return v.nombre();
    }

    /** B2 y B9. */
    static void validarEntrada(VentaInput in) {
        if (in.tipoPago() == TipoPago.CONTADO && in.clienteId() != null) {
            throw ApiException.validacion("clienteId", "debe ser null en una venta al contado");
        }
        if (in.tipoPago() == TipoPago.FIADO && in.clienteId() == null) {
            throw ApiException.validacion("clienteId", "es obligatorio en una venta al fiado");
        }
    }

    static void validarClave(String clave) {
        try {
            UUID.fromString(clave);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.validacion("Idempotency-Key", "debe ser un UUID");
        }
    }

    /** B9: los productos repetidos se suman; la cantidad total por producto sigue limitada a 999. */
    static List<InventarioClient.Item> agrupar(List<ItemInput> items) {
        Map<Long, Integer> suma = new TreeMap<>();
        items.forEach(i -> suma.merge(i.productoId(), i.cantidad(), Integer::sum));
        suma.forEach((id, cant) -> {
            if (cant > 999) {
                throw ApiException.validacion("items", "la cantidad del producto " + id + " supera 999");
            }
        });
        return suma.entrySet().stream().map(e -> new InventarioClient.Item(e.getKey(), e.getValue())).toList();
    }

    static String hash(VentaInput in, List<InventarioClient.Item> items) {
        String canonico = in.tipoPago() + "|" + in.clienteId() + "|"
                + items.stream().map(i -> i.productoId() + ":" + i.cantidad()).collect(Collectors.joining(","));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonico.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** "Arroz extra 1 kg, Aceite vegetal 1 L, Leche y 2 más" (Anexo v1.2 E6). */
    static String describir(InventarioClient.Resultado r) {
        List<String> nombres = r.items().stream().map(InventarioClient.ItemPrecio::nombre).toList();
        String base = String.join(", ", nombres.subList(0, Math.min(3, nombres.size())));
        String texto = nombres.size() > 3 ? base + " y " + (nombres.size() - 3) + " más" : base;
        return texto.length() > 160 ? texto.substring(0, 157) + "..." : texto;
    }

    private static String motivoTexto(ApiException e) {
        Object disponible = e.getExtra().get("disponible");
        return switch (e.getCode()) {
            case CLIENTE_LIMITE_EXCEDIDO -> "Supera el límite de crédito. Disponible: S/ " + formatear(disponible);
            case SERVICIO_NO_DISPONIBLE -> "Servicio no disponible: " + e.getExtra().getOrDefault("servicio", "");
            default -> e.getCode().titulo() + ": " + e.getMessage();
        };
    }

    private static String formatear(Object monto) {
        return monto instanceof Number n ? new BigDecimal(n.toString()).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                : String.valueOf(monto);
    }

    private static ApiException conVenta(ApiException e, Long ventaId) {
        Map<String, Object> extra = new LinkedHashMap<>(e.getExtra());
        extra.put("ventaId", ventaId);
        return new ApiException(e.getCode(), e.getMessage(), extra);
    }

    private static ErrorCode codigoGuardado(String codigo) {
        try {
            return ErrorCode.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ErrorCode.SERVICIO_NO_DISPONIBLE;
        }
    }
}
