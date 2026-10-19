package pe.edu.cibertec.fiadoya.reportes.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.common.web.Dinero;
import pe.edu.cibertec.fiadoya.common.web.Fechas;
import pe.edu.cibertec.fiadoya.reportes.client.ClientesClient.Deudor;
import pe.edu.cibertec.fiadoya.reportes.dto.Archivo;
import pe.edu.cibertec.fiadoya.reportes.dto.ResumenVentas;
import pe.edu.cibertec.fiadoya.reportes.entity.ProductoVendido;
import pe.edu.cibertec.fiadoya.reportes.entity.VentaResumen;
import pe.edu.cibertec.fiadoya.reportes.repository.ProductoVendidoRepository;
import pe.edu.cibertec.fiadoya.reportes.repository.VentaResumenRepository;
import tools.jackson.databind.json.JsonMapper;

/** HU09 y HU10: deudores en PDF (JasperReports 7) y Excel (Apache POI), y resumen de ventas. */
@Service
public class GeneradorReportes {

    private static final DateTimeFormatter ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter LEGIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DatosDeudores deudores;
    private final VentaResumenRepository ventas;
    private final ProductoVendidoRepository productos;
    private final String bodega;
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    private volatile JasperReport plantillaDeudores;

    public GeneradorReportes(DatosDeudores deudores, VentaResumenRepository ventas, ProductoVendidoRepository productos,
                             @Value("${BODEGA_NOMBRE:Bodega Don Carlos}") String bodega) {
        this.deudores = deudores;
        this.ventas = ventas;
        this.productos = productos;
        this.bodega = bodega;
    }

