"""Genera FiadoYa.postman_collection.json (casos del Anexo §13). Ejecutar: python3 tests/postman/generar_coleccion.py"""
import json
import pathlib

items = []


def req(nombre, metodo, ruta, body=None, token="token", headers=None, tests="", pre=""):
    h = [{"key": "Content-Type", "value": "application/json"}] if body is not None else []
    if token:
        h.append({"key": "Authorization", "value": "Bearer {{" + token + "}}"})
    for k, v in (headers or {}).items():
        h.append({"key": k, "value": v})
    item = {
        "name": nombre,
        "request": {"method": metodo, "header": h, "url": "{{baseUrl}}" + ruta},
        "event": [],
    }
    if body is not None:
        item["request"]["body"] = {"mode": "raw", "raw": json.dumps(body, ensure_ascii=False) if not isinstance(body, str) else body}
    if pre:
        item["event"].append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": pre.strip().split("\n")}})
    if tests:
        item["event"].append({"listen": "test", "script": {"type": "text/javascript", "exec": tests.strip().split("\n")}})
    return item


def carpeta(nombre, *reqs):
    items.append({"name": nombre, "item": list(reqs)})


def estado(codigo, code=None):
    t = f"pm.test('HTTP {codigo}', () => pm.response.to.have.status({codigo}));"
    if code:
        t += f"\npm.test('code {code}', () => pm.expect(pm.response.json().code).to.eql('{code}'));"
    return t


NUEVA_CLAVE = "pm.collectionVariables.set('clave', pm.variables.replaceIn('{{$guid}}'));"

carpeta("00 Preparación",
    req("Login bodeguero", "POST", "/api/auth/login", {"username": "{{adminUser}}", "password": "{{adminPassword}}"}, token=None,
        tests=estado(200) + "\npm.collectionVariables.set('token', pm.response.json().token);\npm.collectionVariables.set('sufijo', Date.now() % 100000000);"),
    req("Login cliente demo", "POST", "/api/auth/login", {"username": "cliente1", "password": "{{clientePassword}}"}, token=None,
        tests=estado(200) + "\npm.collectionVariables.set('tokenCliente', pm.response.json().token);"),
    req("Producto A (stock 30)", "POST", "/api/productos",
        {"nombre": "Postman A {{sufijo}}", "precio": 4.50, "stock": 30, "stockMinimo": 3, "categoria": "ABARROTES"},
        tests=estado(201) + "\npm.collectionVariables.set('prodA', pm.response.json().id);"),
    req("Producto B (stock 1)", "POST", "/api/productos",
        {"nombre": "Postman B {{sufijo}}", "precio": 1.335, "stock": 1, "stockMinimo": 0, "categoria": "OTROS"},
        tests=estado(400, "VALIDATION_ERROR") + "\n// Precio con 3 decimales: se rechaza"),
    req("Producto B corregido", "POST", "/api/productos",
        {"nombre": "Postman B {{sufijo}}", "precio": 2.25, "stock": 1, "stockMinimo": 0, "categoria": "OTROS"},
        tests=estado(201) + "\npm.collectionVariables.set('prodB', pm.response.json().id);"),
    req("Cliente con consentimiento (límite 50)", "POST", "/api/clientes",
        {"nombre": "Postman Con Permiso", "telefono": "912345678", "limiteCredito": 50, "consentimiento": True},
        tests=estado(201) + "\npm.collectionVariables.set('cliOk', pm.response.json().id);"),
    req("Cliente sin consentimiento", "POST", "/api/clientes",
        {"nombre": "Postman Sin Permiso", "telefono": "912345679", "limiteCredito": 50, "consentimiento": False},
        tests=estado(201) + "\npm.collectionVariables.set('cliSin', pm.response.json().id);"),
)

carpeta("HU01 Seguridad",
    req("Contraseña incorrecta → 401", "POST", "/api/auth/login", {"username": "{{adminUser}}", "password": "mala12345"}, token=None,
        tests=estado(401, "AUTH_INVALID_CREDENTIALS")),
    req("Token inválido o vencido → 401", "GET", "/api/auth/me", token="tokenInvalido", tests=estado(401, "AUTH_TOKEN_EXPIRED")),
    req("Sin token → 401", "GET", "/api/productos", token=None, tests=estado(401, "AUTH_TOKEN_EXPIRED")),
    req("CLIENTE en /api/productos → 403", "GET", "/api/productos", token="tokenCliente", tests=estado(403, "AUTH_FORBIDDEN")),
    req("/internal no se expone → 403", "POST", "/internal/inventario/devoluciones", {"ventaId": 1}, headers={"X-Internal-Key": "x"}, tests=estado(403)),
    req("GET /me", "GET", "/api/auth/me", tests=estado(200) + "\npm.test('rol', () => pm.expect(pm.response.json().rol).to.eql('BODEGUERO'));"),
    req("GET /bodega", "GET", "/api/bodega", tests=estado(200)),
)

