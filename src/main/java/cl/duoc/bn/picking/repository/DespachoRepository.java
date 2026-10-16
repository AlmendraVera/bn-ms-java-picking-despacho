package cl.duoc.bn.picking.repository;

import cl.duoc.bn.picking.model.Despacho;
import cl.duoc.bn.picking.model.EstadoDespacho;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    Optional<Despacho> findByOrdenId(Long ordenId);

    boolean existsByOrdenId(Long ordenId);

    List<Despacho> findByEstado(EstadoDespacho estado);
}