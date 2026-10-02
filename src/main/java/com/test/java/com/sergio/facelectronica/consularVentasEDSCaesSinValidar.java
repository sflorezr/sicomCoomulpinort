package com.test.java.com.sergio.facelectronica;

/**
 * Inicia el cargador de ventas Caes sin validar si el recibo ya existe en la observacion
 * de una factura de TNS (se usa como clase principal de CaesVentasSinValidar.jar).
 * El control local de recibos_subidos.txt se mantiene.
 */
public class consularVentasEDSCaesSinValidar {
    public static void main(String[] args){
        consularVentasEDSCaes.forzarSinValidar=true;
        consularVentasEDSCaes.main(args);
    }
}
