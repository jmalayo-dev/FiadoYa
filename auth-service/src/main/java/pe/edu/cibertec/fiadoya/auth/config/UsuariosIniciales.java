package pe.edu.cibertec.fiadoya.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.cibertec.fiadoya.auth.entity.Rol;
import pe.edu.cibertec.fiadoya.auth.entity.Usuario;
import pe.edu.cibertec.fiadoya.auth.repository.UsuarioRepository;

/**
 * R7: el bodeguero inicial se crea leyendo ADMIN_USERNAME y ADMIN_PASSWORD del entorno.
 * Con el perfil demo también se crea el cliente de prueba (DEMO_CLIENTE_PASSWORD).
 * Orden fijo para que en una base nueva el bodeguero sea id 1 y el cliente demo id 2.
 */
@Configuration
public class UsuariosIniciales {

    private static final Logger log = LoggerFactory.getLogger(UsuariosIniciales.class);

    @Bean
    @Order(1)
    ApplicationRunner crearBodeguero(UsuarioRepository repo, PasswordEncoder encoder, AuthProperties props) {
        return args -> crearSiFalta(repo, encoder, props.admin().username(), props.admin().password(), Rol.BODEGUERO);
    }

    @Bean
    @Order(2)
    @Profile("demo")
    ApplicationRunner crearClienteDemo(UsuarioRepository repo, PasswordEncoder encoder, AuthProperties props) {
        return (ApplicationArguments args) -> crearSiFalta(repo, encoder,
                props.demo().clienteUsername(), props.demo().clientePassword(), Rol.CLIENTE);
    }

    static void crearSiFalta(UsuarioRepository repo, PasswordEncoder encoder, String username, String password, Rol rol) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("No se creó el usuario {} inicial: falta usuario o contraseña en el entorno", rol);
            return;
        }
        String nombre = username.trim().toLowerCase();
        if (repo.existsByUsername(nombre)) {
            return;
        }
        repo.save(new Usuario(nombre, encoder.encode(password), rol));
        log.info("Usuario {} '{}' creado", rol, nombre);
    }
}
