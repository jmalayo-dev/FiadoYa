package pe.edu.cibertec.fiadoya.distribuidora;

import jakarta.jws.WebService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import pe.edu.cibertec.fiadoya.distribuidora.ws.DistribuidoraPortType;
import pe.edu.cibertec.fiadoya.distribuidora.ws.ItemCatalogo;
import pe.edu.cibertec.fiadoya.distribuidora.ws.Pedido;

/**
 * Implementa el portType generado desde el WSDL (JAX-WS). JAXB convierte los objetos a XML.
 * nroPedido = "PD-" + año + correlativo; fechaEstimada = hoy + 2 días hábiles (Anexo §9).
 */
@WebService(serviceName = "DistribuidoraService", portName = "DistribuidoraPort",
        targetNamespace = "http://fiadoya.local/distribuidora",
        endpointInterface = "pe.edu.cibertec.fiadoya.distribuidora.ws.DistribuidoraPortType",
        wsdlLocation = "classpath:wsdl/distribuidora.wsdl")
public class DistribuidoraEndpoint implements DistribuidoraPortType {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final CatalogoDistribuidora catalogo;
    private final AtomicInteger correlativo = new AtomicInteger();

    public DistribuidoraEndpoint(CatalogoDistribuidora catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public List<ItemCatalogo> consultarCatalogo() {
        return catalogo.todos().stream().map(a -> {
            ItemCatalogo item = new ItemCatalogo();
            item.setCodigo(a.codigo());
            item.setNombre(a.nombre());
            item.setPrecio(a.precio());
            item.setDisponible(a.disponible());
            return item;
        }).toList();
    }

    @Override
    public Pedido registrarPedido(String codigo, int cantidad) {
        if (cantidad <= 0 || cantidad > 9999) {
            throw new IllegalArgumentException("Cantidad fuera de rango: " + cantidad);
        }
        if (catalogo.porCodigo(codigo).isEmpty()) {
            throw new IllegalArgumentException("Código no existe en el catálogo: " + codigo);
        }
        LocalDate hoy = LocalDate.now(LIMA);
        Pedido respuesta = new Pedido();
        respuesta.setNroPedido("PD-%d-%05d".formatted(Year.from(hoy).getValue(), correlativo.incrementAndGet()));
        respuesta.setFechaEstimada(aXml(sumarDiasHabiles(hoy, 2)));
        return respuesta;
    }

    static LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        LocalDate fecha = desde;
        int contados = 0;
        while (contados < dias) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                contados++;
            }
        }
        return fecha;
    }

    private static XMLGregorianCalendar aXml(LocalDate fecha) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(fecha.toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
