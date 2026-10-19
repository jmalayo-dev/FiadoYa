package pe.edu.cibertec.fiadoya.reportes.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.Fechas;
import pe.edu.cibertec.fiadoya.reportes.dto.Archivo;

/**
 * Concurrencia (Plan §5.4 y Anexo §11): genera deudores PDF, deudores Excel y el resumen de ventas
 * en paralelo con CompletableFuture sobre un ExecutorService, y los entrega en un ZIP.
 * Con comparar=true también los genera uno tras otro y devuelve ambos tiempos.
 */
@Service
public class Consolidado {

    private static final Logger log = LoggerFactory.getLogger(Consolidado.class);

    private final GeneradorReportes generador;
    private final ExecutorService ejecutor;

    public Consolidado(GeneradorReportes generador, ExecutorService ejecutorReportes) {
        this.generador = generador;
        this.ejecutor = ejecutorReportes;
    }

    public Resultado generar(boolean comparar) {
        LocalDate hasta = Fechas.hoy();
        LocalDate desde = hasta.minusDays(29);
        List<Supplier<Archivo>> tareas = List.of(generador::deudoresPdf, generador::deudoresExcel,
                () -> generador.resumenJson(desde, hasta));

        long t0 = System.nanoTime();
        List<CompletableFuture<Archivo>> futuros = tareas.stream()
                .map(t -> CompletableFuture.supplyAsync(t, ejecutor)).toList();
        List<Archivo> archivos;
        try {
            archivos = futuros.stream().map(CompletableFuture::join).toList();
        } catch (CompletionException e) {
            if (e.getCause() instanceof ApiException api) {
                throw api;
            }
            throw e;
        }
        long paralelo = (System.nanoTime() - t0) / 1_000_000;

        Long secuencial = null;
        if (comparar) {
            long t1 = System.nanoTime();
            tareas.forEach(Supplier::get);
            secuencial = (System.nanoTime() - t1) / 1_000_000;
            log.info("Consolidado: paralelo {} ms vs secuencial {} ms", paralelo, secuencial);
        }
        return new Resultado(zip(archivos), "reportes-" + hasta + ".zip", paralelo, secuencial);
    }

    private static byte[] zip(List<Archivo> archivos) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Archivo a : archivos) {
                zip.putNextEntry(new ZipEntry(a.nombre()));
                zip.write(a.contenido());
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Resultado(byte[] zip, String nombre, long paraleloMs, Long secuencialMs) {
    }
}
