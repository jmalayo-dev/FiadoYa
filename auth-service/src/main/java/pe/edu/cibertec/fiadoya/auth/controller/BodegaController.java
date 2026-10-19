package pe.edu.cibertec.fiadoya.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.fiadoya.auth.config.AuthProperties;
import pe.edu.cibertec.fiadoya.auth.dto.Bodega;

@RestController
@Tag(name = "Bodega", description = "Anexo v1.2 E5: datos de la bodega (una por instalación)")
public class BodegaController {

    private final Bodega bodega;

    public BodegaController(AuthProperties props) {
        var b = props.bodega();
        this.bodega = new Bodega(b.nombre(), b.telefono(), b.direccion());
    }

    @GetMapping("/api/bodega")
    @Operation(summary = "Nombre, teléfono y dirección de la bodega")
    public Bodega bodega() {
        return bodega;
    }
}
