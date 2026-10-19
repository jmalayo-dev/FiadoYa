package pe.edu.cibertec.fiadoya.clientes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.clientes.dto.CargoDto;
import pe.edu.cibertec.fiadoya.clientes.dto.CargoInput;
import pe.edu.cibertec.fiadoya.clientes.dto.PagoDto;
import pe.edu.cibertec.fiadoya.clientes.dto.PagoInput;
import pe.edu.cibertec.fiadoya.clientes.entity.Cargo;
import pe.edu.cibertec.fiadoya.clientes.entity.Cliente;
import pe.edu.cibertec.fiadoya.clientes.entity.MetodoPago;
import pe.edu.cibertec.fiadoya.clientes.entity.MovimientoCuenta;
import pe.edu.cibertec.fiadoya.clientes.entity.Pago;
import pe.edu.cibertec.fiadoya.clientes.repository.CargoRepository;
import pe.edu.cibertec.fiadoya.clientes.repository.ClienteRepository;
import pe.edu.cibertec.fiadoya.clientes.repository.MovimientoCuentaRepository;
import pe.edu.cibertec.fiadoya.clientes.repository.PagoRepository;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.Dinero;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.contracts.ClienteLimiteCercano;
import pe.edu.cibertec.fiadoya.contracts.PagoRegistrado;

/**
 * Movimientos de la cuenta: cargos de ventas fiadas (saga), su anulación (compensación) y pagos (HU05, HU06).
 * El saldo cambia en la misma transacción que el cargo o el pago, con la fila del cliente bloqueada.
 */
@Service
public class CuentaService {

    private final ClienteRepository clientes;
    private final CargoRepository cargos;
    private final PagoRepository pagos;
    private final MovimientoCuentaRepository movimientos;
    private final ApplicationEventPublisher eventos;
    private final BigDecimal umbral;

    public CuentaService(ClienteRepository clientes, CargoRepository cargos, PagoRepository pagos,
                         MovimientoCuentaRepository movimientos, ApplicationEventPublisher eventos,
                         @Value("${fiadoya.alertas.umbral-limite:0.90}") BigDecimal umbral) {
        this.clientes = clientes;
        this.cargos = cargos;
        this.pagos = pagos;
        this.movimientos = movimientos;
        this.eventos = eventos;
        this.umbral = umbral;
    }

    /** B3: cliente activo, con consentimiento y saldo + monto &lt;= límite. Repetir el mismo ventaId devuelve el mismo cargo. */
    @Transactional
    public CargoDto cargar(Long clienteId, CargoInput in) {
        var existente = cargos.findByVentaId(in.ventaId());
        if (existente.isPresent()) {
            return CargoDto.de(existente.get());
        }
        Cliente c = clientes.bloquear(clienteId).orElseThrow(() -> ApiException.noEncontrado("Cliente", clienteId));
        validarParaFiar(c);
        BigDecimal monto = Dinero.redondear(in.monto());
        BigDecimal saldoAntes = c.getSaldo();
        if (saldoAntes.add(monto).compareTo(c.getLimiteCredito()) > 0) {
            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("saldo", saldoAntes);
            extra.put("limite", c.getLimiteCredito());
            extra.put("monto", monto);
            extra.put("disponible", c.disponible());
            throw new ApiException(ErrorCode.CLIENTE_LIMITE_EXCEDIDO, "El cliente supera su límite.", extra);
        }
        c.cargar(monto);
        Cargo cargo = cargos.save(new Cargo(c.getId(), in.ventaId(), monto, in.descripcion(), in.cantidadItems()));
        movimientos.save(MovimientoCuenta.cargo(c, cargo));
        if (cruzaUmbral(saldoAntes, c.getSaldo(), c.getLimiteCredito())) {
            eventos.publishEvent(ClienteLimiteCercano.nuevo(c.getId(), c.getNombre(), c.getSaldo(), c.getLimiteCredito()));
        }
        return CargoDto.de(cargo);
    }

    /** Compensación de la saga: anula el cargo y baja el saldo. Idempotente. */
    @Transactional
    public void anular(Long ventaId) {
        cargos.findByVentaId(ventaId).filter(cargo -> !cargo.isAnulado()).ifPresent(cargo -> {
            Cliente c = clientes.bloquear(cargo.getClienteId()).orElseThrow();
            cargo.anular();
            c.abonar(cargo.getMonto());
            movimientos.save(MovimientoCuenta.anulacion(c, cargo));
        });
    }

    /** B6: 0 &lt; monto &lt;= saldo. Sin intereses ni mora. */
    @Transactional
    public PagoDto pagar(Long clienteId, PagoInput in) {
        Cliente c = clientes.bloquear(clienteId).orElseThrow(() -> ApiException.noEncontrado("Cliente", clienteId));
        if (!c.isActivo()) {
            throw new ApiException(ErrorCode.CLIENTE_INACTIVO, c.getNombre() + " está desactivado.");
        }
        BigDecimal monto = Dinero.redondear(in.monto());
        if (monto.compareTo(c.getSaldo()) > 0) {
            throw new ApiException(ErrorCode.PAGO_EXCEDE_SALDO, "El pago supera la deuda actual.",
                    Map.of("saldo", c.getSaldo(), "monto", monto));
        }
        MetodoPago metodo = in.metodo() == null ? MetodoPago.EFECTIVO : in.metodo();
        c.abonar(monto);
        Pago pago = pagos.save(new Pago(c.getId(), monto, metodo));
        movimientos.save(MovimientoCuenta.pago(c, pago));
        eventos.publishEvent(PagoRegistrado.nuevo(pago.getId(), c.getId(), monto, c.getSaldo(), metodo.name()));
        return new PagoDto(pago.getId(), c.getId(), monto, metodo.name(), pago.getFecha(), c.getSaldo());
    }

    private static void validarParaFiar(Cliente c) {
        if (!c.isActivo()) {
            throw new ApiException(ErrorCode.CLIENTE_INACTIVO, c.getNombre() + " está desactivado.");
        }
        if (!c.isConsentimiento()) {
            throw new ApiException(ErrorCode.CLIENTE_SIN_CONSENTIMIENTO, c.getNombre() + " no autorizó el registro de su deuda.");
        }
    }

    /** E4: avisa solo al cruzar el umbral (antes &lt; 90 % y después &gt;= 90 %), igual que R9. */
    public boolean cruzaUmbral(BigDecimal antes, BigDecimal despues, BigDecimal limite) {
        if (limite.signum() <= 0) {
            return false;
        }
        BigDecimal usoAntes = antes.divide(limite, 4, RoundingMode.HALF_UP);
        BigDecimal usoDespues = despues.divide(limite, 4, RoundingMode.HALF_UP);
        return usoAntes.compareTo(umbral) < 0 && usoDespues.compareTo(umbral) >= 0;
    }
}
