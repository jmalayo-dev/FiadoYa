package pe.edu.cibertec.fiadoya.auth.dto;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEn, UsuarioSesion usuario) {
}
