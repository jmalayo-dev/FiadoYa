package pe.edu.cibertec.fiadoya.distribuidora;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Catálogo simulado en memoria con los mismos productos de la demo (código P + id de 4 dígitos). */
@Component
public class CatalogoDistribuidora {

    public record Articulo(String codigo, String nombre, BigDecimal precio, int disponible) {
    }

    private static final List<Articulo> ARTICULOS = List.of(
            new Articulo("P0001", "Arroz extra 1 kg", new BigDecimal("3.80"), 500),
            new Articulo("P0002", "Aceite vegetal 1 L", new BigDecimal("8.90"), 200),
            new Articulo("P0003", "Azúcar rubia 1 kg", new BigDecimal("3.50"), 400),
            new Articulo("P0004", "Fideos spaghetti 500 g", new BigDecimal("2.30"), 300),
            new Articulo("P0005", "Leche evaporada 400 g", new BigDecimal("3.40"), 600),
            new Articulo("P0006", "Atún en lata 170 g", new BigDecimal("5.20"), 250),
            new Articulo("P0007", "Galletas de soda 6 pack", new BigDecimal("1.60"), 350),
            new Articulo("P0008", "Gaseosa 1.5 L", new BigDecimal("5.50"), 180),
            new Articulo("P0009", "Huevos x 15", new BigDecimal("8.50"), 120),
            new Articulo("P0010", "Pan de molde 600 g", new BigDecimal("6.90"), 90),
            new Articulo("P0011", "Detergente 900 g", new BigDecimal("9.80"), 150),
            new Articulo("P0012", "Papel higiénico x 4", new BigDecimal("5.90"), 220));

    public List<Articulo> todos() {
        return ARTICULOS;
    }

    public Optional<Articulo> porCodigo(String codigo) {
        return ARTICULOS.stream().filter(a -> a.codigo().equalsIgnoreCase(codigo)).findFirst();
    }
}
