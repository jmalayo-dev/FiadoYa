package pe.edu.cibertec.fiadoya.ventas.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.ventas.client.ClientesClient;
import pe.edu.cibertec.fiadoya.ventas.client.InventarioClient;

/**
 * Envuelve cada llamada Feign en su circuit breaker. Un error de negocio (409) pasa tal cual;
 * caída, timeout o circuito abierto se convierten en SERVICIO_NO_DISPONIBLE (503).
 */
@Component
public class LlamadasRemotas {

    public static final String INVENTARIO = "inventario";
    public static final String CLIENTES = "clientes";

    private final InventarioClient inventario;
    private final ClientesClient clientes;
    private final CircuitBreakerFactory<?, ?> circuitos;

    public LlamadasRemotas(InventarioClient inventario, ClientesClient clientes, CircuitBreakerFactory<?, ?> circuitos) {
        this.inventario = inventario;
        this.clientes = clientes;
        this.circuitos = circuitos;
    }

    public InventarioClient.Resultado descontar(Long ventaId, List<InventarioClient.Item> items) {
        return llamar(INVENTARIO, () -> inventario.descontar(new InventarioClient.Lote(ventaId, items)));
    }

    public void devolver(Long ventaId) {
        llamar(INVENTARIO, () -> {
            inventario.devolver(new InventarioClient.Devolucion(ventaId));
            return null;
        });
    }

    public ClientesClient.Validacion validarCliente(Long clienteId) {
        return llamar(CLIENTES, () -> clientes.validar(clienteId));
    }

    public ClientesClient.Cargo cargar(Long clienteId, ClientesClient.CargoInput cargo) {
        return llamar(CLIENTES, () -> clientes.cargar(clienteId, cargo));
    }

    public void anularCargo(Long ventaId) {
        llamar(CLIENTES, () -> {
            clientes.anular(ventaId);
            return null;
        });
    }

    private <T> T llamar(String circuito, Supplier<T> llamada) {
        return circuitos.create(circuito).run(llamada, error -> {
            Throwable causa = desenvolver(error);
            if (causa instanceof ApiException negocio) {
                throw negocio;
            }
            throw new ApiException(ErrorCode.SERVICIO_NO_DISPONIBLE,
                    "El servicio de " + circuito + " no está disponible.", Map.of("servicio", circuito));
        });
    }

    private static Throwable desenvolver(Throwable t) {
        Throwable actual = t;
        while ((actual instanceof CompletionException || actual instanceof ExecutionException) && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }
}
