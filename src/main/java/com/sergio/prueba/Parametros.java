package com.sergio.prueba;

public class Parametros {
    String token;
    String endpoint;
    String endpointDv;
    String endpointFc;
    String endpointCt;
    String endpointDc;
    String endpointEv;

    String sendmail;

    public String getSendmail() {
        return sendmail;
    }

    public void setSendmail(String sendmail) {
        this.sendmail = sendmail;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

    public String getAtraso() {
        return atraso;
    }

    public void setAtraso(String atraso) {
        this.atraso = atraso;
    }

    String cantidad;
    String atraso;
    String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndpointEv() {
        return endpointEv;
    }

    public void setEndpointEv(String endpointEv) {
        this.endpointEv = endpointEv;
    }

    public String getEndpointDc() {
        return endpointDc;
    }

    public void setEndpointDc(String endpointDc) {
        this.endpointDc = endpointDc;
    }

    public String getEndpointFc() {
        return endpointFc;
    }

    public void setEndpointFc(String endpointFc) {
        this.endpointFc = endpointFc;
    }

    String endpointEmail;
    String footer;

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getCabecera() {
        return cabecera;
    }

    public void setCabecera(String cabecera) {
        this.cabecera = cabecera;
    }

    String cabecera;

    public String getEndpointEmail() {
        return endpointEmail;
    }

    public void setEndpointEmail(String endpointEmail) {
        this.endpointEmail = endpointEmail;
    }


    public Parametros(String token, String endpoint, String endpointDv, String endpointCt,String endpointEmail,String cabecera,String footer,String endpointFc,String endpointDc,String endpointEv,String sendmail,String cantidad,String atraso,String email) {
        this.token = token;
        this.endpoint = endpoint;
        this.endpointDv = endpointDv;
        this.endpointCt = endpointCt;
        this.endpointEmail=endpointEmail;
        this.cabecera=cabecera;
        this.footer=footer;
        this.endpointFc=endpointFc;
        this.endpointDc=endpointDc;
        this.endpointEv=endpointEv;
        this.sendmail=sendmail;
        this.cantidad=cantidad;
        this.atraso=atraso;
        this.email=email;
    }

    public String getToken() {
        return token;
    }

    public String getEndpointDv() {
        return endpointDv;
    }

    public void setEndpointDv(String endpointDv) {
        this.endpointDv = endpointDv;
    }

    public String getEndpointCt() {
        return endpointCt;
    }

    public void setEndpointCt(String endpointCt) {
        this.endpointCt = endpointCt;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }


}
