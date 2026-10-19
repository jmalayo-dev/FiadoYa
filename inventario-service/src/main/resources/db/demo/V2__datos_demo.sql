-- Datos ficticios para ensayos (Anexo §4.2). Dos productos quedan en el mínimo o justo encima para disparar HU08.
INSERT INTO producto (nombre, precio, costo, stock, stock_minimo, categoria, codigo_barras) VALUES
 (N'Arroz extra 1 kg',          4.50, 3.80, 40,  8, 'ABARROTES', '7750001000011'),
 (N'Aceite vegetal 1 L',        9.80, 8.90,  7,  6, 'ABARROTES', '7750001000028'),
 (N'Azúcar rubia 1 kg',         4.20, 3.50, 24,  6, 'ABARROTES', '7750001000035'),
 (N'Fideos spaghetti 500 g',    2.80, 2.30, 30,  6, 'ABARROTES', '7750001000042'),
 (N'Leche evaporada 400 g',     4.00, 3.40,  5,  5, 'LACTEOS',   '7750001000059'),
 (N'Atún en lata 170 g',        6.50, 5.20, 18,  4, 'ABARROTES', '7750001000066'),
 (N'Galletas de soda 6 pack',   2.00, 1.60, 25,  5, 'ABARROTES', '7750001000073'),
 (N'Gaseosa 1.5 L',             6.50, 5.50, 15,  4, 'BEBIDAS',   '7750001000080'),
 (N'Huevos x 15',              10.50, 8.50, 10,  3, 'ABARROTES', '7750001000097'),
 (N'Pan de molde 600 g',        8.20, 6.90,  9,  3, 'ABARROTES', '7750001000103'),
 (N'Detergente 900 g',         12.00, 9.80, 12,  3, 'LIMPIEZA',  '7750001000110'),
 (N'Papel higiénico x 4',       7.50, 5.90, 20,  4, 'LIMPIEZA',  '7750001000127');
