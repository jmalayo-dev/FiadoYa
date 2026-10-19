package pe.edu.cibertec.fiadoya.contracts;

import java.time.Instant;
import java.util.UUID;

/** Sobre común de todos los eventos: eventId, ocurridoEn, version y tipo. */
public sealed interface Evento permits VentaRegistrada, StockBajo, PagoRegistrado, ClienteLimiteCercano {

    UUID eventId();

    Instant ocurridoEn();

    int version();

    String tipo();
}
