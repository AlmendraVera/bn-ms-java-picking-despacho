package cl.duoc.bn.picking.client;

import cl.duoc.bn.picking.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Cliente HTTP hacia bn-ms-java-inventario (consulta y descuenta stock al despachar). */
@Component
public class InventarioClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DisponibilidadDto(String sku, Integer cantidadSolicitada, Integer stockActual, Boolean disponible) {
    }

    private record Cantidad(Integer cantidad) {
    }

    private final RestClient restClient;

    public InventarioClient(@Value("${servicios.inventario.url}") String baseUrl) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    public DisponibilidadDto verificarDisponibilidad(String sku, int cantidad) {
        try {
            return restClient.get()
                    .uri("/api/productos/sku/{sku}/disponibilidad?cantidad={cantidad}", sku, cantidad)
                    .retrieve()
                    .body(DisponibilidadDto.class);
        } catch (RestClientResponseException e) {
            throw ErroresRemotos.traducir(e, "inventario");
        } catch (ResourceAccessException e) {
            throw ApiException.servicioNoDisponible("El microservicio de inventario no responde");
        }
    }

    public void descontar(String sku, int cantidad) {
        try {
            restClient.post()
                    .uri("/api/productos/sku/{sku}/descontar", sku)
                    .body(new Cantidad(cantidad))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw ErroresRemotos.traducir(e, "inventario");
        } catch (ResourceAccessException e) {
            throw ApiException.servicioNoDisponible("El microservicio de inventario no responde");
        }
    }
}