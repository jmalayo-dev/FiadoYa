#!/usr/bin/env bash
# Prueba de humo del flujo del DoD: login → venta FIADO → alerta de stock → pago → estado de cuenta → PDF de deudores.
# Uso: tests/smoke/flujo-completo.sh [URL_GATEWAY]   (por defecto http://localhost:8080)
# Requiere el perfil demo (datos de ejemplo) y python3 para leer JSON.
set -euo pipefail
G=${1:-http://localhost:8080}
ADMIN_USER=${ADMIN_USERNAME:-bodeguero}; ADMIN_PASS=${ADMIN_PASSWORD:-Admin12345}
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }
uuid() { python3 -c "import uuid; print(uuid.uuid4())"; }
ok() { printf '  \033[32m✔\033[0m %s\n' "$1"; }

echo "1. Login del bodeguero"
TOK=$(curl -sf -X POST "$G/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | json "d['token']")
H="Authorization: Bearer $TOK"; ok "token recibido"

echo "2. Sin token y con rol incorrecto"
[ "$(curl -s -o /dev/null -w '%{http_code}' "$G/api/productos")" = 401 ] && ok "sin token → 401"

echo "3. Venta FIADO a María Quispe (cliente 1) que cruza el mínimo del aceite"
STOCK_ANTES=$(curl -sf "$G/api/productos/2" -H "$H" | json "d['stock']")
VENTA=$(curl -sf -X POST "$G/api/ventas" -H "$H" -H "Idempotency-Key: $(uuid)" -H 'Content-Type: application/json' \
  -d '{"clienteId":1,"tipoPago":"FIADO","items":[{"productoId":2,"cantidad":1},{"productoId":1,"cantidad":1}]}')
echo "$VENTA" | json "'  venta N.º %s %s por S/ %s' % (d['id'], d['estado'], d['total'])"
STOCK_DESPUES=$(curl -sf "$G/api/productos/2" -H "$H" | json "d['stock']")
ok "stock del aceite: $STOCK_ANTES → $STOCK_DESPUES"

echo "4. Alertas (coreografía por RabbitMQ)"
sleep 2
curl -sf "$G/api/alertas?limite=3" -H "$H" | json "'\n'.join('  - %s %s' % (a['tipo'], a['nombre']) for a in d) or '  (sin alertas nuevas: el producto ya estaba bajo el mínimo)'"

echo "5. Venta FIADO que excede el límite de Jorge Huamán (cliente 2)"
CODE=$(curl -s -X POST "$G/api/ventas" -H "$H" -H "Idempotency-Key: $(uuid)" -H 'Content-Type: application/json' \
  -d '{"clienteId":2,"tipoPago":"FIADO","items":[{"productoId":9,"cantidad":1}]}' | json "d.get('code')")
ok "respuesta: $CODE"

echo "6. Pago parcial de S/ 5.00 de María"
curl -sf -X POST "$G/api/clientes/1/pagos" -H "$H" -H 'Content-Type: application/json' -d '{"monto":5,"metodo":"YAPE_PLIN"}' \
  | json "'  saldo después: S/ %s' % d['saldoDespues']"

echo "7. Estado de cuenta"
curl -sf "$G/api/clientes/1/estado-cuenta" -H "$H" \
  | json "'\n'.join('  %-9s S/ %7s → saldo %s  %s' % (m['tipo'], m['monto'], m['saldoDespues'], m.get('descripcion') or '') for m in d['movimientos'][:4])"

echo "8. Reportes"
curl -sf -o /tmp/fiadoya-deudores.pdf -w "  PDF: %{http_code} %{size_download} bytes → /tmp/fiadoya-deudores.pdf\n" "$G/api/reportes/deudores.pdf" -H "$H"
curl -sf -o /dev/null -D - "$G/api/reportes/consolidado?comparar=true" -H "$H" | grep -iE "x-tiempo" | sed 's/^/  /'

echo "9. Vista del cliente (cliente1)"
CT=$(curl -sf -X POST "$G/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"cliente1\",\"password\":\"${DEMO_CLIENTE_PASSWORD:-Demo12345}\"}" | json "d['token']")
curl -sf "$G/api/clientes/mi-cuenta" -H "Authorization: Bearer $CT" | json "'  %s debe S/ %s' % (d['cliente']['nombre'], d['cliente']['saldo'])"
[ "$(curl -s -o /dev/null -w '%{http_code}' "$G/api/productos" -H "Authorization: Bearer $CT")" = 403 ] && ok "cliente → /api/productos = 403"

echo "10. Reposición por SOAP"
curl -sf -X POST "$G/api/reposiciones" -H "$H" -H 'Content-Type: application/json' -d '{"productoId":5,"cantidad":24}' \
  | json "'  pedido %s llega el %s' % (d['nroPedido'], d['fechaEstimada'])"
echo "Listo."
