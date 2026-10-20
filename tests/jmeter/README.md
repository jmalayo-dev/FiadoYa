# Prueba de carga (JMeter)

Meta del Plan §9: **50 usuarios concurrentes** sobre `POST /api/ventas` y `GET /api/clientes/{id}/estado-cuenta`, **p95 < 2 s**.

Con el stack levantado (perfil `demo`) y JMeter 5.6.3 instalado (https://jmeter.apache.org):

```bash
# desde backend/
jmeter -n -t tests/jmeter/fiadoya-carga.jmx -Jhost=localhost -Jport=8080 -Jusuarios=50 -Jiteraciones=20 \
  -JadminPassword="$ADMIN_PASSWORD" -l docs/handoff/pruebas/jmeter-resultados.jtl -e -o docs/handoff/pruebas/jmeter-reporte
```

El reporte HTML queda en `docs/handoff/pruebas/jmeter-reporte/index.html` (pestaña *Statistics*, columna *95th pct*).
Resultado de referencia (19/10/2026, Docker local): 2002 peticiones, 0 % errores, p95 POST /api/ventas = 258 ms, p95 estado de cuenta = 30 ms.
