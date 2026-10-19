package pe.edu.cibertec.fiadoya.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.cibertec.fiadoya.clientes.dto.CargoInput;
import pe.edu.cibertec.fiadoya.clientes.dto.PagoInput;
import pe.edu.cibertec.fiadoya.clientes.entity.Cargo;
import pe.edu.cibertec.fiadoya.clientes.entity.Cliente;
import pe.edu.cibertec.fiadoya.clientes.entity.MetodoPago;
import pe.edu.cibertec.fiadoya.clientes.repository.CargoRepository;
import pe.edu.cibertec.fiadoya.clientes.repository.ClienteRepository;
import pe.edu.cibertec.fiadoya.clientes.repository.MovimientoCuentaRepository;
import pe.edu.cibertec.fiadoya.clientes.repository.PagoRepository;
import pe.edu.cibertec.fiadoya.clientes.service.CuentaService;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.contracts.ClienteLimiteCercano;
import pe.edu.cibertec.fiadoya.contracts.PagoRegistrado;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    ClienteRepository clientes;
    @Mock
    CargoRepository cargos;
    @Mock
    PagoRepository pagos;
    @Mock
    MovimientoCuentaRepository movimientos;
    @Mock
    ApplicationEventPublisher eventos;

    CuentaService service;

    @BeforeEach
    void setUp() {
        service = new CuentaService(clientes, cargos, pagos, movimientos, eventos, new BigDecimal("0.90"));
    }

    static Cliente cliente(String limite, String saldo, boolean consentimiento) {
        Cliente c = new Cliente("María", "987654321", null, null, new BigDecimal(limite), consentimiento, null);
        ReflectionTestUtils.setField(c, "id", 1L);
        ReflectionTestUtils.setField(c, "saldo", new BigDecimal(saldo));
        return c;
    }

    private void prepararCargo(Cliente c) {
        when(cargos.findByVentaId(any())).thenReturn(Optional.empty());
        when(clientes.bloquear(1L)).thenReturn(Optional.of(c));
    }

    @Test
    void cargoDentroDelLimiteSubeElSaldo() {
        Cliente c = cliente("150.00", "85.50", true);
        prepararCargo(c);
        when(cargos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cargar(1L, new CargoInput(20L, new BigDecimal("25.30"), "3 productos", 3));

        assertThat(c.getSaldo()).isEqualByComparingTo("110.80");
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void cargoQueExcedeElLimiteDa409ConExtra() {
        Cliente c = cliente("100.00", "80.00", true);
        prepararCargo(c);

        assertThatThrownBy(() -> service.cargar(1L, new CargoInput(21L, new BigDecimal("35.50"), null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.CLIENTE_LIMITE_EXCEDIDO);
                    assertThat(e.getExtra()).containsKeys("saldo", "limite", "monto", "disponible");
                    assertThat((BigDecimal) e.getExtra().get("disponible")).isEqualByComparingTo("20.00");
                });
        assertThat(c.getSaldo()).isEqualByComparingTo("80.00");
    }

    @Test
    void sinConsentimientoNoSeFia() {
        prepararCargo(cliente("100.00", "0.00", false));
        assertThatThrownBy(() -> service.cargar(1L, new CargoInput(22L, BigDecimal.TEN, null, null)))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.CLIENTE_SIN_CONSENTIMIENTO);
    }

    @Test
    void cargoRepetidoDevuelveElMismo() {
        Cargo previo = new Cargo(1L, 23L, BigDecimal.TEN, null, null);
        when(cargos.findByVentaId(23L)).thenReturn(Optional.of(previo));

        var r = service.cargar(1L, new CargoInput(23L, BigDecimal.TEN, null, null));

        assertThat(r.ventaId()).isEqualTo(23L);
        verify(clientes, never()).bloquear(any());
    }

    @Test
    void cruzarEl90PorCientoAvisaUnaVez() {
        Cliente c = cliente("100.00", "80.00", true);
        prepararCargo(c);
        when(cargos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cargar(1L, new CargoInput(24L, new BigDecimal("12.00"), null, null));

        verify(eventos).publishEvent(any(ClienteLimiteCercano.class));
        assertThat(service.cruzaUmbral(new BigDecimal("92"), new BigDecimal("95"), new BigDecimal("100"))).isFalse();
    }

    @Test
    void pagoParcialBajaSaldoYPublicaEvento() {
        Cliente c = cliente("150.00", "85.50", true);
        when(clientes.bloquear(1L)).thenReturn(Optional.of(c));
        when(pagos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var pago = service.pagar(1L, new PagoInput(new BigDecimal("10.00"), MetodoPago.YAPE_PLIN));

        assertThat(pago.saldoDespues()).isEqualByComparingTo("75.50");
        assertThat(pago.metodo()).isEqualTo("YAPE_PLIN");
        verify(eventos).publishEvent(any(PagoRegistrado.class));
    }

    @Test
    void pagoMayorAlSaldoDa409() {
        when(clientes.bloquear(1L)).thenReturn(Optional.of(cliente("150.00", "20.00", true)));
        assertThatThrownBy(() -> service.pagar(1L, new PagoInput(new BigDecimal("25.00"), null)))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.PAGO_EXCEDE_SALDO);
    }

    @Test
    void anularEsIdempotente() {
        Cliente c = cliente("150.00", "50.00", true);
        Cargo cargo = new Cargo(1L, 25L, new BigDecimal("20.00"), null, null);
        when(cargos.findByVentaId(25L)).thenReturn(Optional.of(cargo));
        when(clientes.bloquear(1L)).thenReturn(Optional.of(c));

        service.anular(25L);
        service.anular(25L);

        assertThat(c.getSaldo()).isEqualByComparingTo("30.00");
        assertThat(cargo.isAnulado()).isTrue();
    }
}
