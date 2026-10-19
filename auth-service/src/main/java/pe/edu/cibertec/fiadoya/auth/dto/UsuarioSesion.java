package pe.edu.cibertec.fiadoya.auth.dto;

import pe.edu.cibertec.fiadoya.auth.entity.Usuario;

public record UsuarioSesion(Long id, String username, String rol) {

    public static UsuarioSesion de(Usuario u) {
        return new UsuarioSesion(u.getId(), u.getUsername(), u.getRol().name());
    }
}
