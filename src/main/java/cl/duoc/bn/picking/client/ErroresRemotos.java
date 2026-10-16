package cl.duoc.bn.picking.client;

import cl.duoc.bn.picking.exception.ApiException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

/** Traduce los errores HTTP de otro microservicio a una ApiException de este servicio. */
final class ErroresRemotos {

    private ErroresRemotos() {
    }

    static ApiException traducir(RestClientResponseException e, String servicio) {
        int codigo = e.getStatusCode().value();
        if (codigo >= 500) {
            return ApiException.servicioNoDisponible("El microservicio de " + servicio + " respondió con error " + codigo);
        }
        return new ApiException(HttpStatus.valueOf(codigo), "[" + servicio + "] " + extraerMensaje(e));
    }

    private static String extraerMensaje(RestClientResponseException e) {
        try {
            Map<?, ?> cuerpo = e.getResponseBodyAs(Map.class);
            if (cuerpo != null && cuerpo.get("mensaje") != null) {
                return cuerpo.get("mensaje").toString();
            }
        } catch (RuntimeException ignorado) {
            // si el cuerpo no es JSON usamos el mensaje genérico
        }
        return "error " + e.getStatusCode().value();
    }
}