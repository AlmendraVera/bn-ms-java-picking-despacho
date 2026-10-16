package cl.duoc.bn.picking.client;

import cl.duoc.bn.picking.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Cliente HTTP hacia bn-ms-java-ordenes (lee la orden y actualiza su estado/tracking). */
@Component
public class OrdenesClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DetalleDto(String sku, Integer cantidad) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrdenDto(Long id, String codigoExterno, String cliente, String estado, List<DetalleDto> detalles) {
    }

    private record CambioEstado(String estado, String comentario) {
    }

    private final RestClient restClient;

    public OrdenesClient(@Value("${servicios.ordenes.url}") String baseUrl) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    public OrdenDto obtenerOrden(Long ordenId) {
        try {
            return restClient.get()
                    .uri("/api/ordenes/{id}", ordenId)
                    .retrieve()
                    .body(OrdenDto.class);
        } catch (RestClientResponseException e) {
            throw ErroresRemotos.traducir(e, "ordenes");
        } catch (ResourceAccessException e) {
            throw ApiException.servicioNoDisponible("El microservicio de ordenes no responde");
        }
    }

    public void cambiarEstado(Long ordenId, String estado, String comentario) {
        try {
            restClient.put()
                    .uri("/api/ordenes/{id}/estado", ordenId)
                    .body(new CambioEstado(estado, comentario))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw ErroresRemotos.traducir(e, "ordenes");
        } catch (ResourceAccessException e) {
            throw ApiException.servicioNoDisponible("El microservicio de ordenes no responde");
        }
    }
}