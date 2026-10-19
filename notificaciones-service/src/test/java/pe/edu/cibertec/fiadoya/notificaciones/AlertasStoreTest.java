package pe.edu.cibertec.fiadoya.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pe.edu.cibertec.fiadoya.contracts.ClienteLimiteCercano;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;
import pe.edu.cibertec.fiadoya.notificaciones.dto.Alerta;
import pe.edu.cibertec.fiadoya.notificaciones.service.AlertasStore;

class AlertasStoreTest {

    @Test
    void ignoraEventosRepetidos() {
        AlertasStore store = new AlertasStore(100);
        Alerta a = Alerta.de(StockBajo.nuevo(1L, "Arroz", 3, 5));
        assertThat(store.agregar(a)).isTrue();
        assertThat(store.agregar(a)).isFalse();
        assertThat(store.ultimas(50, null)).hasSize(1);
    }

    @Test
    void guardaSoloLasUltimasYMasRecientePrimero() {
        AlertasStore store = new AlertasStore(3);
        for (int i = 1; i <= 5; i++) {
            store.agregar(Alerta.de(StockBajo.nuevo((long) i, "P" + i, 1, 2)));
        }
        assertThat(store.ultimas(50, null)).extracting(Alerta::nombre).containsExactly("P5", "P4", "P3");
    }

    @Test
    void filtraPorTipo() {
        AlertasStore store = new AlertasStore(10);
        store.agregar(Alerta.de(StockBajo.nuevo(1L, "Arroz", 3, 5)));
        store.agregar(Alerta.de(ClienteLimiteCercano.nuevo(2L, "Jorge", new java.math.BigDecimal("142"), new java.math.BigDecimal("150"))));
        assertThat(store.ultimas(10, "LIMITE_CLIENTE")).singleElement()
                .satisfies(a -> assertThat(a.clienteId()).isEqualTo(2L));
    }
}