carpeta("HU02 Clientes",
    req("FIADO a cliente sin consentimiento → 409", "POST", "/api/ventas",
        {"clienteId": "{{cliSin}}", "tipoPago": "FIADO", "items": [{"productoId": "{{prodA}}", "cantidad": 1}]},
        headers={"Idempotency-Key": "{{clave}}"}, pre=NUEVA_CLAVE, tests=estado(409, "CLIENTE_SIN_CONSENTIMIENTO")),
    req("Teléfono inválido → VALIDATION_ERROR", "POST", "/api/clientes",
        {"nombre": "X", "telefono": "123", "limiteCredito": 10, "consentimiento": True}, tests=estado(400, "VALIDATION_ERROR")),
    req("Lista con deuda", "GET", "/api/clientes?conDeuda=true&size=5", tests=estado(200) + "\npm.test('Page<Cliente>', () => pm.expect(pm.response.json()).to.have.keys('content','page','size','totalElements','totalPages'));"),
    req("Resumen", "GET", "/api/clientes/resumen", tests=estado(200)),
)

carpeta("HU03 Productos",
    req("Ajuste que deja stock negativo → 400", "POST", "/api/productos/{{prodA}}/ajuste", {"delta": -99, "motivo": "prueba"}, tests=estado(400, "VALIDATION_ERROR")),
    req("Búsqueda y solo bajos", "GET", "/api/productos?q=Postman&soloBajos=false", tests=estado(200)),
)

carpeta("HU04 Venta al contado",
    req("Contado 2 ítems → 201 CONFIRMADA", "POST", "/api/ventas",
        {"clienteId": None, "tipoPago": "CONTADO", "items": [{"productoId": "{{prodA}}", "cantidad": 2}, {"productoId": "{{prodB}}", "cantidad": 1}]},
        headers={"Idempotency-Key": "{{clave}}"}, pre=NUEVA_CLAVE,
        tests=estado(201) + "\nconst v = pm.response.json();\npm.test('CONFIRMADA y total HALF_UP', () => { pm.expect(v.estado).to.eql('CONFIRMADA'); pm.expect(v.total).to.eql(11.25); });\npm.collectionVariables.set('ventaContado', v.id);"),
    req("Misma Idempotency-Key → misma venta", "POST", "/api/ventas",
        {"clienteId": None, "tipoPago": "CONTADO", "items": [{"productoId": "{{prodA}}", "cantidad": 2}, {"productoId": "{{prodB}}", "cantidad": 1}]},
        headers={"Idempotency-Key": "{{clave}}"},
        tests=estado(201) + "\npm.test('mismo id', () => pm.expect(pm.response.json().id).to.eql(pm.collectionVariables.get('ventaContado')));"),
    req("Stock restado una sola vez", "GET", "/api/productos/{{prodA}}", tests=estado(200) + "\npm.test('stock 28', () => pm.expect(pm.response.json().stock).to.eql(28));"),
    req("Stock insuficiente en el 2.º ítem → 409 sin descontar", "POST", "/api/ventas",
        {"clienteId": None, "tipoPago": "CONTADO", "items": [{"productoId": "{{prodA}}", "cantidad": 1}, {"productoId": "{{prodB}}", "cantidad": 5}]},
        headers={"Idempotency-Key": "{{clave}}"}, pre=NUEVA_CLAVE, tests=estado(409, "PRODUCTO_STOCK_INSUFICIENTE")),
    req("Ningún ítem descontado", "GET", "/api/productos/{{prodA}}", tests=estado(200) + "\npm.test('stock sigue en 28', () => pm.expect(pm.response.json().stock).to.eql(28));"),
    req("Sin Idempotency-Key → 400", "POST", "/api/ventas",
        {"clienteId": None, "tipoPago": "CONTADO", "items": [{"productoId": "{{prodA}}", "cantidad": 1}]}, tests=estado(400, "VALIDATION_ERROR")),
    req("Historial de hoy", "GET", "/api/ventas?estado=CONFIRMADA&size=5", tests=estado(200)),
)

carpeta("HU05 Venta al fiado",
    req("FIADO dentro del límite → CONFIRMADA", "POST", "/api/ventas",
        {"clienteId": "{{cliOk}}", "tipoPago": "FIADO", "items": [{"productoId": "{{prodA}}", "cantidad": 4}]},
        headers={"Idempotency-Key": "{{clave}}"}, pre=NUEVA_CLAVE,
        tests=estado(201) + "\npm.test('CONFIRMADA', () => pm.expect(pm.response.json().estado).to.eql('CONFIRMADA'));"),
    req("Saldo sube a 18.00", "GET", "/api/clientes/{{cliOk}}", tests=estado(200) + "\npm.test('saldo 18', () => pm.expect(pm.response.json().saldo).to.eql(18));"),
    req("FIADO que excede el límite → 409", "POST", "/api/ventas",
        {"clienteId": "{{cliOk}}", "tipoPago": "FIADO", "items": [{"productoId": "{{prodA}}", "cantidad": 4}, {"productoId": "{{prodA}}", "cantidad": 4}]},
        # 8 × 4.50 = 36 > disponible 32,
        headers={"Idempotency-Key": "{{clave}}"}, pre=NUEVA_CLAVE,
        tests="pm.collectionVariables.set('ventaRechazada', pm.response.json().extra && pm.response.json().extra.ventaId);\n" + estado(409, "CLIENTE_LIMITE_EXCEDIDO")),
    req("Queda RECHAZADA con motivo", "GET", "/api/ventas/{{ventaRechazada}}",
        tests=estado(200) + "\npm.test('RECHAZADA', () => { pm.expect(pm.response.json().estado).to.eql('RECHAZADA'); pm.expect(pm.response.json().motivoRechazo).to.include('límite'); });"),
    req("Stock devuelto (sigue en 24)", "GET", "/api/productos/{{prodA}}", tests=estado(200) + "\npm.test('stock 24', () => pm.expect(pm.response.json().stock).to.eql(24));"),
)

