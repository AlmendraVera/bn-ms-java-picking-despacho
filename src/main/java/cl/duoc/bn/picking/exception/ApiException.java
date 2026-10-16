package cl.duoc.bn.picking.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio: lleva el código HTTP que debe devolver la API.
 * La captura ManejadorExcepciones y la convierte en una respuesta JSON uniforme.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus estado;

    public ApiException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public static ApiException solicitudInvalida(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);          // 400
    }

    public static ApiException noAutorizado(String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensaje);         // 401
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje);            // 403
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje);            // 404
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);             // 409
    }

    public static ApiException servicioNoDisponible(String mensaje) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, mensaje);  // 503
    }
}