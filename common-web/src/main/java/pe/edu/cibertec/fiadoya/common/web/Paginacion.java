package pe.edu.cibertec.fiadoya.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Regla B11: size por defecto 20, máximo 100, page base 0. */
public final class Paginacion {

    public static final int SIZE_DEFECTO = 20;
    public static final int SIZE_MAXIMO = 100;

    private Paginacion() {
    }

    public static Pageable de(Integer page, Integer size, Sort orden) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size <= 0 ? SIZE_DEFECTO : Math.min(size, SIZE_MAXIMO);
        return PageRequest.of(p, s, orden);
    }
}
