-- ventas-service (Anexo §4.1)
CREATE TABLE venta (
    id                 INT IDENTITY(1,1) PRIMARY KEY,
    cliente_id         INT           NULL,
    cliente_nombre     NVARCHAR(80)  NULL,
    tipo_pago          NVARCHAR(10)  NOT NULL CONSTRAINT ck_venta_tipo CHECK (tipo_pago IN ('CONTADO','FIADO')),
    estado             NVARCHAR(12)  NOT NULL CONSTRAINT ck_venta_estado CHECK (estado IN ('PENDIENTE','CONFIRMADA','RECHAZADA')),
    total              DECIMAL(10,2) NOT NULL CONSTRAINT df_venta_total DEFAULT 0,
    motivo_rechazo     NVARCHAR(200) NULL,
    codigo_rechazo     NVARCHAR(40)  NULL,
    clave_idempotencia NVARCHAR(64)  NOT NULL CONSTRAINT uq_venta_clave UNIQUE,
    hash_cuerpo        NVARCHAR(64)  NOT NULL,
    fecha              DATETIME2     NOT NULL CONSTRAINT df_venta_fecha DEFAULT SYSUTCDATETIME()
);
CREATE INDEX ix_venta_fecha ON venta (fecha DESC);
CREATE INDEX ix_venta_estado ON venta (estado, fecha);

CREATE TABLE detalle_venta (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    venta_id        INT           NOT NULL CONSTRAINT fk_detalle_venta REFERENCES venta (id),
    producto_id     INT           NOT NULL,
    nombre_producto NVARCHAR(80)  NOT NULL,
    cantidad        INT           NOT NULL CONSTRAINT ck_detalle_cantidad CHECK (cantidad BETWEEN 1 AND 999),
    precio_unitario DECIMAL(10,2) NOT NULL
);
CREATE INDEX ix_detalle_venta ON detalle_venta (venta_id);
