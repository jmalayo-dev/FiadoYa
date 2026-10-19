-- clientes-fiado-service (Anexo §4.1 + v1.2 E1, E2 y E6)
CREATE TABLE cliente (
    id                   INT IDENTITY(1,1) PRIMARY KEY,
    usuario_id           INT           NULL,
    dni                  NVARCHAR(8)   NULL,
    nombre               NVARCHAR(80)  NOT NULL,
    telefono             NVARCHAR(9)   NOT NULL,
    direccion            NVARCHAR(120) NULL,
    limite_credito       DECIMAL(10,2) NOT NULL CONSTRAINT ck_cliente_limite CHECK (limite_credito >= 0),
    saldo                DECIMAL(10,2) NOT NULL CONSTRAINT df_cliente_saldo DEFAULT 0 CONSTRAINT ck_cliente_saldo CHECK (saldo >= 0),
    consentimiento       BIT           NOT NULL CONSTRAINT df_cliente_consentimiento DEFAULT 0,
    fecha_consentimiento DATETIME2     NULL,
    activo               BIT           NOT NULL CONSTRAINT df_cliente_activo DEFAULT 1,
    creado_en            DATETIME2     NOT NULL CONSTRAINT df_cliente_creado DEFAULT SYSUTCDATETIME(),
    actualizado_en       DATETIME2     NOT NULL CONSTRAINT df_cliente_actualizado DEFAULT SYSUTCDATETIME()
);
CREATE UNIQUE INDEX uq_cliente_usuario ON cliente (usuario_id) WHERE usuario_id IS NOT NULL;
CREATE UNIQUE INDEX uq_cliente_dni ON cliente (dni) WHERE dni IS NOT NULL;
CREATE INDEX ix_cliente_nombre ON cliente (nombre);

CREATE TABLE cargo (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    cliente_id     INT           NOT NULL CONSTRAINT fk_cargo_cliente REFERENCES cliente (id),
    venta_id       INT           NOT NULL CONSTRAINT uq_cargo_venta UNIQUE,
    monto          DECIMAL(10,2) NOT NULL CONSTRAINT ck_cargo_monto CHECK (monto > 0),
    descripcion    NVARCHAR(160) NULL,
    cantidad_items INT           NULL,
    anulado        BIT           NOT NULL CONSTRAINT df_cargo_anulado DEFAULT 0,
    creado_en      DATETIME2     NOT NULL CONSTRAINT df_cargo_creado DEFAULT SYSUTCDATETIME()
);

CREATE TABLE pago (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    cliente_id INT           NOT NULL CONSTRAINT fk_pago_cliente REFERENCES cliente (id),
    monto      DECIMAL(10,2) NOT NULL CONSTRAINT ck_pago_monto CHECK (monto > 0),
    metodo     NVARCHAR(20)  NOT NULL CONSTRAINT df_pago_metodo DEFAULT 'EFECTIVO'
               CONSTRAINT ck_pago_metodo CHECK (metodo IN ('EFECTIVO','YAPE_PLIN','TRANSFERENCIA')),
    fecha      DATETIME2     NOT NULL CONSTRAINT df_pago_fecha DEFAULT SYSUTCDATETIME()
);

-- Libro de la cuenta: cada cargo, pago o anulación con el saldo resultante (estado de cuenta, HU07).
CREATE TABLE movimiento_cuenta (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    cliente_id     INT           NOT NULL CONSTRAINT fk_movcuenta_cliente REFERENCES cliente (id),
    tipo           NVARCHAR(20)  NOT NULL CONSTRAINT ck_movcuenta_tipo CHECK (tipo IN ('CARGO','PAGO','ANULACION')),
    monto          DECIMAL(10,2) NOT NULL,
    venta_id       INT           NULL,
    saldo_despues  DECIMAL(10,2) NOT NULL,
    descripcion    NVARCHAR(160) NULL,
    cantidad_items INT           NULL,
    metodo_pago    NVARCHAR(20)  NULL,
    fecha          DATETIME2     NOT NULL CONSTRAINT df_movcuenta_fecha DEFAULT SYSUTCDATETIME()
);
CREATE INDEX ix_movcuenta_cliente ON movimiento_cuenta (cliente_id, fecha DESC);
