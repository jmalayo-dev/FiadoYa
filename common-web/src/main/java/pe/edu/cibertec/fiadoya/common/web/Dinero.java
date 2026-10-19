package pe.edu.cibertec.fiadoya.common.web;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regla B1: moneda PEN, HALF_UP a 2 decimales. */
public final class Dinero {

    public static final BigDecimal CERO = new BigDecimal("0.00");

    private Dinero() {
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
