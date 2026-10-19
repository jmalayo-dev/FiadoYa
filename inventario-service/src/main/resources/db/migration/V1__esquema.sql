-- inventario-service (Anexo §4.1 + v1.2 E3)
CREATE TABLE producto (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    nombre         NVARCHAR(80)  NOT NULL,
    precio         DECIMAL(10,2) NOT NULL CONSTRAINT ck_producto_precio CHECK (precio > 0),
    costo          DECIMAL(10,2) NULL     CONSTRAINT ck_producto_costo CHECK (costo IS NULL OR costo > 0),
    stock          INT           NOT NULL CONSTRAINT ck_producto_stock CHECK (stock >= 0),
    stock_minimo   INT           NOT NULL CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo >= 0),
    categoria      NVARCHAR(20)  NOT NULL CONSTRAINT df_producto_categoria DEFAULT 'OTROS'
                   CONSTRAINT ck_producto_categoria CHECK (categoria IN ('ABARROTES','LACTEOS','BEBIDAS','LIMPIEZA','OTROS')),
    codigo_barras  NVARCHAR(14)  NULL,
    activo         BIT           NOT NULL CONSTRAINT df_producto_activo DEFAULT 1,
    creado_en      DATETIME2     NOT NULL CONSTRAINT df_producto_creado DEFAULT SYSUTCDATETIME(),
    actualizado_en DATETIME2     NOT NULL CONSTRAINT df_producto_actualizado DEFAULT SYSUTCDATETIME()
);
CREATE UNIQUE INDEX uq_producto_codigo ON producto (codigo_barras) WHERE codigo_barras IS NOT NULL;
CREATE INDEX ix_producto_nombre ON producto (nombre);

CREATE TABLE movimiento_stock (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    producto_id     INT           NOT NULL CONSTRAINT fk_movimiento_producto REFERENCES producto (id),
    tipo            NVARCHAR(20)  NOT NULL CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('VENTA','DEVOLUCION','REPOSICION','AJUSTE')),
    cantidad        INT           NOT NULL,
    venta_id        INT           NULL,
    precio_unitario DECIMAL(10,2) NULL,
    motivo          NVARCHAR(120) NULL,
    creado_en       DATETIME2     NOT NULL CONSTRAINT df_movimiento_creado DEFAULT SYSUTCDATETIME()
);
-- Hace idempotentes los descuentos y devoluciones de una misma venta (Anexo §10.1).
CREATE UNIQUE INDEX uq_movimiento_venta ON movimiento_stock (venta_id, producto_id, tipo) WHERE venta_id IS NOT NULL;
CREATE INDEX ix_movimiento_producto ON movimiento_stock (producto_id, creado_en);

CREATE TABLE reposicion (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    producto_id    INT          NOT NULL CONSTRAINT fk_reposicion_producto REFERENCES producto (id),
    cantidad       INT          NOT NULL CONSTRAINT ck_reposicion_cantidad CHECK (cantidad BETWEEN 1 AND 9999),
    nro_pedido     NVARCHAR(30) NOT NULL,
    fecha_estimada DATE         NOT NULL,
    recibida       BIT          NOT NULL CONSTRAINT df_reposicion_recibida DEFAULT 0,
    creado_en      DATETIME2    NOT NULL CONSTRAINT df_reposicion_creado DEFAULT SYSUTCDATETIME()
);
