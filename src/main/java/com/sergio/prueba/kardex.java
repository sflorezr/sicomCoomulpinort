package com.sergio.prueba;

public class kardex {
    private String kardexid;
    private String tipoDocument;
    private String numero;
    private String fecha;
    private String hora;
    private String formapago;
    private String plazodias;
    private String fecvence;
    private String base;
    private String vriva;
    private String total;
    private String documento;
    private String telefono;
    private String direccion;
    private String email;
    private String tipoIdentificacion;
    private String tipoOrganizacion;
    private String municipio;
    private String tipoRegimen;
    private String nombre;
    private String resolucion;
    private String contingencia;
    private String prefe;
    private String numerodev;
    private String cufedev;
    private String fechadev;
    private String motivo;
    private String notes;
    private String dv;

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getCountry_id() {
        return country_id;
    }

    public void setCountry_id(String country_id) {
        this.country_id = country_id;
    }

    private String exportacion;
    private String factorconv;
    private String ciudad;
    private String departamento;
    private String country_id;

    public String getFactorconv() {
        return factorconv;
    }

    public void setFactorconv(String factorconv) {
        this.factorconv = factorconv;
    }

    public String getExportacion() {
        return exportacion;
    }

    public void setExportacion(String exportacion) {
        this.exportacion = exportacion;
    }

    public String getDv() {
        return dv;
    }

    public void setDv(String dv) {
        this.dv = dv;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getNumerodev() {
        return numerodev;
    }

    public void setNumerodev(String numerodev) {
        this.numerodev = numerodev;
    }

    public String getCufedev() {
        return cufedev;
    }

    public void setCufedev(String cufedev) {
        this.cufedev = cufedev;
    }

    public String getFechadev() {
        return fechadev;
    }

    public void setFechadev(String fechadev) {
        this.fechadev = fechadev;
    }

    public kardex(String kardexid, String tipoDocument, String numero, String fecha, String hora, String formapago, String plazodias, String fecvence, String base, String vriva, String total, String documento, String telefono, String direccion, String email, String tipoIdentificacion, String tipoOrganizacion, String municipio, String tipoRegimen, String nombre, String resolucion, String contingencia, String prefe, String numerodev, String cufedev, String fechadev,String motivo,String notes,String dv,String exportacion,String factorconv,String ciudad,String departamento,String country_id) {
        this.kardexid = kardexid;
        this.tipoDocument = tipoDocument;
        this.numero = numero;
        this.fecha = fecha;
        this.hora = hora;
        this.formapago = formapago;
        this.plazodias = plazodias;
        this.fecvence = fecvence;
        this.base = base;
        this.vriva = vriva;
        this.total = total;
        this.documento = documento;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
        this.tipoIdentificacion = tipoIdentificacion;
        this.tipoOrganizacion = tipoOrganizacion;
        this.municipio = municipio;
        this.tipoRegimen = tipoRegimen;
        this.nombre = nombre;
        this.resolucion = resolucion;
        this.contingencia = contingencia;
        this.prefe = prefe;
        this.numerodev = numerodev;
        this.cufedev = cufedev;
        this.fechadev = fechadev;
        this.motivo = motivo;
        this.notes = notes;
        this.dv=dv;
        this.exportacion=exportacion;
        this.factorconv=factorconv;
        this.ciudad=ciudad;
        this.departamento=departamento;
        this.country_id=country_id;
    }


    public String getContingencia() {
        return contingencia;
    }

    public void setContingencia(String contingencia) {
        this.contingencia = contingencia;
    }

    public String getPrefe() {
        return prefe;
    }

    public void setPrefe(String prefe) {
        this.prefe = prefe;
    }

    public String getResolucion() {
        return resolucion;
    }

    public void setResolucion(String resolucion) {
        this.resolucion = resolucion;
    }
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }


    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTipoIdentificacion() {
        return tipoIdentificacion;
    }

    public void setTipoIdentificacion(String tipoIdentificacion) {
        this.tipoIdentificacion = tipoIdentificacion;
    }

    public String getTipoOrganizacion() {
        return tipoOrganizacion;
    }

    public void setTipoOrganizacion(String tipoOrganizacion) {
        this.tipoOrganizacion = tipoOrganizacion;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getTipoRegimen() {
        return tipoRegimen;
    }

    public void setTipoRegimen(String tipoRegimen) {
        this.tipoRegimen = tipoRegimen;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getFormapago() {
        return formapago;
    }

    public void setFormapago(String formapago) {
        this.formapago = formapago;
    }

    public String getPlazodias() {
        return plazodias;
    }

    public void setPlazodias(String plazodias) {
        this.plazodias = plazodias;
    }

    public String getFecvence() {
        return fecvence;
    }

    public void setFecvence(String fecvence) {
        this.fecvence = fecvence;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getVriva() {
        return vriva;
    }

    public void setVriva(String vrbase) {
        this.vriva = vrbase;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }


    public String getTipoDocument() {
        return tipoDocument;
    }

    public void setTipoDocument(String tipoDocument) {
        this.tipoDocument = tipoDocument;
    }

    public String getKardexid() {
        return kardexid;
    }

    public void setKardexid(String kardexid) {
        this.kardexid = kardexid;
    }

}
