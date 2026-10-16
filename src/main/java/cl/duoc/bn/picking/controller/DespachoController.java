package cl.duoc.bn.picking.controller;

import cl.duoc.bn.picking.model.Despacho;
import cl.duoc.bn.picking.model.EstadoDespacho;
import cl.duoc.bn.picking.service.DespachoService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/despachos")
public class DespachoController {

    public record CrearDespachoRequest(Long ordenId) {
    }

    public record IniciarPickingRequest(String operario) {
    }

    private final DespachoService despachoService;

    public DespachoController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    @PostMapping
    public ResponseEntity<Despacho> crear(@RequestBody CrearDespachoRequest peticion) {
        Despacho creado = despachoService.crear(peticion.ordenId());
        return ResponseEntity.created(URI.create("/api/despachos/" + creado.getId())).body(creado);
    }

    /** GET /api/despachos  o  GET /api/despachos?estado=EN_PICKING */
    @GetMapping
    public List<Despacho> listar(@RequestParam(required = false) EstadoDespacho estado) {
        return despachoService.listar(estado);
    }

    @GetMapping("/{id}")
    public Despacho buscarPorId(@PathVariable Long id) {
        return despachoService.buscarPorId(id);
    }

    @GetMapping("/orden/{ordenId}")
    public Despacho buscarPorOrden(@PathVariable Long ordenId) {
        return despachoService.buscarPorOrden(ordenId);
    }

    @PostMapping("/{id}/iniciar-picking")
    public Despacho iniciarPicking(@PathVariable Long id, @RequestBody IniciarPickingRequest peticion) {
        return despachoService.iniciarPicking(id, peticion.operario());
    }

    @PutMapping("/{id}/items/{itemId}/recoger")
    public Despacho recogerItem(@PathVariable Long id, @PathVariable Long itemId) {
        return despachoService.marcarRecogido(id, itemId);
    }

    @PostMapping("/{id}/despachar")
    public Despacho despachar(@PathVariable Long id) {
        return despachoService.despachar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        despachoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}