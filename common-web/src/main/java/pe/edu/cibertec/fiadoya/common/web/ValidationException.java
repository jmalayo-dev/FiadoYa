package pe.edu.cibertec.fiadoya.common.web;

import java.util.List;

/** VALIDATION_ERROR con lista de campos, lanzada desde reglas de negocio. */
public class ValidationException extends ApiException {

    private final List<ErrorCampo> errores;

    public ValidationException(List<ErrorCampo> errores) {
        super(ErrorCode.VALIDATION_ERROR, "Revisa los campos marcados.");
        this.errores = List.copyOf(errores);
    }

    public List<ErrorCampo> getErrores() {
        return errores;
    }
}
