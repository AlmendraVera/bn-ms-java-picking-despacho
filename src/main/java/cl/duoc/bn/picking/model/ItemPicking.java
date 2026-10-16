package cl.duoc.bn.picking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Un producto que el operario debe recoger de la bodega para armar el despacho. */
@Entity
@Table(name = "items_picking")
public class ItemPicking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer cantidad;

    private boolean recogido;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "despacho_id")
    private Despacho despacho;

    public ItemPicking() {
    }

    public ItemPicking(Despacho despacho, String sku, Integer cantidad) {
        this.despacho = despacho;
        this.sku = sku;
        this.cantidad = cantidad;
        this.recogido = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public boolean isRecogido() {
        return recogido;
    }

    public void setRecogido(boolean recogido) {
        this.recogido = recogido;
    }

    public Despacho getDespacho() {
        return despacho;
    }

    public void setDespacho(Despacho despacho) {
        this.despacho = despacho;
    }
}