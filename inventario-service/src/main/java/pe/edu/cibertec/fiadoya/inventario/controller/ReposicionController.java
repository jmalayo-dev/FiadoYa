package pe.edu.cibertec.fiadoya.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.fiadoya.common.web.PageResponse;
import pe.edu.cibertec.fiadoya.inventario.dto.CatalogoItem;
import pe.edu.cibertec.fiadoya.inventario.dto.ReposicionDto;
import pe.edu.cibertec.fiadoya.inventario.dto.ReposicionInput;
import pe.edu.cibertec.fiadoya.inventario.service.ReposicionService;

@RestController
@RequestMapping("/api/reposiciones")
@PreAuthorize("hasRole('BODEGUERO')")
@Tag(name = "Reposiciones", description = "HU11: pedidos a la distribuidora por SOAP")
public class ReposicionController {

    private final ReposicionService service;

    public ReposicionController(ReposicionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ReposicionDto> listar(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        return service.listar(page, size);
    }

    @GetMapping("/catalogo")
    @Operation(summary = "Catálogo de la distribuidora (SOAP consultarCatalogo)")
    public List<CatalogoItem> catalogo() {
        return service.catalogo();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un pedido en la distribuidora (SOAP registrarPedido)")
    public ReposicionDto crear(@Valid @RequestBody ReposicionInput input) {
        return service.crear(input);
    }

    @PostMapping("/{id}/recibir")
    @Operation(summary = "Marca el pedido como recibido y suma el stock")
    public ReposicionDto recibir(@PathVariable Long id) {
        return service.recibir(id);
    }
}
