package pe.edu.cibertec.fiadoya.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fiadoya")
public record AuthProperties(Jwt jwt, Admin admin, Demo demo, Bodega bodega) {

    public record Jwt(int ttlHoras) {
    }

    public record Admin(String username, String password) {
    }

    public record Demo(String clienteUsername, String clientePassword) {
    }

    public record Bodega(String nombre, String telefono, String direccion) {
    }
}
