package pe.edu.cibertec.fiadoya.common.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Excepción de negocio que termina como application/problem+json con su code y extra. */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> extra;

    public ApiException(ErrorCode code, String detail) {
        this(code, detail, Map.of());
    }

    public ApiException(ErrorCode code, String detail, Map<String, Object> extra) {
        super(detail);
        this.code = code;
        this.extra = extra == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(extra));
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public static ApiException noEncontrado(String recurso, Object id) {
        return new ApiException(ErrorCode.NOT_FOUND, recurso + " " + id + " no existe.");
    }

    public static ApiException validacion(String campo, String mensaje) {
        return new ValidationException(java.util.List.of(new ErrorCampo(campo, mensaje)));
    }

    public static ApiException servicioNoDisponible(String servicio) {
        return new ApiException(ErrorCode.SERVICIO_NO_DISPONIBLE, "El servicio " + servicio + " no responde.");
    }
}
