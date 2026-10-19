package pe.edu.cibertec.fiadoya.common.web;

import org.springframework.http.HttpStatus;

/** Catálogo de errores del Anexo v1.1 §7.1 más los agregados en v1.2. */
public enum ErrorCode {

    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Sesión vencida o token inválido"),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "Sin permiso"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Datos inválidos"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso no encontrado"),
    PRODUCTO_STOCK_INSUFICIENTE(HttpStatus.CONFLICT, "Stock insuficiente"),
    PRODUCTO_INACTIVO(HttpStatus.CONFLICT, "Producto dado de baja"),
    CLIENTE_LIMITE_EXCEDIDO(HttpStatus.CONFLICT, "Límite de crédito excedido"),
    CLIENTE_SIN_CONSENTIMIENTO(HttpStatus.CONFLICT, "Cliente sin consentimiento"),
    CLIENTE_INACTIVO(HttpStatus.CONFLICT, "Cliente inactivo"),
    CLIENTE_CON_SALDO(HttpStatus.CONFLICT, "Cliente con saldo pendiente"),
    PAGO_EXCEDE_SALDO(HttpStatus.CONFLICT, "El pago supera el saldo"),
    USERNAME_DUPLICADO(HttpStatus.CONFLICT, "Usuario duplicado"),
    DNI_DUPLICADO(HttpStatus.CONFLICT, "DNI duplicado"),
    CODIGO_DUPLICADO(HttpStatus.CONFLICT, "Código de barras duplicado"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Clave de idempotencia reutilizada"),
    SERVICIO_NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado");

    private final HttpStatus status;
    private final String titulo;

    ErrorCode(HttpStatus status, String titulo) {
        this.status = status;
        this.titulo = titulo;
    }

    public HttpStatus status() {
        return status;
    }

    public String titulo() {
        return titulo;
    }

    /** https://fiadoya.local/errors/cliente-limite-excedido */
    public String tipoUri() {
        return "https://fiadoya.local/errors/" + name().toLowerCase().replace('_', '-');
    }
}
