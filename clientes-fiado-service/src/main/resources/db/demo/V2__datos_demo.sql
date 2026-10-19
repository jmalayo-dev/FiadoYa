-- 8 clientes ficticios (Anexo §4.2): 5 con consentimiento y deuda, 1 al borde del límite,
-- 1 sin consentimiento y 1 sin deuda. Teléfonos y DNI inventados.
-- María Quispe (usuario_id = 2) es el cliente demo con acceso: usuario "cliente1" creado por auth-service.
SET IDENTITY_INSERT cliente ON;
INSERT INTO cliente (id, usuario_id, dni, nombre, telefono, direccion, limite_credito, saldo, consentimiento, fecha_consentimiento) VALUES
 (1, 2,    '40000001', N'María Quispe',   '987654321', N'Jr. Áncash 412',     150.00,  85.50, 1, SYSUTCDATETIME()),
 (2, NULL, '40000002', N'Jorge Huamán',   '912345678', N'Av. Grau 120',       150.00, 142.00, 1, SYSUTCDATETIME()),
 (3, NULL, '40000003', N'Luis Condori',   '954112890', N'Jr. Puno 88',        150.00,  38.20, 1, SYSUTCDATETIME()),
 (4, NULL, '40000004', N'Rosa Paredes',   '945882103', N'Psje. Los Olivos 5', 150.00,   0.00, 1, SYSUTCDATETIME()),
 (5, NULL, '40000005', N'Carmen Rojas',   '966123450', N'Av. Perú 1500',      200.00,  60.00, 1, SYSUTCDATETIME()),
 (6, NULL, '40000006', N'Pedro Salazar',  '978456123', N'Jr. Junín 230',      100.00,  25.00, 1, SYSUTCDATETIME()),
 (7, NULL, '40000007', N'Ana Torres',     '934567812', N'Calle Real 45',      120.00,  15.50, 1, SYSUTCDATETIME()),
 (8, NULL, '40000008', N'Julio Mendoza',  '923456781', NULL,                  100.00,   0.00, 0, NULL);
SET IDENTITY_INSERT cliente OFF;

-- Movimientos de apertura para que el estado de cuenta cuadre con el saldo inicial.
INSERT INTO movimiento_cuenta (cliente_id, tipo, monto, venta_id, saldo_despues, descripcion, cantidad_items, fecha) VALUES
 (1, 'CARGO', 85.50, NULL, 85.50, N'Saldo anterior (cuaderno)', NULL, DATEADD(day, -3, SYSUTCDATETIME())),
 (2, 'CARGO', 142.00, NULL, 142.00, N'Saldo anterior (cuaderno)', NULL, DATEADD(day, -5, SYSUTCDATETIME())),
 (3, 'CARGO', 38.20, NULL, 38.20, N'Saldo anterior (cuaderno)', NULL, DATEADD(day, -2, SYSUTCDATETIME())),
 (5, 'CARGO', 60.00, NULL, 60.00, N'Saldo anterior (cuaderno)', NULL, DATEADD(day, -4, SYSUTCDATETIME())),
 (6, 'CARGO', 25.00, NULL, 25.00, N'Saldo anterior (cuaderno)', NULL, DATEADD(day, -1, SYSUTCDATETIME())),
 (7, 'CARGO', 15.50, NULL, 15.50, N'Saldo anterior (cuaderno)', NULL, DATEADD(day, -6, SYSUTCDATETIME()));
