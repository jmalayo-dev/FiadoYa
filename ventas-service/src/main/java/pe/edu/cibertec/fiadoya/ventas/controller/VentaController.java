package pe.edu.cibertec.fiadoya.ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.fiadoya.common.web.PageResponse;
import pe.edu.cibertec.fiadoya.ventas.dto.VentaDto;
import pe.edu.cibertec.fiadoya.ventas.dto.VentaInput;
import pe.edu.cibertec.fiadoya.ventas.entity.EstadoVenta;
import pe.edu.cibertec.fiadoya.ventas.service.ConsultaVentas;
import pe.edu.cibertec.fiadoya.ventas.service.SagaVentas;

@RestController
@RequestMapping("/api/ventas")
@PreAuthorize("hasRole('BODEGUERO')")
@Tag(name = "Ventas", description = "HU04 y HU05: venta al contado o al fiado (saga orquestada)")
public class VentaController {

    private final SagaVentas saga;
    private final ConsultaVentas consulta;

    public VentaController(SagaVentas saga, ConsultaVentas consulta) {
        this.saga = saga;
        this.consulta = consulta;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una venta. Exige Idempotency-Key (UUID); una venta RECHAZADA responde 409")
    public VentaDto registrar(
            @Parameter(description = "UUID único por intento de venta") @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody VentaInput input) {
        return saga.registrar(clave, input);
    }

    @GetMapping
    public PageResponse<VentaDto> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return consulta.listar(desde, hasta, estado, clienteId, page, size);
    }

    @GetMapping("/{id}")
    public VentaDto obtener(@PathVariable Long id) {
        return consulta.obtener(id);
    }
}
