package pe.edu.cibertec.fiadoya.notificaciones.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pe.edu.cibertec.fiadoya.notificaciones.dto.Alerta;
import pe.edu.cibertec.fiadoya.notificaciones.service.AlertasStore;
import pe.edu.cibertec.fiadoya.notificaciones.service.SseHub;

@RestController
@RequestMapping("/api/alertas")
@PreAuthorize("hasRole('BODEGUERO')")
@Tag(name = "Alertas", description = "HU08: alertas de stock bajo y límite de crédito en tiempo real")
public class AlertaController {

    private final SseHub sse;
    private final AlertasStore store;

    public AlertaController(SseHub sse, AlertasStore store) {
        this.sse = sse;
        this.store = store;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream SSE: event 'alerta' con cada alerta nueva y ': ping' cada 25 s")
    public SseEmitter stream(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return sse.conectar();
    }

    @GetMapping
    @Operation(summary = "Historial de alertas, más reciente primero (tipo=STOCK_BAJO|LIMITE_CLIENTE)")
    public List<Alerta> historial(@RequestParam(defaultValue = "50") int limite,
                                  @RequestParam(required = false) String tipo) {
        return store.ultimas(Math.max(1, Math.min(limite, 100)), tipo);
    }
}
