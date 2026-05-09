package com.sergio.prueba;

public class empleado {
    private String cedula;
    private String fecingreso;
    private String diasper;
    private String nombre1;
    private String nombre2;
    private String apellido1;
    private String apellido2;
    private String direccion;
    private String basico;
    private String banco;
    private String cuenta;
    private String salario;
    private String transporte;
    private String eps;
    private String pension;
    private String descuentos;
    private String tipo;
    private String estadodian;
    private String cune;
    


    public String getEstadodian() {
        return estadodian;
    }

    public void setEstadodian(String estadodian) {
        this.estadodian = estadodian;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public empleado(String cedula, String fecingreso, String diasper, String nombre1, String nombre2, String apellido1,
            String apellido2, String direccion, String basico, String banco, String cuenta, String salario,
            String transporte, String eps, String pension, String descuentos, String tipo,String estadodian,String cune) {
        this.cedula = cedula;
        this.fecingreso = fecingreso;
        this.diasper = diasper;
        this.nombre1 = nombre1;
        this.nombre2 = nombre2;
        this.apellido1 = apellido1;
        this.apellido2 = apellido2;
        this.direccion = direccion;
        this.basico = basico;
        this.banco = banco;
        this.cuenta = cuenta;
        this.salario = salario;
        this.transporte = transporte;
        this.eps = eps;
        this.pension = pension;
        this.descuentos = descuentos;
        this.tipo = tipo;
        this.estadodian=estadodian;
        this.cune=cune;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getFecingreso() {
        return fecingreso;
    }

    public void setFecingreso(String fecingreso) {
        this.fecingreso = fecingreso;
    }

    public String getDiasper() {
        return diasper;
    }

    public void setDiasper(String diasper) {
        this.diasper = diasper;
    }

    public String getNombre1() {
        return nombre1;
    }

    public void setNombre1(String nombre1) {
        this.nombre1 = nombre1;
    }

    public String getNombre2() {
        return nombre2;
    }

    public void setNombre2(String nombre2) {
        this.nombre2 = nombre2;
    }

    public String getApellido1() {
        return apellido1;
    }

    public void setApellido1(String apellido1) {
        this.apellido1 = apellido1;
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String apellido2) {
        this.apellido2 = apellido2;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getBasico() {
        return basico;
    }

    public void setBasico(String basico) {
        this.basico = basico;
    }

    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public String getSalario() {
        return salario;
    }

    public void setSalario(String salario) {
        this.salario = salario;
    }

    public String getTransporte() {
        return transporte;
    }

    public void setTransporte(String transporte) {
        this.transporte = transporte;
    }

    public String getEps() {
        return eps;
    }

    public void setEps(String eps) {
        this.eps = eps;
    }

    public String getPension() {
        return pension;
    }

    public void setPension(String pension) {
        this.pension = pension;
    }

    public String getDescuentos() {
        return descuentos;
    }

    public void setDescuentos(String descuentos) {
        this.descuentos = descuentos;
    }

    public String getCune() {
        return cune;
    }

    public void setCune(String cune) {
        this.cune = cune;
    }
}
