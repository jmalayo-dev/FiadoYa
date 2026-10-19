package pe.edu.cibertec.fiadoya.reportes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.Fechas;
import pe.edu.cibertec.fiadoya.reportes.dto.Archivo;
import pe.edu.cibertec.fiadoya.reportes.dto.ResumenVentas;
import pe.edu.cibertec.fiadoya.reportes.service.Consolidado;
import pe.edu.cibertec.fiadoya.reportes.service.GeneradorReportes;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('BODEGUERO')")
@Tag(name = "Reportes", description = "HU09 y HU10: deudores (PDF/Excel), resumen de ventas y consolidado en paralelo")
public class ReporteController {

    private final GeneradorReportes generador;
    private final Consolidado consolidado;

    public ReporteController(GeneradorReportes generador, Consolidado consolidado) {
        this.generador = generador;
        this.consolidado = consolidado;
    }

    @GetMapping("/deudores.pdf")
    @Operation(summary = "Deudores en PDF (JasperReports)")
    public ResponseEntity<byte[]> deudoresPdf() {
        return descargar(generador.deudoresPdf());
    }

    @GetMapping("/deudores.xlsx")
    @Operation(summary = "Deudores en Excel (Apache POI)")
    public ResponseEntity<byte[]> deudoresExcel() {
        return descargar(generador.deudoresExcel());
    }

    @GetMapping("/ventas/resumen")
    @Operation(summary = "Resumen de ventas confirmadas por día y top 5 productos (por defecto, últimos 7 días)")
    public ResumenVentas resumen(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate fin = hasta == null ? Fechas.hoy() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(6) : desde;
        if (inicio.isAfter(fin)) {
            throw ApiException.validacion("desde", "no puede ser posterior a hasta");
        }
        if (inicio.plusDays(366).isBefore(fin)) {
            throw ApiException.validacion("desde", "el rango máximo es de un año");
        }
        return generador.resumen(inicio, fin);
    }

    @GetMapping("/consolidado")
    @Operation(summary = "ZIP con los tres reportes generados en paralelo; comparar=true mide también la versión secuencial")
    public ResponseEntity<byte[]> consolidado(@RequestParam(defaultValue = "false") boolean comparar) {
        Consolidado.Resultado r = consolidado.generar(comparar);
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(r.nombre()).build().toString())
                .header("X-Tiempo-Paralelo-Ms", String.valueOf(r.paraleloMs()));
        if (r.secuencialMs() != null) {
            respuesta.header("X-Tiempo-Secuencial-Ms", String.valueOf(r.secuencialMs()));
        }
        return respuesta.body(r.zip());
    }

    private static ResponseEntity<byte[]> descargar(Archivo a) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(a.nombre()).build().toString())
                .body(a.contenido());
    }
}
