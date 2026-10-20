# Prueba de carga (JMeter)

Meta del Plan §9: **50 usuarios concurrentes** sobre `POST /api/ventas` y `GET /api/clientes/{id}/estado-cuenta`, **p95 < 2 s**.

Con el stack levantado (perfil `demo`), sin instalar JMeter, usando Docker:

```bash
# desde backend/
docker run --rm --network fiadoya -v "$PWD/tests/jmeter:/plan" -v "$PWD/docs/handoff/pruebas:/salida" \
  alpine/jmeter:5.6.3 -n -t /plan/fiadoya-carga.jmx \
  -Jhost=api-gateway -Jport=8080 -Jusuarios=50 -Jiteraciones=20 -JadminPassword="$ADMIN_PASSWORD" \
  -l /salida/jmeter-resultados.jtl -e -o /salida/jmeter-reporte
```

El reporte HTML queda en `docs/handoff/pruebas/jmeter-reporte/index.html` (pestaña *Statistics*, columna *95th pct*).
Con JMeter instalado: `jmeter -n -t tests/jmeter/fiadoya-carga.jmx -Jhost=localhost -Jport=8080 ...`.
