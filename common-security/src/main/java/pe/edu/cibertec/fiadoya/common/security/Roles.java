package pe.edu.cibertec.fiadoya.common.security;

public final class Roles {

    public static final String BODEGUERO = "BODEGUERO";
    public static final String CLIENTE = "CLIENTE";
    public static final String INTERNO = "INTERNO";

    /** Nombre del claim que lleva el rol dentro del JWT. */
    public static final String CLAIM_ROL = "rol";

    private Roles() {
    }
}
