package com.test.java.com.sergio.facelectronica;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.*;

/**
 * Reversa en TNS las ventas de un prefijo (por defecto PO) creando una devolucion por cada factura
 * (/v2/facturacion/Devolucion/Crear) con los mismos articulos, cantidades y valores.
 * El API de TNS no permite eliminar ni anular facturas, por eso se reversan con una devolucion.
 * Usa la conexion de caes.properties (login TNS de consularVentasEDSCaes) y estos parametros opcionales:
 *   reversar.prefijo=PO                      prefijo de las facturas a reversar
 *   reversar.prefijoDevolucion=00            prefijo de las devoluciones
 *   reversar.motivo=01                       codigo del motivo de devolucion en TNS (si no esta, se pide en pantalla)
 *   reversar.usarFechaFactura=S              S: devolucion con la fecha de la factura, N: con la fecha actual
 *   reversar.referencia=auto                 como se indica la factura devuelta: prefijo-en-numero
 *                                            (numeroFacturaDevolucion=PO123), prefijo-factura (devolucion con el
 *                                            prefijo de la factura), solo-numero; auto prueba en ese orden
 * El banco, la talla y el color se toman de tns.banco, tns.talla y tns.color (por defecto 00).
 * Las facturas reversadas se registran en reversadas_<prefijo>.txt para no reversarlas dos veces,
 * y el resultado queda en logs/reversar_*.log.
 */
public class ReversarVentasPO {
    private static String prefijoFacturas="PO";
    private static String prefijoDevolucion="00";
    private static String motivo="";
    private static boolean usarFechaFactura=true;
    private static Set<String> reversadas=new HashSet<>();
    private static final int ERRORESSEGUIDOS=5;
    // formas de indicar a TNS la factura que se devuelve, en el orden en que se prueban
    private static final String[] FORMASREFERENCIA={"prefijo-en-numero", "prefijo-factura", "solo-numero"};
    private static String formaReferencia=null;

    private static JFrame ventana;
    private static JButton botonBuscar;
    private static JProgressBar barra;
    private static JLabel estado;

