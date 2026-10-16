package cl.duoc.bn.picking.service;

import cl.duoc.bn.picking.client.InventarioClient;
import cl.duoc.bn.picking.client.InventarioClient.DisponibilidadDto;
import cl.duoc.bn.picking.client.OrdenesClient;
import cl.duoc.bn.picking.client.OrdenesClient.DetalleDto;
import cl.duoc.bn.picking.client.OrdenesClient.OrdenDto;
import cl.duoc.bn.picking.exception.ApiException;
import cl.duoc.bn.picking.model.Despacho;
import cl.duoc.bn.picking.model.EstadoDespacho;
import cl.duoc.bn.picking.model.ItemPicking;
import cl.duoc.bn.picking.repository.DespachoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo de bodega: crear despacho -> iniciar picking -> recoger ítems -> despachar.
 * Al despachar descuenta el stock en inventario y marca la orden como DESPACHADA.
 */
@Service
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final OrdenesClient ordenesClient;
    private final InventarioClient inventarioClient;

    public DespachoService(DespachoRepository despachoRepository, OrdenesClient ordenesClient,
            InventarioClient inventarioClient) {
        this.despachoRepository = despachoRepository;
        this.ordenesClient = ordenesClient;
        this.inventarioClient = inventarioClient;
    }

    public List<Despacho> listar(EstadoDespacho estado) {
        if (estado != null) {
            return despachoRepository.findByEstado(estado);
        }
        return despachoRepository.findAll();
    }

    public Despacho buscarPorId(Long id) {
        return despachoRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe un despacho con id " + id));
    }

    public Despacho buscarPorOrden(Long ordenId) {
        return despachoRepository.findByOrdenId(ordenId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe un despacho para la orden " + ordenId));
    }

    /** Crea el despacho de una orden CREADA, con un ítem de picking por cada línea de la orden. */
    @Transactional
    public Despacho crear(Long ordenId) {
        if (ordenId == null) {
            throw ApiException.solicitudInvalida("El ordenId es obligatorio");
        }
        if (despachoRepository.existsByOrdenId(ordenId)) {
            throw ApiException.conflicto("Ya existe un despacho para la orden " + ordenId);
        }
        OrdenDto orden = ordenesClient.obtenerOrden(ordenId);
        if (!"CREADA".equals(orden.estado())) {
            throw ApiException.conflicto("La orden " + ordenId + " está en estado " + orden.estado()
                    + "; solo se puede preparar el despacho de órdenes CREADA");
        }
        Despacho despacho = new Despacho();
        despacho.setOrdenId(ordenId);
        despacho.setEstado(EstadoDespacho.PENDIENTE);
        despacho.setFechaCreacion(LocalDateTime.now());
        for (DetalleDto d : orden.detalles()) {
            despacho.getItems().add(new ItemPicking(despacho, d.sku(), d.cantidad()));
        }
        return despachoRepository.save(despacho);
    }

    @Transactional
    public Despacho iniciarPicking(Long id, String operario) {
        if (operario == null || operario.isBlank()) {
            throw ApiException.solicitudInvalida("El operario es obligatorio");
        }
        Despacho despacho = buscarPorId(id);
        exigirEstado(despacho, EstadoDespacho.PENDIENTE, "iniciar el picking");
        ordenesClient.cambiarEstado(despacho.getOrdenId(), "EN_PICKING", "Picking iniciado por " + operario.trim());
        despacho.setOperario(operario.trim());
        despacho.setEstado(EstadoDespacho.EN_PICKING);
        return despachoRepository.save(despacho);
    }

    @Transactional
    public Despacho marcarRecogido(Long despachoId, Long itemId) {
        Despacho despacho = buscarPorId(despachoId);
        exigirEstado(despacho, EstadoDespacho.EN_PICKING, "recoger productos");
        ItemPicking item = despacho.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ApiException.noEncontrado("El despacho " + despachoId
                        + " no tiene un ítem con id " + itemId));
        item.setRecogido(true);
        return despachoRepository.save(despacho);
    }

    /** Verifica stock de todos los ítems, lo descuenta en inventario y marca la orden como DESPACHADA. */
    @Transactional
    public Despacho despachar(Long id) {
        Despacho despacho = buscarPorId(id);
        exigirEstado(despacho, EstadoDespacho.EN_PICKING, "despachar");

        long pendientes = despacho.getItems().stream().filter(i -> !i.isRecogido()).count();
        if (pendientes > 0) {
            throw ApiException.conflicto("Faltan " + pendientes + " ítem(s) por recoger antes de despachar");
        }
        // 1) Verificar stock de todo antes de descontar nada
        for (ItemPicking item : despacho.getItems()) {
            DisponibilidadDto disp = inventarioClient.verificarDisponibilidad(item.getSku(), item.getCantidad());
            if (!Boolean.TRUE.equals(disp.disponible())) {
                throw ApiException.conflicto("Stock insuficiente para el SKU " + item.getSku()
                        + ": disponible " + disp.stockActual() + ", requerido " + item.getCantidad());
            }
        }
        // 2) Descontar stock
        for (ItemPicking item : despacho.getItems()) {
            inventarioClient.descontar(item.getSku(), item.getCantidad());
        }
        // 3) Actualizar la orden y cerrar el despacho
        String codigo = String.format("DSP-%06d", despacho.getId());
        ordenesClient.cambiarEstado(despacho.getOrdenId(), "DESPACHADA", "Despachada con código " + codigo);
        despacho.setEstado(EstadoDespacho.DESPACHADO);
        despacho.setFechaDespacho(LocalDateTime.now());
        despacho.setCodigoSeguimiento(codigo);
        return despachoRepository.save(despacho);
    }

    public void eliminar(Long id) {
        Despacho despacho = buscarPorId(id);
        exigirEstado(despacho, EstadoDespacho.PENDIENTE, "eliminar el despacho");
        despachoRepository.delete(despacho);
    }

    private void exigirEstado(Despacho despacho, EstadoDespacho requerido, String accion) {
        if (despacho.getEstado() != requerido) {
            throw ApiException.conflicto("No se puede " + accion + ": el despacho está " + despacho.getEstado()
                    + " y debe estar " + requerido);
        }
    }
}