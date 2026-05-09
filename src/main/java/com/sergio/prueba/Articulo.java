package com.sergio.prueba;

public class Articulo {

    public Articulo(String unidad, String cantidad, String parcvta, String porcdescuento, String porciva, String vriva, String preciobase, String descripcion, String codigo) {
        this.unidad = unidad;
        this.cantidad = cantidad;
        this.parcvta = parcvta;
        this.porcdescuento = porcdescuento;
        this.porciva = porciva;
        this.vriva = vriva;
        this.preciobase = preciobase;
        this.descripcion = descripcion;
        this.codigo = codigo;
    }
    private String unidad;
    private String cantidad;
    private String parcvta;
    private String porcdescuento;
    private String porciva;
    private String vriva;
    private String preciobase;
    private String descripcion;
    private String codigo;

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

    public String getParcvta() {
        return parcvta;
    }

    public void setParcvta(String parcvta) {
        this.parcvta = parcvta;
    }

    public String getPorcdescuento() {
        return porcdescuento;
    }

    public void setPorcdescuento(String porcdescuento) {
        this.porcdescuento = porcdescuento;
    }

    public String getPorciva() {
        return porciva;
    }

    public void setPorciva(String porciva) {
        this.porciva = porciva;
    }

    public String getVriva() {
        return vriva;
    }

    public void setVriva(String vriva) {
        this.vriva = vriva;
    }

    public String getPreciobase() {
        return preciobase;
    }

    public void setPreciobase(String preciobase) {
        this.preciobase = preciobase;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

}
