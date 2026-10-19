package pe.edu.cibertec.fiadoya.ventas.service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.Fechas;
import pe.edu.cibertec.fiadoya.common.web.PageResponse;
import pe.edu.cibertec.fiadoya.common.web.Paginacion;
import pe.edu.cibertec.fiadoya.ventas.dto.VentaDto;
import pe.edu.cibertec.fiadoya.ventas.entity.EstadoVenta;
import pe.edu.cibertec.fiadoya.ventas.entity.Venta;
import pe.edu.cibertec.fiadoya.ventas.repository.VentaRepository;

/** HU04: historial de ventas. Los días se interpretan en America/Lima (B10). */
@Service
public class ConsultaVentas {

    private final VentaRepository ventas;

    public ConsultaVentas(VentaRepository ventas) {
        this.ventas = ventas;
    }

    @Transactional(readOnly = true)
    public PageResponse<VentaDto> listar(LocalDate desde, LocalDate hasta, EstadoVenta estado, Long clienteId,
                                         Integer page, Integer size) {
        Specification<Venta> d = desde == null ? null
                : (r, q, cb) -> cb.greaterThanOrEqualTo(r.get("fecha"), Fechas.inicioDelDia(desde));
        Specification<Venta> h = hasta == null ? null
                : (r, q, cb) -> cb.lessThan(r.get("fecha"), Fechas.finDelDia(hasta));
        Specification<Venta> e = estado == null ? null : (r, q, cb) -> cb.equal(r.get("estado"), estado);
        Specification<Venta> c = clienteId == null ? null : (r, q, cb) -> cb.equal(r.get("clienteId"), clienteId);
        Specification<Venta> spec = Stream.of(d, h, e, c).filter(Objects::nonNull).reduce(Specification::and)
                .orElse((r, q, cb) -> cb.conjunction());
        var pagina = ventas.findAll(spec, Paginacion.de(page, size, Sort.by(Sort.Direction.DESC, "fecha", "id")));
        return PageResponse.de(pagina, VentaDto::de);
    }

    @Transactional(readOnly = true)
    public VentaDto obtener(Long id) {
        return ventas.findWithItemsById(id).map(VentaDto::de).orElseThrow(() -> ApiException.noEncontrado("Venta", id));
    }
}
