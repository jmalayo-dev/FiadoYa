package pe.edu.cibertec.fiadoya.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.fiadoya.auth.dto.LoginRequest;
import pe.edu.cibertec.fiadoya.auth.dto.LoginResponse;
import pe.edu.cibertec.fiadoya.auth.dto.UsuarioInput;
import pe.edu.cibertec.fiadoya.auth.dto.UsuarioSesion;
import pe.edu.cibertec.fiadoya.auth.service.AuthService;
import pe.edu.cibertec.fiadoya.common.security.UsuarioActual;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "HU01: login, sesión y usuarios")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve el JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario de la sesión")
    public UsuarioSesion me(Authentication auth) {
        return service.buscar(UsuarioActual.id(auth));
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BODEGUERO')")
    @Operation(summary = "Crea un usuario (el bodeguero da acceso a un cliente)")
    public UsuarioSesion crear(@Valid @RequestBody UsuarioInput input) {
        return service.crear(input);
    }
}
