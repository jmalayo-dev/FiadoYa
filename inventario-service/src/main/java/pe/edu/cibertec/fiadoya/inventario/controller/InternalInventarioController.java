package pe.edu.cibertec.fiadoya.inventario.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.fiadoya.inventario.dto.Devolucion;
import pe.edu.cibertec.fiadoya.inventario.dto.LoteDescuento;
import pe.edu.cibertec.fiadoya.inventario.dto.ResultadoDescuento;
import pe.edu.cibertec.fiadoya.inventario.service.StockService;

/** Endpoints servicio a servicio (R1). Exigen X-Internal-Key y el gateway no los expone. */
@Hidden
@RestController
@RequestMapping("/internal/inventario")
public class InternalInventarioController {

    private final StockService stock;

    public InternalInventarioController(StockService stock) {
        this.stock = stock;
    }

    @PostMapping("/descuentos")
    public ResultadoDescuento descontar(@Valid @RequestBody LoteDescuento lote) {
        return stock.descontar(lote);
    }

    @PostMapping("/devoluciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void devolver(@Valid @RequestBody Devolucion devolucion) {
        stock.devolver(devolucion.ventaId());
    }
}
