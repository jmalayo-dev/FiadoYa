package pe.edu.cibertec.fiadoya.contracts;

/** Nombres de exchanges, routing keys y colas (Plan v1.0 §5.3 y Anexo v1.2 E4). */
public final class Mensajeria {

    public static final String EXCHANGE = "fiadoya.events";
    public static final String DLX = "fiadoya.dlx";

    public static final String RK_VENTA_REGISTRADA = "venta.registrada";
    public static final String RK_STOCK_BAJO = "stock.bajo";
    public static final String RK_PAGO_REGISTRADO = "pago.registrado";
    public static final String RK_CLIENTE_LIMITE_CERCANO = "cliente.limite-cercano";

    public static final String Q_NOTIFICACIONES_STOCK_BAJO = "notificaciones.stock-bajo";
    public static final String Q_NOTIFICACIONES_LIMITE_CLIENTE = "notificaciones.limite-cliente";
    public static final String Q_REPORTES_VENTAS = "reportes.ventas";
    public static final String Q_REPORTES_PAGOS = "reportes.pagos";

    /** Sufijo de la cola de mensajes muertos: notificaciones.stock-bajo.dlq */
    public static final String DLQ_SUFIJO = ".dlq";

    private Mensajeria() {
    }
}
