package pe.edu.cibertec.fiadoya.clientes.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.fiadoya.clientes.dto.CargoDto;
import pe.edu.cibertec.fiadoya.clientes.dto.CargoInput;
import pe.edu.cibertec.fiadoya.clientes.dto.Deudor;
import pe.edu.cibertec.fiadoya.clientes.dto.Validacion;
import pe.edu.cibertec.fiadoya.clientes.service.ClienteService;
import pe.edu.cibertec.fiadoya.clientes.service.CuentaService;

/** Endpoints servicio a servicio (Anexo §6.2). Exigen X-Internal-Key. */
@Hidden
@RestController
@RequestMapping("/internal/clientes")
public class InternalClientesController {

    private final ClienteService clientes;
    private final CuentaService cuentas;

    public InternalClientesController(ClienteService clientes, CuentaService cuentas) {
        this.clientes = clientes;
        this.cuentas = cuentas;
    }

    @PostMapping("/{id}/cargos")
    public CargoDto cargar(@PathVariable Long id, @Valid @RequestBody CargoInput input) {
        return cuentas.cargar(id, input);
    }

    @DeleteMapping("/cargos/{ventaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anular(@PathVariable Long ventaId) {
        cuentas.anular(ventaId);
    }

    @GetMapping("/{id}/validacion")
    public Validacion validacion(@PathVariable Long id) {
        return clientes.validacion(id);
    }

    @GetMapping("/deudores")
    public List<Deudor> deudores() {
        return clientes.deudores();
    }
}
