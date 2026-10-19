package pe.edu.cibertec.fiadoya.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.cibertec.fiadoya.auth.dto.LoginRequest;
import pe.edu.cibertec.fiadoya.auth.dto.UsuarioInput;
import pe.edu.cibertec.fiadoya.auth.entity.Rol;
import pe.edu.cibertec.fiadoya.auth.entity.Usuario;
import pe.edu.cibertec.fiadoya.auth.repository.UsuarioRepository;
import pe.edu.cibertec.fiadoya.auth.service.AuthService;
import pe.edu.cibertec.fiadoya.auth.service.TokenService;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepository repo;
    @Mock
    TokenService tokens;

    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(repo, encoder, tokens);
    }

    private Usuario usuario(String username, String password, Rol rol) {
        Usuario u = new Usuario(username, encoder.encode(password), rol);
        ReflectionTestUtils.setField(u, "id", 1L);
        return u;
    }

    @Test
    void loginCorrectoDevuelveTokenYUsuario() {
        when(repo.findByUsername("bodeguero")).thenReturn(Optional.of(usuario("bodeguero", "Admin12345", Rol.BODEGUERO)));
        when(tokens.emitir(any())).thenReturn(new TokenService.Emitido("jwt", Instant.parse("2026-10-20T00:00:00Z")));

        var resp = service.login(new LoginRequest("  BODEGUERO ", "Admin12345"));

        assertThat(resp.token()).isEqualTo("jwt");
        assertThat(resp.tipo()).isEqualTo("Bearer");
        assertThat(resp.usuario().rol()).isEqualTo("BODEGUERO");
    }

    @Test
    void contrasenaIncorrectaDa401() {
        when(repo.findByUsername("bodeguero")).thenReturn(Optional.of(usuario("bodeguero", "Admin12345", Rol.BODEGUERO)));

        assertThatThrownBy(() -> service.login(new LoginRequest("bodeguero", "otra")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        verify(tokens, never()).emitir(any());
    }

    @Test
    void usuarioInexistenteDa401() {
        when(repo.findByUsername("nadie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(new LoginRequest("nadie", "x")))
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    void crearUsuarioDuplicadoDa409() {
        when(repo.existsByUsername("cliente1")).thenReturn(true);
        assertThatThrownBy(() -> service.crear(new UsuarioInput("cliente1", "Clave1234", Rol.CLIENTE)))
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.USERNAME_DUPLICADO);
    }

    @Test
    void crearUsuarioGuardaHashBcrypt() {
        when(repo.existsByUsername("maria")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 5L);
            return u;
        });

        var creado = service.crear(new UsuarioInput("maria", "Clave1234", Rol.CLIENTE));

        assertThat(creado.id()).isEqualTo(5L);
        assertThat(creado.rol()).isEqualTo("CLIENTE");
    }
}
