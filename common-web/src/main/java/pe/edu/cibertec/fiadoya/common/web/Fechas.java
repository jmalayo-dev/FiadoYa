package pe.edu.cibertec.fiadoya.common.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Regla B10: se guarda en UTC; los filtros por día se interpretan en America/Lima. */
public final class Fechas {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    private Fechas() {
    }

    public static Instant inicioDelDia(LocalDate dia) {
        return dia.atStartOfDay(LIMA).toInstant();
    }

    /** Fin exclusivo: inicio del día siguiente. */
    public static Instant finDelDia(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay(LIMA).toInstant();
    }

    public static LocalDate diaEnLima(Instant instante) {
        return instante.atZone(LIMA).toLocalDate();
    }

    public static LocalDate hoy() {
        return LocalDate.now(LIMA);
    }
}
