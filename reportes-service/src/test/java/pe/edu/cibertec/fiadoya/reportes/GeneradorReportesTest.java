package pe.edu.cibertec.fiadoya.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pe.edu.cibertec.fiadoya.reportes.client.ClientesClient.Deudor;
import pe.edu.cibertec.fiadoya.reportes.repository.ProductoVendidoRepository;
import pe.edu.cibertec.fiadoya.reportes.repository.VentaResumenRepository;
import pe.edu.cibertec.fiadoya.reportes.service.DatosDeudores;
import pe.edu.cibertec.fiadoya.reportes.service.GeneradorReportes;

/** Prueba de humo T0.6: JasperReports 7 compila la plantilla y genera un PDF; POI genera un Excel legible. */
class GeneradorReportesTest {

    private GeneradorReportes generador() {
        DatosDeudores datos = mock(DatosDeudores.class);
        when(datos.obtener()).thenReturn(List.of(
                new Deudor(1L, "María Quispe", "987654321", new BigDecimal("85.50"), new BigDecimal("150.00")),
                new Deudor(2L, "Jorge Huamán", "912345678", new BigDecimal("142.00"), new BigDecimal("150.00")),
                new Deudor(3L, "Luis Condori", "954112890", new BigDecimal("38.20"), new BigDecimal("150.00"))));
        return new GeneradorReportes(datos, mock(VentaResumenRepository.class), mock(ProductoVendidoRepository.class), "Bodega Test");
    }

    @Test
    void pdfDeDeudores() {
        var archivo = generador().deudoresPdf();
        assertThat(archivo.nombre()).matches("deudores-\\d{8}\\.pdf");
        assertThat(new String(archivo.contenido(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(archivo.contenido().length).isGreaterThan(1000);
    }

    @Test
    void excelDeDeudoresConTotalCorrecto() throws Exception {
        var archivo = generador().deudoresExcel();
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo.contenido()))) {
            var hoja = libro.getSheet("Deudores");
            assertThat(hoja.getRow(4).getCell(0).getStringCellValue()).isEqualTo("María Quispe");
            assertThat(hoja.getRow(8).getCell(2).getNumericCellValue()).isEqualTo(265.70);
        }
    }
}
