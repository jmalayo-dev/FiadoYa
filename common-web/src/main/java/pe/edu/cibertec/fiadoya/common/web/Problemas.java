package pe.edu.cibertec.fiadoya.common.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;

/** Construye ProblemDetail (RFC 9457) con los campos extra del contrato: code, extra y errors. */
public final class Problemas {

    private Problemas() {
    }

    public static ProblemDetail de(ErrorCode code, String detail) {
        return de(code, detail, Map.of(), List.of());
    }

    public static ProblemDetail de(ErrorCode code, String detail, Map<String, Object> extra, List<ErrorCampo> errores) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), detail);
        pd.setType(URI.create(code.tipoUri()));
        pd.setTitle(code.titulo());
        pd.setProperty("code", code.name());
        if (extra != null && !extra.isEmpty()) {
            pd.setProperty("extra", extra);
        }
        if (errores != null && !errores.isEmpty()) {
            pd.setProperty("errors", errores);
        }
        return pd;
    }

    /** Forma plana (type, title, status, detail, code, extra, errors) para escribir fuera de Spring MVC. */
    public static Map<String, Object> comoMapa(ProblemDetail pd) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("type", String.valueOf(pd.getType()));
        mapa.put("title", pd.getTitle());
        mapa.put("status", pd.getStatus());
        mapa.put("detail", pd.getDetail());
        if (pd.getProperties() != null) {
            mapa.putAll(pd.getProperties());
        }
        return mapa;
    }
}
