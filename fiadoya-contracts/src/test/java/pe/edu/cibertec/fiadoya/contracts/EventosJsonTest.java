package pe.edu.cibertec.fiadoya.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Verifica que los eventos viajan como JSON y vuelven al mismo record (serialización JSON del sílabo 5401). */
class EventosJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void ventaRegistradaIdaYVuelta() {
        var evento = VentaRegistrada.nueva(12L, 3L, "FIADO", new BigDecimal("35.50"),
                List.of(new VentaRegistrada.Item(1L, "Arroz 1kg", 2, new BigDecimal("4.50"))));

        String json = mapper.writeValueAsString(evento);
        var leido = mapper.readValue(json, VentaRegistrada.class);

        assertThat(json).contains("\"tipo\":\"VentaRegistrada\"").contains("\"ventaId\":12");
        assertThat(leido).isEqualTo(evento);
    }

    @Test
    void stockBajoUsaFechaIso() {
        var evento = StockBajo.nuevo(1L, "Arroz 1kg", 3, 5);
        String json = mapper.writeValueAsString(evento);
        assertThat(json).containsPattern("\"ocurridoEn\":\"\\d{4}-\\d{2}-\\d{2}T");
        assertThat(mapper.readValue(json, StockBajo.class)).isEqualTo(evento);
    }
}
