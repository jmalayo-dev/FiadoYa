-- auth-service: usuarios del sistema (Anexo §4.1)
CREATE TABLE usuario (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    username      NVARCHAR(40)  NOT NULL CONSTRAINT uq_usuario_username UNIQUE,
    password_hash NVARCHAR(100) NOT NULL,
    rol           NVARCHAR(20)  NOT NULL CONSTRAINT ck_usuario_rol CHECK (rol IN ('BODEGUERO', 'CLIENTE')),
    activo        BIT           NOT NULL CONSTRAINT df_usuario_activo DEFAULT 1,
    creado_en     DATETIME2     NOT NULL CONSTRAINT df_usuario_creado DEFAULT SYSUTCDATETIME()
);
