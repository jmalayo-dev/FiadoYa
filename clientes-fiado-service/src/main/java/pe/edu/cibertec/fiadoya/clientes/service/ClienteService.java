package pe.edu.cibertec.fiadoya.clientes.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.clientes.dto.ClienteDto;
import pe.edu.cibertec.fiadoya.clientes.dto.ClienteInput;
import pe.edu.cibertec.fiadoya.clientes.dto.ClienteResumen;
import pe.edu.cibertec.fiadoya.clientes.dto.Deudor;
import pe.edu.cibertec.fiadoya.clientes.dto.EstadoCuenta;
import pe.edu.cibertec.fiadoya.clientes.dto.MovimientoDto;
import pe.edu.cibertec.fiadoya.clientes.dto.Validacion;
import pe.edu.cibertec.fiadoya.clientes.entity.Cliente;
import pe.edu.cibertec.fiadoya.clientes.repository.ClienteRepository;
import pe.edu.cibertec.fiadoya.clientes.repository.MovimientoCuentaRepository;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.Dinero;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.common.web.PageResponse;
import pe.edu.cibertec.fiadoya.common.web.Paginacion;

/** HU02, HU07 y HU09: clientes, estado de cuenta y resumen de deuda. */
@Service
public class ClienteService {

    private final ClienteRepository clientes;
    private final MovimientoCuentaRepository movimientos;

    public ClienteService(ClienteRepository clientes, MovimientoCuentaRepository movimientos) {
        this.clientes = clientes;
        this.movimientos = movimientos;
    }

    /** orden: reciente (por defecto, B11), deuda (mayor saldo primero) o nombre. */
    @Transactional(readOnly = true)
    public PageResponse<ClienteDto> listar(String q, Boolean conDeuda, String orden, Integer page, Integer size) {
        Specification<Cliente> activos = (r, cq, cb) -> cb.isTrue(r.get("activo"));
        Specification<Cliente> texto = q == null || q.isBlank() ? null : (r, cq, cb) -> {
            String patron = "%" + q.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(r.get("nombre")), patron), cb.like(r.get("telefono"), patron),
                    cb.like(r.get("dni"), patron));
        };
        Specification<Cliente> deuda = Boolean.TRUE.equals(conDeuda)
                ? (r, cq, cb) -> cb.greaterThan(r.get("saldo"), BigDecimal.ZERO) : null;
        Specification<Cliente> spec = Stream.of(activos, texto, deuda).filter(Objects::nonNull)
                .reduce(Specification::and).orElseThrow();
        Sort sort = switch (orden == null ? "reciente" : orden) {
            case "deuda" -> Sort.by(Sort.Direction.DESC, "saldo").and(Sort.by("nombre"));
            case "nombre" -> Sort.by("nombre");
            default -> Sort.by(Sort.Direction.DESC, "creadoEn", "id");
        };
        return PageResponse.de(clientes.findAll(spec, Paginacion.de(page, size, sort)), ClienteDto::de);
    }

    @Transactional(readOnly = true)
    public ClienteDto obtener(Long id) {
        return ClienteDto.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public ClienteResumen resumen() {
        return new ClienteResumen(clientes.countByActivoTrue(),
                clientes.countByActivoTrueAndSaldoGreaterThan(BigDecimal.ZERO),
                Dinero.redondear(clientes.deudaTotal()));
    }

    @Transactional
    public ClienteDto crear(ClienteInput in) {
        String dni = vacioANull(in.dni());
        if (dni != null && clientes.existsByDni(dni)) {
            throw dniDuplicado(dni);
        }
        if (in.usuarioId() != null && clientes.existsByUsuarioId(in.usuarioId())) {
            throw ApiException.validacion("usuarioId", "ese usuario ya está vinculado a otro cliente");
        }
        Cliente c = new Cliente(in.nombre().trim(), in.telefono(), dni, vacioANull(in.direccion()),
                Dinero.redondear(in.limiteCredito()), in.consentimiento(), in.usuarioId());
        return ClienteDto.de(clientes.save(c));
    }

    @Transactional
    public ClienteDto actualizar(Long id, ClienteInput in) {
        Cliente c = buscar(id);
        String dni = vacioANull(in.dni());
        if (dni != null && clientes.existsByDniAndIdNot(dni, id)) {
            throw dniDuplicado(dni);
        }
        if (in.usuarioId() != null && clientes.existsByUsuarioIdAndIdNot(in.usuarioId(), id)) {
            throw ApiException.validacion("usuarioId", "ese usuario ya está vinculado a otro cliente");
        }
        c.actualizar(in.nombre().trim(), in.telefono(), dni, vacioANull(in.direccion()),
                Dinero.redondear(in.limiteCredito()), in.consentimiento(), in.usuarioId());
        return ClienteDto.de(c);
    }

    /** B7: no se desactiva a un cliente con deuda. */
    @Transactional
    public void desactivar(Long id) {
        Cliente c = buscar(id);
        if (c.getSaldo().signum() > 0) {
            throw new ApiException(ErrorCode.CLIENTE_CON_SALDO, c.getNombre() + " todavía debe S/ " + c.getSaldo() + ".",
                    Map.of("saldo", c.getSaldo()));
        }
        c.desactivar();
    }

    @Transactional(readOnly = true)
    public EstadoCuenta estadoCuenta(Long id) {
        return estadoDe(buscar(id));
    }

    /** HU07/HU12: el cliente ve solo su propia cuenta (cliente.usuario_id = sub del JWT). */
    @Transactional(readOnly = true)
    public EstadoCuenta miCuenta(Long usuarioId) {
        Cliente c = clientes.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Tu usuario no está vinculado a una cuenta de fiado."));
        return estadoDe(c);
    }

    @Transactional(readOnly = true)
    public Validacion validacion(Long id) {
        return clientes.findById(id)
                .map(c -> new Validacion(true, c.isActivo(), c.isConsentimiento(), c.getNombre()))
                .orElse(new Validacion(false, false, false, null));
    }

    @Transactional(readOnly = true)
    public List<Deudor> deudores() {
        return clientes.findBySaldoGreaterThanOrderBySaldoDesc(BigDecimal.ZERO).stream()
                .map(c -> new Deudor(c.getId(), c.getNombre(), c.getTelefono(), c.getSaldo(), c.getLimiteCredito()))
                .toList();
    }

    private EstadoCuenta estadoDe(Cliente c) {
        List<MovimientoDto> lista = movimientos.findTop200ByClienteIdOrderByFechaDescIdDesc(c.getId()).stream()
                .map(MovimientoDto::de).toList();
        return new EstadoCuenta(ClienteDto.de(c), lista);
    }

    Cliente buscar(Long id) {
        return clientes.findById(id).orElseThrow(() -> ApiException.noEncontrado("Cliente", id));
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ApiException dniDuplicado(String dni) {
        return new ApiException(ErrorCode.DNI_DUPLICADO, "Ya existe un cliente con DNI " + dni + ".", Map.of("dni", dni));
    }
}
