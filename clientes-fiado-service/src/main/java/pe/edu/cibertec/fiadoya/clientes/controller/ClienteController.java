package pe.edu.cibertec.fiadoya.clientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.fiadoya.clientes.dto.ClienteDto;
import pe.edu.cibertec.fiadoya.clientes.dto.ClienteInput;
import pe.edu.cibertec.fiadoya.clientes.dto.ClienteResumen;
import pe.edu.cibertec.fiadoya.clientes.dto.EstadoCuenta;
import pe.edu.cibertec.fiadoya.clientes.dto.PagoDto;
import pe.edu.cibertec.fiadoya.clientes.dto.PagoInput;
import pe.edu.cibertec.fiadoya.clientes.service.ClienteService;
import pe.edu.cibertec.fiadoya.clientes.service.CuentaService;
import pe.edu.cibertec.fiadoya.common.security.UsuarioActual;
import pe.edu.cibertec.fiadoya.common.web.PageResponse;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "HU02, HU06, HU07 y HU09: clientes, pagos y estado de cuenta")
public class ClienteController {

    private final ClienteService clientes;
    private final CuentaService cuentas;

    public ClienteController(ClienteService clientes, CuentaService cuentas) {
        this.clientes = clientes;
        this.cuentas = cuentas;
    }

    @GetMapping
    @PreAuthorize("hasRole('BODEGUERO')")
    @Operation(summary = "Lista clientes activos (q busca en nombre, teléfono y DNI; orden=reciente|deuda|nombre)")
    public PageResponse<ClienteDto> listar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Boolean conDeuda,
                                           @RequestParam(required = false) String orden,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        return clientes.listar(q, conDeuda, orden, page, size);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('BODEGUERO')")
    public ClienteResumen resumen() {
        return clientes.resumen();
    }

    @GetMapping("/mi-cuenta")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Estado de cuenta del cliente que inició sesión")
    public EstadoCuenta miCuenta(Authentication auth) {
        return clientes.miCuenta(UsuarioActual.id(auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BODEGUERO')")
    public ClienteDto obtener(@PathVariable Long id) {
        return clientes.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BODEGUERO')")
    public ClienteDto crear(@Valid @RequestBody ClienteInput input) {
        return clientes.crear(input);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BODEGUERO')")
    public ClienteDto actualizar(@PathVariable Long id, @Valid @RequestBody ClienteInput input) {
        return clientes.actualizar(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('BODEGUERO')")
    @Operation(summary = "Desactiva al cliente (CLIENTE_CON_SALDO si debe)")
    public void desactivar(@PathVariable Long id) {
        clientes.desactivar(id);
    }

    @PostMapping("/{id}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BODEGUERO')")
    @Operation(summary = "Registra un pago parcial o total")
    public PagoDto pagar(@PathVariable Long id, @Valid @RequestBody PagoInput input) {
        return cuentas.pagar(id, input);
    }

    @GetMapping("/{id}/estado-cuenta")
    @PreAuthorize("hasRole('BODEGUERO')")
    public EstadoCuenta estadoCuenta(@PathVariable Long id) {
        return clientes.estadoCuenta(id);
    }
}