carpeta("HU06–HU07 Pagos y estado de cuenta",
    req("Pago parcial → 201", "POST", "/api/clientes/{{cliOk}}/pagos", {"monto": 8, "metodo": "YAPE_PLIN"},
        tests=estado(201) + "\npm.test('saldoDespues 10', () => pm.expect(pm.response.json().saldoDespues).to.eql(10));"),
    req("Pago mayor al saldo → 409", "POST", "/api/clientes/{{cliOk}}/pagos", {"monto": 99, "metodo": "EFECTIVO"}, tests=estado(409, "PAGO_EXCEDE_SALDO")),
    req("Estado de cuenta en orden", "GET", "/api/clientes/{{cliOk}}/estado-cuenta",
        tests=estado(200) + "\nconst m = pm.response.json().movimientos;\npm.test('PAGO primero y saldo coherente', () => { pm.expect(m[0].tipo).to.eql('PAGO'); pm.expect(m[0].saldoDespues).to.eql(10); pm.expect(m[1].tipo).to.eql('CARGO'); });"),
    req("Desactivar cliente con saldo → 409", "DELETE", "/api/clientes/{{cliOk}}", tests=estado(409, "CLIENTE_CON_SALDO")),
    req("Mi cuenta (CLIENTE)", "GET", "/api/clientes/mi-cuenta", token="tokenCliente", tests=estado(200)),
)

carpeta("HU08 Alertas",
    req("Historial de alertas", "GET", "/api/alertas?limite=10", tests=estado(200) + "\npm.test('es lista', () => pm.expect(pm.response.json()).to.be.an('array'));"),
)

carpeta("HU09–HU10 Reportes",
    req("Deudores PDF", "GET", "/api/reportes/deudores.pdf", tests=estado(200) + "\npm.test('PDF', () => pm.expect(pm.response.headers.get('Content-Type')).to.include('application/pdf'));"),
    req("Deudores Excel", "GET", "/api/reportes/deudores.xlsx", tests=estado(200) + "\npm.test('xlsx', () => pm.expect(pm.response.headers.get('Content-Type')).to.include('spreadsheetml'));"),
    req("Consolidado con tiempos", "GET", "/api/reportes/consolidado?comparar=true",
        tests=estado(200) + "\npm.test('headers de tiempo', () => { pm.expect(pm.response.headers.has('X-Tiempo-Paralelo-Ms')).to.be.true; pm.expect(pm.response.headers.has('X-Tiempo-Secuencial-Ms')).to.be.true; });"),
    req("Resumen de ventas", "GET", "/api/reportes/ventas/resumen", tests=estado(200)),
)

carpeta("HU11 Reposiciones (SOAP)",
    req("Catálogo del distribuidor", "GET", "/api/reposiciones/catalogo", tests=estado(200)),
    req("Nuevo pedido → 201 con nroPedido", "POST", "/api/reposiciones", {"productoId": 1, "cantidad": 12},
        tests=estado(201) + "\npm.test('nroPedido', () => pm.expect(pm.response.json().nroPedido).to.match(/^PD-\\d{4}-\\d+/));"),
)

coleccion = {
    "info": {
        "name": "FiadoYa — Casos HU01–HU11",
        "description": "Casos del Anexo de Backend §13 a través del gateway. Ejecutar con: npx newman run tests/postman/FiadoYa.postman_collection.json -e tests/postman/local.postman_environment.json",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "item": items,
    "variable": [
        {"key": "token", "value": ""},
        {"key": "tokenCliente", "value": ""},
        {"key": "tokenInvalido", "value": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.firma-invalida"},
        {"key": "clave", "value": ""},
    ],
}
ruta = pathlib.Path(__file__).parent
(ruta / "FiadoYa.postman_collection.json").write_text(json.dumps(coleccion, indent=2, ensure_ascii=False))
(ruta / "local.postman_environment.json").write_text(json.dumps({
    "name": "FiadoYa local",
    "values": [
        {"key": "baseUrl", "value": "http://localhost:8080", "enabled": True},
        {"key": "adminUser", "value": "bodeguero", "enabled": True},
        {"key": "adminPassword", "value": "Cambiar_Esto_789", "enabled": True},
        {"key": "clientePassword", "value": "Demo12345", "enabled": True},
    ],
}, indent=2, ensure_ascii=False))
print("Colección generada")
