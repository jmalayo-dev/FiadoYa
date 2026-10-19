package pe.edu.cibertec.fiadoya.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.auth.dto.LoginRequest;
import pe.edu.cibertec.fiadoya.auth.dto.LoginResponse;
import pe.edu.cibertec.fiadoya.auth.dto.UsuarioInput;
import pe.edu.cibertec.fiadoya.auth.dto.UsuarioSesion;
import pe.edu.cibertec.fiadoya.auth.entity.Usuario;
import pe.edu.cibertec.fiadoya.auth.repository.UsuarioRepository;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;

@Service
public class AuthService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthService(UsuarioRepository repo, PasswordEncoder encoder, TokenService tokens) {
        this.repo = repo;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        Usuario usuario = repo.findByUsername(req.username().trim().toLowerCase())
                .filter(Usuario::isActivo)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Usuario o contraseña incorrectos."));
        TokenService.Emitido emitido = tokens.emitir(usuario);
        return new LoginResponse(emitido.token(), "Bearer", emitido.expiraEn(), UsuarioSesion.de(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioSesion buscar(Long id) {
        return repo.findById(id).filter(Usuario::isActivo).map(UsuarioSesion::de)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_TOKEN_EXPIRED, "El usuario ya no existe o está inactivo."));
    }

    @Transactional
    public UsuarioSesion crear(UsuarioInput input) {
        String username = input.username().trim().toLowerCase();
        if (repo.existsByUsername(username)) {
            throw new ApiException(ErrorCode.USERNAME_DUPLICADO, "El usuario " + username + " ya existe.");
        }
        Usuario guardado = repo.save(new Usuario(username, encoder.encode(input.password()), input.rol()));
        return UsuarioSesion.de(guardado);
    }
}
