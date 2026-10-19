package pe.edu.cibertec.fiadoya.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/** Se carga sola en cada servicio que dependa de common-web. */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class CommonWebAutoConfiguration {
}