    public Archivo deudoresPdf() {
        List<Deudor> lista = deudores.obtener();
        List<Map<String, ?>> filas = new ArrayList<>();
        for (Deudor d : lista) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("cliente", d.nombre());
            fila.put("telefono", d.telefono());
            fila.put("saldo", d.saldo());
            fila.put("limite", d.limiteCredito());
            fila.put("usado", porcentaje(d));
            filas.add(fila);
        }
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("BODEGA", bodega);
        parametros.put("FECHA", LocalDateTime.now(Fechas.LIMA).format(LEGIBLE));
        parametros.put("TOTAL", total(lista));
        parametros.put("CANTIDAD", lista.size());
        try {
            JasperPrint print = JasperFillManager.fillReport(plantilla(), parametros, new JRMapCollectionDataSource(filas));
            return new Archivo("deudores-" + hoy() + ".pdf", "application/pdf", JasperExportManager.exportReportToPdf(print));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de deudores", e);
        }
    }

    public Archivo deudoresExcel() {
        List<Deudor> lista = deudores.obtener();
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Deudores");
            CellStyle encabezado = estiloEncabezado(libro);
            CellStyle moneda = libro.createCellStyle();
            moneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));
            CellStyle negrita = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            negrita.setFont(fuente);

            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue("FiadoYa · " + bodega + " — Deudores");
            titulo.getCell(0).setCellStyle(negrita);
            hoja.createRow(1).createCell(0).setCellValue("Generado: " + LocalDateTime.now(Fechas.LIMA).format(LEGIBLE) + " (Lima)");

            String[] columnas = {"Cliente", "Teléfono", "Saldo (S/)", "Límite (S/)", "% usado"};
            Row cab = hoja.createRow(3);
            for (int i = 0; i < columnas.length; i++) {
                Cell c = cab.createCell(i);
                c.setCellValue(columnas[i]);
                c.setCellStyle(encabezado);
            }
            int fila = 4;
            for (Deudor d : lista) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(d.nombre());
                r.createCell(1).setCellValue(d.telefono());
                celdaMoneda(r, 2, d.saldo(), moneda);
                celdaMoneda(r, 3, d.limiteCredito(), moneda);
                r.createCell(4).setCellValue(porcentaje(d));
            }
            Row pie = hoja.createRow(fila + 1);
            pie.createCell(0).setCellValue("Total por cobrar (" + lista.size() + " clientes)");
            pie.getCell(0).setCellStyle(negrita);
            celdaMoneda(pie, 2, total(lista), moneda);
            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }
            libro.write(out);
            return new Archivo("deudores-" + hoy() + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel de deudores", e);
        }
    }

    /** HU10: solo ventas CONFIRMADA (son las únicas que publican venta.registrada); días en hora de Lima. */
    @Transactional(readOnly = true)
    public ResumenVentas resumen(LocalDate desde, LocalDate hasta) {
        var inicio = Fechas.inicioDelDia(desde);
        var fin = Fechas.finDelDia(hasta);
        List<VentaResumen> lista = ventas.findByFechaGreaterThanEqualAndFechaLessThan(inicio, fin);
        Map<LocalDate, long[]> cantidades = new TreeMap<>();
        Map<LocalDate, BigDecimal> montos = new TreeMap<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            cantidades.put(d, new long[]{0});
            montos.put(d, BigDecimal.ZERO);
        }
        for (VentaResumen v : lista) {
            LocalDate dia = Fechas.diaEnLima(v.getFecha());
            cantidades.computeIfAbsent(dia, k -> new long[]{0})[0]++;
            montos.merge(dia, v.getTotal(), BigDecimal::add);
        }
        List<ResumenVentas.PorDia> porDia = cantidades.keySet().stream()
                .map(d -> new ResumenVentas.PorDia(d, cantidades.get(d)[0], Dinero.redondear(montos.get(d)))).toList();

        Map<Long, ResumenVentas.TopProducto> agregados = new LinkedHashMap<>();
        for (ProductoVendido p : productos.findByFechaGreaterThanEqualAndFechaLessThan(inicio, fin)) {
            agregados.merge(p.getProductoId(),
                    new ResumenVentas.TopProducto(p.getProductoId(), p.getNombreProducto(), p.getCantidad(), p.getMonto()),
                    (a, b) -> new ResumenVentas.TopProducto(a.productoId(), a.nombre(), a.cantidad() + b.cantidad(),
                            a.monto().add(b.monto())));
        }
        List<ResumenVentas.TopProducto> top = agregados.values().stream()
                .sorted(Comparator.comparing(ResumenVentas.TopProducto::monto).reversed())
                .limit(5)
                .map(t -> new ResumenVentas.TopProducto(t.productoId(), t.nombre(), t.cantidad(), Dinero.redondear(t.monto())))
                .toList();
        BigDecimal montoTotal = Dinero.redondear(lista.stream().map(VentaResumen::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new ResumenVentas(desde, hasta, lista.size(), montoTotal, porDia, top);
    }

    public Archivo resumenJson(LocalDate desde, LocalDate hasta) {
        return new Archivo("resumen-ventas-" + desde + "_" + hasta + ".json", "application/json",
                json.writerWithDefaultPrettyPrinter().writeValueAsBytes(resumen(desde, hasta)));
    }

    /** La plantilla .jrxml se compila una vez y se reutiliza. */
    private JasperReport plantilla() throws Exception {
        JasperReport r = plantillaDeudores;
        if (r == null) {
            synchronized (this) {
                if (plantillaDeudores == null) {
                    try (InputStream in = getClass().getResourceAsStream("/reports/deudores.jrxml")) {
                        plantillaDeudores = JasperCompileManager.compileReport(in);
                    }
                }
                r = plantillaDeudores;
            }
        }
        return r;
    }

    private static int porcentaje(Deudor d) {
        if (d.limiteCredito() == null || d.limiteCredito().signum() == 0) {
            return 100;
        }
        return d.saldo().multiply(BigDecimal.valueOf(100)).divide(d.limiteCredito(), 0, RoundingMode.HALF_UP).intValue();
    }

    private static BigDecimal total(List<Deudor> lista) {
        return Dinero.redondear(lista.stream().map(Deudor::saldo).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static String hoy() {
        return Fechas.hoy().format(ARCHIVO);
    }

    private static void celdaMoneda(Row r, int col, BigDecimal valor, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellValue(valor == null ? 0 : valor.doubleValue());
        c.setCellStyle(estilo);
    }

    private static CellStyle estiloEncabezado(XSSFWorkbook libro) {
        CellStyle s = libro.createCellStyle();
        Font f = libro.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }
}
