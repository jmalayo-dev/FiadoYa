-- reportes-service: copia local para reportes, alimentada por eventos (Anexo §4.1)
CREATE TABLE venta_resumen (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    venta_id   INT           NOT NULL CONSTRAINT uq_venta_resumen UNIQUE,
    cliente_id INT           NULL,
    tipo_pago  NVARCHAR(10)  NOT NULL,
    total      DECIMAL(10,2) NOT NULL,
    fecha      DATETIME2     NOT NULL
);
CREATE INDEX ix_venta_resumen_fecha ON venta_resumen (fecha);

CREATE TABLE producto_vendido (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    venta_id        INT           NOT NULL,
    producto_id     INT           NOT NULL,
    nombre_producto NVARCHAR(80)  NOT NULL,
    cantidad        INT           NOT NULL,
    monto           DECIMAL(10,2) NOT NULL,
    fecha           DATETIME2     NOT NULL
);
CREATE INDEX ix_producto_vendido_fecha ON producto_vendido (fecha);

CREATE TABLE pago_resumen (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    pago_id    INT           NOT NULL CONSTRAINT uq_pago_resumen UNIQUE,
    cliente_id INT           NOT NULL,
    monto      DECIMAL(10,2) NOT NULL,
    metodo     NVARCHAR(20)  NULL,
    fecha      DATETIME2     NOT NULL
);

-- Deduplicación: un eventId se procesa una sola vez (Anexo §8.1)
CREATE TABLE evento_procesado (
    event_id     NVARCHAR(36) NOT NULL PRIMARY KEY,
    tipo         NVARCHAR(40) NOT NULL,
    procesado_en DATETIME2    NOT NULL CONSTRAINT df_evento_procesado DEFAULT SYSUTCDATETIME()
);
