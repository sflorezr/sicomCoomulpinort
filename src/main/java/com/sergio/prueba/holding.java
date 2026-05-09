package com.sergio.prueba;

public class holding {
    private String tax_id;
    private String valordto;
    private String base;
    private String porc;

    public holding(String tax_id, String valordto, String base, String porc) {
        this.tax_id = tax_id;
        this.valordto = valordto;
        this.base = base;
        this.porc = porc;
    }

    public void setTax_id(String tax_id) {
        this.tax_id = tax_id;
    }

    public void setValordto(String valordto) {
        this.valordto = valordto;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public void setPorc(String porc) {
        this.porc = porc;
    }

    public String getTax_id() {
        return tax_id;
    }

    public String getValordto() {
        return valordto;
    }

    public String getBase() {
        return base;
    }

    public String getPorc() {
        return porc;
    }
}