    public static void main(String[] args){
        java.util.Locale.setDefault(new java.util.Locale("es", "CO"));
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> {
            if(consularVentasEDSCaes.CargarConfiguracion()){
                prefijoFacturas=consularVentasEDSCaes.config.getProperty("reversar.prefijo","PO").trim();
                prefijoDevolucion=consularVentasEDSCaes.config.getProperty("reversar.prefijoDevolucion","00").trim();
                motivo=consularVentasEDSCaes.config.getProperty("reversar.motivo","").trim();
                String forma=consularVentasEDSCaes.config.getProperty("reversar.referencia","auto").trim();
                if(java.util.Arrays.asList(FORMASREFERENCIA).contains(forma)){
                    formaReferencia=forma;
                }
                usarFechaFactura=!consularVentasEDSCaes.config.getProperty("reversar.usarFechaFactura","S").trim().equalsIgnoreCase("N");
                CrearVentana();
            }else{
                System.exit(0);
            }
        });
    }

    private static void CrearVentana(){
        ventana=new JFrame("Reversar ventas "+prefijoFacturas+" en TNS");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel=new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints c=new GridBagConstraints();
        c.insets=new Insets(5, 5, 5, 5);
        c.fill=GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=0; panel.add(new JLabel("<html>Crea una devolucion en TNS por cada factura con prefijo <b>"+prefijoFacturas
            +"</b>.<br>Prefijo de la devolucion: <b>"+prefijoDevolucion+"</b></html>"), c);
        botonBuscar=new JButton("Buscar ventas "+prefijoFacturas);
        botonBuscar.addActionListener(e -> Buscar());
        c.gridy=1; panel.add(botonBuscar, c);
        barra=new JProgressBar();
        barra.setStringPainted(true);
        barra.setString("");
        c.gridy=2; panel.add(barra, c);
        estado=new JLabel("Presione el boton para buscar las ventas a reversar.");
        c.gridy=3; panel.add(estado, c);
        ventana.getContentPane().add(panel, BorderLayout.CENTER);
        ventana.pack();
        ventana.setSize(Math.max(ventana.getWidth(), 460), ventana.getHeight());
        ventana.setResizable(false);
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
    }
    private static void Estado(String texto){
        SwingUtilities.invokeLater(() -> estado.setText(texto));
    }

    /**
     * Paso 1: lista las facturas del prefijo y pide confirmacion.
     */
    private static void Buscar(){
        try {
            consularVentasEDSCaes.AbrirLog("reversar");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(ventana, "No fue posible crear el archivo de log.\n"+e.getMessage(), "Reversar ventas", JOptionPane.ERROR_MESSAGE);
            return;
        }
        consularVentasEDSCaes.GuardarLog("INICIO DE LA BUSQUEDA DE VENTAS CON PREFIJO "+prefijoFacturas);
        botonBuscar.setEnabled(false);
        barra.setIndeterminate(true);
        barra.setString("Buscando ventas...");
        new SwingWorker<List<JSONObject>, Void>() {
            boolean sinSesion=false;
            JSONArray motivos=null;
            int yaReversadas=0;
            @Override
            protected List<JSONObject> doInBackground() throws Exception {
                CargarReversadas();
                Estado("Iniciando sesion en TNS...");
                if(!consularVentasEDSCaes.LoginTNS()){
                    sinSesion=true;
                    return null;
                }
                Estado("Buscando ventas "+prefijoFacturas+"...");
                HttpUrl urlListar=HttpUrl.parse(consularVentasEDSCaes.URLTNS+"/v2/facturacion/Ventas/Listar").newBuilder()
                    .addQueryParameter("codigosucursal", consularVentasEDSCaes.sucursalTNS)
                    .addQueryParameter("filtro", prefijoFacturas).build();
                Response response=consularVentasEDSCaes.EjecutarTNS(urlListar, null);
                String respuesta=response.body().string();
                JSONObject rta=consularVentasEDSCaes.LeerRespuestaTNS(respuesta);
                JSONArray filas=rta.optJSONArray("data");
                if(response.code()!=200 || filas==null){
                    consularVentasEDSCaes.GuardarLog("ERROR "+response.code()+" LISTANDO LAS VENTAS: "+consularVentasEDSCaes.Recortar(respuesta));
                    return null;
                }
                List<JSONObject> facturas=new ArrayList<>();
                for (int i = 0; i < filas.length(); i++) {
                    JSONObject factura=filas.getJSONObject(i);
                    // el filtro busca por prefijo/numero, solo se toman las del prefijo exacto
                    if(!factura.optString("codigoPrefijo").trim().equalsIgnoreCase(prefijoFacturas)){
                        continue;
                    }
                    if(reversadas.contains(factura.optString("kardexId").trim())){
                        yaReversadas++;
                        continue;
                    }
                    facturas.add(factura);
                }
                consularVentasEDSCaes.GuardarLog("FACTURAS "+prefijoFacturas+" POR REVERSAR: "+facturas.size()+" (YA REVERSADAS ANTES: "+yaReversadas+")");
                if(!facturas.isEmpty()){
                    Estado("Consultando los motivos de devolucion...");
                    motivos=ConsultarMotivos();
                }
                return facturas;
            }
            @Override
            protected void done() {
                barra.setIndeterminate(false);
                barra.setString("");
                List<JSONObject> facturas;
                try {
                    facturas=get();
                } catch (Exception e) {
                    consularVentasEDSCaes.GuardarLog("ERROR BUSCANDO LAS VENTAS: "+consularVentasEDSCaes.CausaDe(e));
                    Finalizar("Ocurrio un error buscando las ventas.", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(sinSesion){
                    Finalizar("No fue posible iniciar sesion en TNS. Revise los datos tns.* de caes.properties.", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(facturas==null){
                    Finalizar("No fue posible listar las ventas en TNS (ver log).", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String aviso=yaReversadas>0 ? "\n"+yaReversadas+" factura(s) ya fueron reversadas anteriormente y se omiten." : "";
                if(facturas.isEmpty()){
                    Finalizar("No hay ventas "+prefijoFacturas+" por reversar."+aviso, JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if(!ElegirMotivo(motivos)){
                    Finalizar("Reversion cancelada. No se selecciono un motivo de devolucion valido (ver log).", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                double total=0;
                for (JSONObject factura : facturas) {
                    total+=Numero(factura.optString("valorNeto"));
                }
                Object[] opciones={"Reversar todas", "Solo la primera (prueba)", "Cancelar"};
                int opcion=JOptionPane.showOptionDialog(ventana,
                    "Se encontraron "+facturas.size()+" facturas "+prefijoFacturas+" por reversar"
                    +"\npor un valor neto de $"+String.format("%,.2f", total)+"."+aviso
                    +"\n\nSe creara una devolucion (prefijo "+prefijoDevolucion+", motivo "+motivo+") por cada factura."
                    +"\nEsta operacion no se puede deshacer desde este programa.",
                    "Confirmar reversion", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, opciones, opciones[1]);
                if(opcion==1){
                    facturas=facturas.subList(0, 1);
                }else if(opcion!=0){
                    consularVentasEDSCaes.GuardarLog("REVERSION CANCELADA POR EL USUARIO");
                    Finalizar("Reversion cancelada. No se modifico ninguna venta.", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                Reversar(new ArrayList<>(facturas));
            }
        }.execute();
    }

    /**
     * Paso 2: crea las devoluciones mostrando el avance.
     */
    private static void Reversar(List<JSONObject> facturas){
        barra.setMinimum(0);
        barra.setMaximum(facturas.size());
        barra.setValue(0);
        new SwingWorker<int[], Integer>() {
            @Override
            protected int[] doInBackground() throws Exception {
                int reversadasOk=0;
                int conError=0;
                int erroresSeguidos=0;
                for (int i = 0; i < facturas.size(); i++) {
                    if(erroresSeguidos>=ERRORESSEGUIDOS && !ContinuarConErrores(erroresSeguidos)){
                        consularVentasEDSCaes.GuardarLog("REVERSION DETENIDA POR EL USUARIO DESPUES DE "+erroresSeguidos+" ERRORES SEGUIDOS");
                        break;
                    }
                    if(erroresSeguidos>=ERRORESSEGUIDOS){
                        erroresSeguidos=0;
                    }
                    JSONObject factura=facturas.get(i);
                    Estado("Reversando factura "+(i+1)+" de "+facturas.size()+"...");
                    try {
                        if(ReversarFactura(factura)){
                            reversadasOk++;
                            erroresSeguidos=0;
                        }else{
                            conError++;
                            erroresSeguidos++;
                        }
                    } catch (Exception e) {
                        conError++;
                        erroresSeguidos++;
                        consularVentasEDSCaes.GuardarLog("ERROR REVERSANDO LA FACTURA "+NombreFactura(factura)+": "+consularVentasEDSCaes.CausaDe(e));
                    }
                    publish(i+1);
                }
                return new int[]{reversadasOk, conError};
            }
            @Override
            protected void process(List<Integer> avance) {
                int valor=avance.get(avance.size()-1);
                barra.setValue(valor);
                barra.setString(valor+" / "+facturas.size());
            }
            @Override
            protected void done() {
                int[] resultado;
                try {
                    resultado=get();
                } catch (Exception e) {
                    consularVentasEDSCaes.GuardarLog("ERROR REVERSANDO LAS VENTAS: "+consularVentasEDSCaes.CausaDe(e));
                    Finalizar("Ocurrio un error reversando las ventas.", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                consularVentasEDSCaes.GuardarLog("FIN DE LA REVERSION. REVERSADAS: "+resultado[0]+" CON ERROR: "+resultado[1]);
                Finalizar("Reversion terminada.\n\nFacturas seleccionadas: "+facturas.size()+"\nReversadas: "+resultado[0]+"\nCon error: "+resultado[1],
                    resultado[1]>0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    /**
     * Consulta los motivos de devolucion de TNS (codigo, descripcion). Devuelve null si no se pudo.
     */
    private static JSONArray ConsultarMotivos() throws IOException{
        Response response=consularVentasEDSCaes.EjecutarTNS(HttpUrl.parse(consularVentasEDSCaes.URLTNS+"/v2/Interaccion/ObtenerMotivos"), null);
        String respuesta=response.body().string();
        JSONArray motivos=consularVentasEDSCaes.LeerRespuestaTNS(respuesta).optJSONArray("data");
        if(response.code()!=200 || motivos==null){
            consularVentasEDSCaes.GuardarLog("ERROR "+response.code()+" CONSULTANDO LOS MOTIVOS DE DEVOLUCION: "+consularVentasEDSCaes.Recortar(respuesta));
            return null;
        }
        return motivos;
    }
    /**
     * Valida reversar.motivo contra los motivos de TNS o pide elegir uno. Devuelve false si no hay motivo.
     */
    private static boolean ElegirMotivo(JSONArray motivos){
        if(motivos==null || motivos.length()==0){
            if(motivo.isEmpty()){
                consularVentasEDSCaes.GuardarLog("NO SE PUDIERON CONSULTAR LOS MOTIVOS DE DEVOLUCION Y NO HAY reversar.motivo EN caes.properties");
                return false;
            }
            consularVentasEDSCaes.GuardarLog("NO SE PUDIERON CONSULTAR LOS MOTIVOS DE DEVOLUCION, SE USA reversar.motivo="+motivo);
            return true;
        }
        List<String> opciones=new ArrayList<>();
        String seleccionada=null;
        for (int i = 0; i < motivos.length(); i++) {
            JSONObject m=motivos.optJSONObject(i);
            if(m==null){
                continue;
            }
            String opcion=m.optString("codigo").trim()+" - "+m.optString("descripcion").trim();
            opciones.add(opcion);
            if(!motivo.isEmpty() && (m.optString("codigo").trim().equalsIgnoreCase(motivo) || m.optString("descripcion").trim().equalsIgnoreCase(motivo))){
                seleccionada=opcion;
            }
        }
        if(seleccionada==null){
            if(!motivo.isEmpty()){
                consularVentasEDSCaes.GuardarLog("EL MOTIVO '"+motivo+"' DE caes.properties NO EXISTE EN TNS, SE PIDE SELECCIONARLO");
            }
            Object elegido=JOptionPane.showInputDialog(ventana, "Seleccione el motivo de devolucion de TNS para las reversiones:",
                "Motivo de devolucion", JOptionPane.QUESTION_MESSAGE, null, opciones.toArray(), opciones.isEmpty() ? null : opciones.get(0));
            if(elegido==null){
                consularVentasEDSCaes.GuardarLog("NO SE SELECCIONO MOTIVO DE DEVOLUCION");
                return false;
            }
            seleccionada=elegido.toString();
        }
        motivo=seleccionada.substring(0, seleccionada.indexOf(" - ")).trim();
        consularVentasEDSCaes.GuardarLog("MOTIVO DE DEVOLUCION: "+seleccionada);
        return true;
    }

    /**
     * Pregunta (desde el hilo de trabajo) si se continua despues de varios errores seguidos.
     */
    private static boolean ContinuarConErrores(int errores){
        final int[] opcion={JOptionPane.NO_OPTION};
        try {
            SwingUtilities.invokeAndWait(() -> opcion[0]=JOptionPane.showConfirmDialog(ventana,
                "Se presentaron "+errores+" errores seguidos al reversar (ver log).\n\nDesea continuar con las demas facturas?",
                "Reversar ventas", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE));
        } catch (Exception e) {
            return false;
        }
        return opcion[0]==JOptionPane.YES_OPTION;
    }

    /**
     * Consulta el detalle de la factura y crea su devolucion. Devuelve true si TNS la acepto.
     */
    public static Boolean ReversarFactura(JSONObject factura) throws IOException{
        String kardexId=factura.optString("kardexId").trim();
        String numero=factura.optString("numero").trim();
        HttpUrl urlDetalle=HttpUrl.parse(consularVentasEDSCaes.URLTNS+"/v2/facturacion/Ventas/Detallar").newBuilder()
            .addQueryParameter("kardexid", kardexId)
            .addQueryParameter("codigosucursal", consularVentasEDSCaes.sucursalTNS).build();
        Response response=consularVentasEDSCaes.EjecutarTNS(urlDetalle, null);
        String respuesta=response.body().string();
        JSONObject detalle=consularVentasEDSCaes.LeerRespuestaTNS(respuesta).optJSONObject("data");
        if(response.code()!=200 || detalle==null){
            consularVentasEDSCaes.GuardarLog("ERROR "+response.code()+" CONSULTANDO EL DETALLE DE LA FACTURA "+NombreFactura(factura)+": "+consularVentasEDSCaes.Recortar(respuesta));
            return false;
        }
        JSONArray lineas=detalle.optJSONArray("detallesVenta");
        if(lineas==null || lineas.length()==0){
            consularVentasEDSCaes.GuardarLog("LA FACTURA "+NombreFactura(factura)+" NO TIENE DETALLE, NO SE REVERSA");
            return false;
        }
        String fecha=LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if(usarFechaFactura){
            String fechaFactura=FechaTNS(detalle.optString("fecha", factura.optString("fecha")));
            if(fechaFactura.isEmpty()){
                consularVentasEDSCaes.GuardarLog("NO SE RECONOCIO LA FECHA '"+detalle.optString("fecha")+"' DE LA FACTURA "+NombreFactura(factura)+", SE USA LA FECHA ACTUAL");
            }else{
                fecha=fechaFactura;
            }
        }
        String formaPago=detalle.optString("formaPago").trim().toUpperCase();
        formaPago=formaPago.startsWith("CR") ? "CR" : "CO";
        String observacion=("REVERSA FACTURA "+NombreFactura(factura)+" "+detalle.optString("observacion").trim()).trim();
        if(observacion.length()>200){
            observacion=observacion.substring(0, 200);
        }
        JsonObject devolucion=new JsonObject();
        devolucion.addProperty("numero", numero);
        devolucion.addProperty("motivo", motivo);
        devolucion.addProperty("fecha", fecha);
        devolucion.addProperty("fechaVence", fecha);
        String tercero=Primero(detalle.optString("codigoTercero"), factura.optString("codigoTercero"));
        devolucion.addProperty("codTercero", tercero);
        devolucion.addProperty("codVendedor", Primero(detalle.optString("codigoVendedor"), factura.optString("codigoVendedor")));
        devolucion.addProperty("codDespachar", Primero(detalle.optString("codigoDespachar"), tercero));
        devolucion.addProperty("codFormaPago", formaPago);
        devolucion.addProperty("codBanco", consularVentasEDSCaes.bancoTNS);
        devolucion.addProperty("plazoDias", 0);
        devolucion.addProperty("observacion", observacion);
        JsonArray detallePedido=new JsonArray();
        double total=0;
        for (int i = 0; i < lineas.length(); i++) {
            JSONObject linea=lineas.getJSONObject(i);
            JsonObject item=new JsonObject();
            item.addProperty("codMat", linea.optString("codigoArticulo"));
            item.addProperty("codBodega", Primero(linea.optString("codigoBodega"), consularVentasEDSCaes.bodega));
            item.addProperty("codTalla", consularVentasEDSCaes.talla);
            item.addProperty("codColor", consularVentasEDSCaes.color);
            item.addProperty("cantidad", Numero(linea.optString("cantidad")));
            item.addProperty("tipoUnidad", "D");
            item.addProperty("descuento", 0);
            item.addProperty("porcIva", Numero(linea.optString("porcentaIva")));
            item.addProperty("valor", Numero(linea.optString("valorBase")));
            item.addProperty("impConsumo", 0);
            item.addProperty("observacion", "");
            detallePedido.add(item);
            total+=Numero(linea.optString("valorParcial"));
        }
        devolucion.add("detallePedido", detallePedido);
        JsonArray detalleFormaPago=new JsonArray();
        JsonObject pago=new JsonObject();
        pago.addProperty("codigoFormaPago", formaPago);
        pago.addProperty("plazoDias", "0");
        pago.addProperty("fechaVencimiento", fecha);
        pago.addProperty("valor", String.valueOf(total));
        detalleFormaPago.add(pago);
        devolucion.add("detalleFormaPago", detalleFormaPago);

        HttpUrl urlDevolucion=HttpUrl.parse(consularVentasEDSCaes.URLTNS+"/v2/facturacion/Devolucion/Crear").newBuilder()
            .addQueryParameter("codigosucursal", consularVentasEDSCaes.sucursalTNS).build();
        String prefijoFactura=factura.optString("codigoPrefijo").trim();
        // el Swagger no indica como se referencia la factura: mientras no se conozca la forma se prueban
        // las variantes en orden (un rechazo de TNS no crea nada) y se usa la primera que TNS acepte
        String[] formas=formaReferencia!=null ? new String[]{formaReferencia} : FORMASREFERENCIA;
        for (int intento = 0; intento < formas.length; intento++) {
            String forma=formas[intento];
            String prefijoDv=forma.equals("prefijo-factura") ? prefijoFactura : prefijoDevolucion;
            devolucion.addProperty("codigoPrefijo", prefijoDv);
            devolucion.addProperty("numeroFacturaDevolucion", forma.equals("prefijo-en-numero") ? prefijoFactura+numero : numero);
            RequestBody body=RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(devolucion));
            Response response2=consularVentasEDSCaes.EjecutarTNS(urlDevolucion, body);
            String respuesta2=response2.body().string();
            JSONObject rta=consularVentasEDSCaes.LeerRespuestaTNS(respuesta2);
            JSONObject datos=rta.optJSONObject("data");
            if(response2.code()!=200 || !rta.optBoolean("status") || (datos!=null && !datos.optBoolean("success"))){
                String mensaje=consularVentasEDSCaes.MensajeErrorTNS(rta, respuesta2);
                boolean facturaNoEncontrada=mensaje.toLowerCase().contains("no existe") && mensaje.toLowerCase().contains("factura");
                if(formaReferencia==null && facturaNoEncontrada && intento<formas.length-1){
                    consularVentasEDSCaes.GuardarLog("TNS NO ENCONTRO LA FACTURA "+NombreFactura(factura)+" CON LA FORMA '"+forma+"', SE PRUEBA LA SIGUIENTE");
                    continue;
                }
                consularVentasEDSCaes.GuardarLog("ERROR "+response2.code()+" "+mensaje+" REVERSANDO LA FACTURA "+NombreFactura(factura));
                consularVentasEDSCaes.GuardarLog("    ENVIADO: "+new Gson().toJson(devolucion));
                return false;
            }
            if(formaReferencia==null){
                formaReferencia=forma;
                consularVentasEDSCaes.GuardarLog("FORMA DE REFERENCIAR LA FACTURA ACEPTADA POR TNS: '"+forma+"' (prefijo devolucion "+prefijoDv+", numeroFacturaDevolucion "+devolucion.get("numeroFacturaDevolucion").getAsString()+")");
            }
            String numeroDevolucion=numero;
            if(datos!=null && !datos.optString("consecutivo").trim().isEmpty()){
                numeroDevolucion=datos.optString("consecutivo").trim();
            }
            RegistrarReversada(factura, prefijoDv+numeroDevolucion);
            consularVentasEDSCaes.GuardarLog("FACTURA "+NombreFactura(factura)+" REVERSADA CON LA DEVOLUCION "+prefijoDv+numeroDevolucion);
            return true;
        }
        return false;
    }

    private static String Primero(String valor,String alterno){
        return valor==null || valor.trim().isEmpty() ? alterno : valor.trim();
    }
    private static String NombreFactura(JSONObject factura){
        return factura.optString("codigoPrefijo").trim()+factura.optString("numero").trim();
    }
    /**
     * Convierte la fecha que devuelve TNS (yyyy-MM-dd, yyyy/MM/dd, dd/MM/yyyy, dd-MM-yyyy) a dd/MM/yyyy. Vacio si no se reconoce.
     */
    static String FechaTNS(String fecha){
        fecha=fecha.trim();
        if(fecha.matches("^\\d{4}-\\d{2}-\\d{2}.*")){
            return fecha.substring(8, 10)+"/"+fecha.substring(5, 7)+"/"+fecha.substring(0, 4);
        }
        if(fecha.matches("^\\d{4}/\\d{2}/\\d{2}.*")){
            return fecha.substring(8, 10)+"/"+fecha.substring(5, 7)+"/"+fecha.substring(0, 4);
        }
        if(fecha.matches("^\\d{2}/\\d{2}/\\d{4}.*")){
            return fecha.substring(0, 10);
        }
        if(fecha.matches("^\\d{2}-\\d{2}-\\d{4}.*")){
            return fecha.substring(0, 10).replace("-", "/");
        }
        return "";
    }
    /**
     * Convierte a numero los valores que TNS devuelve como texto (1234.5, 1.234,5 o 1234,5).
     */
    static double Numero(String valor){
        String limpio=valor.trim().replace(" ", "").replace("$", "");
        if(limpio.contains(",") && limpio.contains(".")){
            if(limpio.lastIndexOf(',')>limpio.lastIndexOf('.')){
                limpio=limpio.replace(".", "").replace(",", ".");
            }else{
                limpio=limpio.replace(",", "");
            }
        }else if(limpio.contains(",")){
            limpio=limpio.replace(",", ".");
        }
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Registro local de facturas reversadas (kardexId;factura;devolucion;fecha).
     */
    private static File ArchivoReversadas(){
        return new File(consularVentasEDSCaes.CarpetaAplicacion(), "reversadas_"+prefijoFacturas+".txt");
    }
    private static void CargarReversadas() throws IOException{
        reversadas.clear();
        File archivo=ArchivoReversadas();
        if(!archivo.exists()){
            return;
        }
        try (BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea=br.readLine())!=null) {
                String kardexId=linea.split(";")[0].trim();
                if(!kardexId.isEmpty()){
                    reversadas.add(kardexId);
                }
            }
        }
    }
    private static synchronized void RegistrarReversada(JSONObject factura,String devolucion){
        String kardexId=factura.optString("kardexId").trim();
        reversadas.add(kardexId);
        try (Writer w=new OutputStreamWriter(new FileOutputStream(ArchivoReversadas(), true), StandardCharsets.UTF_8)) {
            w.write(kardexId+";"+NombreFactura(factura)+";"+devolucion+";"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))+"\r\n");
        } catch (IOException e) {
            consularVentasEDSCaes.GuardarLog("NO FUE POSIBLE REGISTRAR LA FACTURA "+NombreFactura(factura)+" COMO REVERSADA: "+e.getMessage());
        }
    }

    private static void Finalizar(String mensaje,int tipo){
        File archivo=consularVentasEDSCaes.CerrarLogYDevolverArchivo();
        estado.setText("Proceso terminado. Log: "+(archivo!=null ? archivo.getName() : ""));
        botonBuscar.setEnabled(true);
        JOptionPane.showMessageDialog(ventana, mensaje, "Reversar ventas", tipo);
        consularVentasEDSCaes.AbrirArchivo(ventana, archivo);
    }
}
