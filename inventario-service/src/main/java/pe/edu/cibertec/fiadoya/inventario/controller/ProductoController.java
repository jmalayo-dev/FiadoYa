package pe.edu.cibertec.fiadoya.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import pe.edu.cibertec.fiadoya.common.web.PageResponse;
import pe.edu.cibertec.fiadoya.inventario.dto.AjusteStock;
import pe.edu.cibertec.fiadoya.inventario.dto.ProductoDto;
import pe.edu.cibertec.fiadoya.inventario.dto.ProductoInput;
import pe.edu.cibertec.fiadoya.inventario.entity.Categoria;
import pe.edu.cibertec.fiadoya.inventario.service.ProductoService;

@RestController
@RequestMapping("/api/productos")
@PreAuthorize("hasRole('BODEGUERO')")
@Tag(name = "Productos", description = "HU03: catálogo y stock")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista productos activos (q busca en nombre y código de barras)")
    public PageResponse<ProductoDto> listar(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) Boolean soloBajos,
                                            @RequestParam(required = false) Categoria categoria,
                                            @RequestParam(required = false) String codigo,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return service.listar(q, soloBajos, categoria, codigo, page, size);
    }

    @GetMapping("/{id}")
    public ProductoDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDto crear(@Valid @RequestBody ProductoInput input) {
        return service.crear(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza datos del producto (el stock se ignora: usar /ajuste)")
    public ProductoDto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoInput input) {
        return service.actualizar(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Baja lógica")
    public void darDeBaja(@PathVariable Long id) {
        service.darDeBaja(id);
    }

    @PostMapping("/{id}/ajuste")
    @Operation(summary = "Ajuste manual de stock con motivo")
    public ProductoDto ajustar(@PathVariable Long id, @Valid @RequestBody AjusteStock ajuste) {
        return service.ajustar(id, ajuste);
    }
}
