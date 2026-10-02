package com.test.java.com.sergio.facelectronica;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.*;

/**
 * Aplicacion de escritorio que consulta las ventas de la estacion (api/Estaciones/{id}/ventas/date/{fecha})
 * entre una fecha inicial y una final, muestra cuantas hay, y si el usuario confirma las sube a TNS
 * por API v2 (api.tns.co/v2/facturacion/Ventas/Crear) con token Bearer obtenido en /v2/Acceso/Login.
 * Los datos de conexion se leen de caes.properties y el resultado queda en logs/caes_*.log,
 * ambos en la carpeta del jar. El numero de la factura lo asigna TNS (consecutivo del prefijo),
 * salvo en las ventas con factura electronica de la estacion, y los recibos ya subidos se registran
 * en recibos_subidos.txt para no subirlos de nuevo. Antes de subir tambien se omiten los recibos
 * que ya aparecen en la observacion de una factura de TNS (reporte ObtenerVentasDetallada).
 */
public class consularVentasEDSCaes {
    private static final String ARCHIVOCONFIG="caes.properties";
    private static final String ARCHIVORECIBOS="recibos_subidos.txt";
    private static final String CONSUMIDORFINAL="222222222222";
    private static final DateTimeFormatter FORMATOFECHA=DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static String URLTNS="https://api.tns.co";
    private static String url="";
    private static String idEstacion="";
    private static String usuarioTNS="";
    private static String passwordTNS="";    
    private static String empresaTNS="";  
    static String sucursalTNS="00";
    static Properties config=new Properties();
    static String bodega="00";
    static String talla="00";
    static String color="00";
    static String bancoTNS="00";
    private static String centroCosto="00";
    private static String prefijo="FE";
    private static DateTimeFormatter formatoFechaReporte=DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static String token="";
    private static String tokenTNS="";
    private static String json =null; 
    private static Map<String,String> tercerosConsultados = new HashMap<>();
    private static PrintWriter log=null;
    private static File archivoLog=null;
    private static java.util.Set<String> recibosSubidos=new java.util.HashSet<>();

    private static JFrame ventana;
    private static JSpinner fechaInicial;
    private static JSpinner fechaFinal;
    private static JButton botonProcesar;
    private static JProgressBar barra;
    private static JLabel estado;

    public static void main(String[] args){
        java.util.Locale.setDefault(new java.util.Locale("es", "CO"));
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> {
            if(CargarConfiguracion()){
                CrearVentana();
            }else{
                System.exit(0);
            }
        });
    }

    /**
     * Lee caes.properties de la carpeta del jar. Si no existe crea una plantilla y avisa.
     */
    static Boolean CargarConfiguracion(){
        File archivo=new File(CarpetaAplicacion(), ARCHIVOCONFIG);
        if(!archivo.exists()){
            try (Writer w=new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
                w.write("# Configuracion de la carga de ventas Caes a TNS\r\n"
                    +"# API de la estacion\r\n"
                    +"estacion.url=\r\n"
                    +"estacion.usuario=\r\n"
                    +"estacion.clave=\r\n"
                    +"estacion.id=\r\n"
                    +"# API de TNS\r\n"
                    +"tns.url=https://api.tns.co\r\n"
                    +"tns.empresa=\r\n"
                    +"tns.usuario=\r\n"
                    +"tns.clave=\r\n"
                    +"tns.sucursal=00\r\n"
                    +"tns.bodega=00\r\n"
                    +"tns.centroCosto=00\r\n"
                    +"# codigos que TNS exige aunque el articulo no los maneje\r\n"
                    +"tns.talla=00\r\n"
                    +"tns.color=00\r\n"
                    +"tns.banco=00\r\n"
                    +"tns.prefijo=FE\r\n"
                    +"# formato de fecha del reporte ObtenerVentasDetallada (verificacion de duplicados)\r\n"
                    +"tns.formatoFechaReporte=yyyy-MM-dd\r\n");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "No fue posible crear el archivo "+archivo.getAbsolutePath()+"\n"+e.getMessage(), "Ventas Caes", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            JOptionPane.showMessageDialog(null, "Se creo el archivo de configuracion:\n"+archivo.getAbsolutePath()+"\n\nDiligencie los datos de conexion y vuelva a abrir el programa.", "Ventas Caes", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        config=new Properties();
        try (Reader r=new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
            config.load(r);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No fue posible leer "+archivo.getAbsolutePath()+"\n"+e.getMessage(), "Ventas Caes", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        List<String> faltantes=new ArrayList<>();
        for (String llave : new String[]{"estacion.url","estacion.usuario","estacion.clave","estacion.id","tns.url","tns.empresa","tns.usuario","tns.clave"}) {
            if(config.getProperty(llave,"").trim().isEmpty()){
                faltantes.add(llave);
            }
        }
        if(!faltantes.isEmpty()){
            JOptionPane.showMessageDialog(null, "Faltan datos en "+archivo.getAbsolutePath()+":\n"+String.join("\n", faltantes), "Ventas Caes", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        url=QuitarBarraFinal(config.getProperty("estacion.url").trim());
        idEstacion=config.getProperty("estacion.id").trim();
        token=Base64.getEncoder().encodeToString((config.getProperty("estacion.usuario").trim()+":"+config.getProperty("estacion.clave").trim()).getBytes(StandardCharsets.UTF_8));
        URLTNS=QuitarBarraFinal(config.getProperty("tns.url").trim());
        empresaTNS=config.getProperty("tns.empresa").trim();
        usuarioTNS=config.getProperty("tns.usuario").trim();
        passwordTNS=config.getProperty("tns.clave").trim();
        sucursalTNS=config.getProperty("tns.sucursal","00").trim();
        bodega=config.getProperty("tns.bodega","00").trim();
        centroCosto=config.getProperty("tns.centroCosto","00").trim();
        talla=config.getProperty("tns.talla","00").trim();
        color=config.getProperty("tns.color","00").trim();
        bancoTNS=config.getProperty("tns.banco","00").trim();
        prefijo=config.getProperty("tns.prefijo","FE").trim();
        try {
            formatoFechaReporte=DateTimeFormatter.ofPattern(config.getProperty("tns.formatoFechaReporte","yyyy-MM-dd").trim());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, "El formato tns.formatoFechaReporte no es valido: "+e.getMessage(), "Ventas Caes", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    private static String QuitarBarraFinal(String texto){
        while(texto.endsWith("/")){
            texto=texto.substring(0, texto.length()-1);
        }
        return texto;
    }
    /**
     * Carpeta donde esta el jar (o la carpeta de trabajo si se ejecuta desde el IDE).
     */
    static File CarpetaAplicacion(){
        try {
            File ubicacion=new File(consularVentasEDSCaes.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if(ubicacion.isFile()){
                return ubicacion.getParentFile();
            }
        } catch (Exception e) {
        }
        return new File(System.getProperty("user.dir"));
    }

    private static void CrearVentana(){
        ventana=new JFrame("Ventas Caes a TNS");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel=new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints c=new GridBagConstraints();
        c.insets=new Insets(5, 5, 5, 5);
        c.fill=GridBagConstraints.HORIZONTAL;
        fechaInicial=CrearCampoFecha();
        fechaFinal=CrearCampoFecha();
        c.gridx=0; c.gridy=0; panel.add(new JLabel("Fecha inicial:"), c);
        c.gridx=1; panel.add(fechaInicial, c);
        c.gridx=0; c.gridy=1; panel.add(new JLabel("Fecha final:"), c);
        c.gridx=1; panel.add(fechaFinal, c);
        botonProcesar=new JButton("Procesar");
        botonProcesar.addActionListener(e -> Procesar());
        c.gridx=0; c.gridy=2; c.gridwidth=2; panel.add(botonProcesar, c);
        barra=new JProgressBar();
        barra.setStringPainted(true);
        barra.setString("");
        c.gridy=3; panel.add(barra, c);
        estado=new JLabel("Seleccione el rango de fechas y presione Procesar.");
        c.gridy=4; panel.add(estado, c);
        ventana.getContentPane().add(panel, BorderLayout.CENTER);
        ventana.pack();
        ventana.setSize(Math.max(ventana.getWidth(), 460), ventana.getHeight());
        ventana.setResizable(false);
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
    }
    private static JSpinner CrearCampoFecha(){
        JSpinner campo=new JSpinner(new SpinnerDateModel());
        campo.setEditor(new JSpinner.DateEditor(campo, "yyyy-MM-dd"));
        return campo;
    }
    private static LocalDate FechaDe(JSpinner campo){
        return ((Date) campo.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    private static void Estado(String texto){
        SwingUtilities.invokeLater(() -> estado.setText(texto));
    }

    /**
     * Paso 1: consulta las ventas del rango en segundo plano y pide confirmacion.
     */
    private static void Procesar(){
        LocalDate inicio=FechaDe(fechaInicial);
        LocalDate fin=FechaDe(fechaFinal);
        if(inicio.isAfter(fin)){
            JOptionPane.showMessageDialog(ventana, "La fecha inicial no puede ser mayor que la fecha final.", "Ventas Caes", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            AbrirLog();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(ventana, "No fue posible crear el archivo de log.\n"+e.getMessage(), "Ventas Caes", JOptionPane.ERROR_MESSAGE);
            return;
        }
        GuardarLog("INICIO DE LA CONSULTA DE VENTAS DEL "+inicio.format(FORMATOFECHA)+" AL "+fin.format(FORMATOFECHA)+" ESTACION "+idEstacion);
        HabilitarControles(false);
        barra.setIndeterminate(true);
        barra.setString("Consultando ventas...");
        new SwingWorker<List<JSONObject>, Void>() {
            int diasConError=0;
            int yaSubidas=0;
            int yaEnTNS=0;
            boolean sinSesionTNS=false;
            boolean sinVerificacionTNS=false;
            @Override
            protected List<JSONObject> doInBackground() throws Exception {
                CargarRecibosSubidos();
                List<JSONObject> ventas=new ArrayList<>();
                long totalDias=java.time.temporal.ChronoUnit.DAYS.between(inicio, fin)+1;
                int dia=0;
                for (LocalDate fecha=inicio; !fecha.isAfter(fin); fecha=fecha.plusDays(1)) {
                    dia++;
                    Estado("Consultando dia "+dia+" de "+totalDias+" ("+fecha.format(FORMATOFECHA)+")...");
                    JSONArray ventasDia=ConsultarVentasDia(fecha.format(FORMATOFECHA));
                    if(ventasDia==null){
                        diasConError++;
                        continue;
                    }
                    for (int i = 0; i < ventasDia.length(); i++) {
                        JSONObject venta=ventasDia.getJSONObject(i);
                        if(recibosSubidos.contains(LlaveRecibo(venta))){
                            yaSubidas++;
                            GuardarLog("EL RECIBO "+venta.optString("Recibo").trim()+" YA FUE SUBIDO ANTERIORMENTE, SE OMITE");
                            continue;
                        }
                        ventas.add(venta);
                    }
                    GuardarLog("DIA "+fecha.format(FORMATOFECHA)+": "+ventasDia.length()+" VENTAS");
                }
                if(ventas.isEmpty()){
                    return ventas;
                }
                // ventas que ya existen en TNS (subidas por este u otro software): el recibo esta en la observacion
                Estado("Iniciando sesion en TNS...");
                if(!LoginTNS()){
                    sinSesionTNS=true;
                    return ventas;
                }
                Estado("Verificando en TNS las ventas ya existentes...");
                Map<String,String> recibosEnTNS=ConsultarRecibosEnTNS(inicio, fin);
                if(recibosEnTNS==null){
                    sinVerificacionTNS=true;
                    return ventas;
                }
                List<JSONObject> porSubir=new ArrayList<>();
                for (JSONObject venta : ventas) {
                    String factura=recibosEnTNS.get(NormalizarNumero(venta.optString("Recibo")));
                    if(factura!=null){
                        yaEnTNS++;
                        GuardarLog("EL RECIBO "+venta.optString("Recibo").trim()+" YA EXISTE EN TNS EN LA FACTURA "+factura+", NO SE SUBE");
                        continue;
                    }
                    porSubir.add(venta);
                }
                return porSubir;
            }
            @Override
            protected void done() {
                barra.setIndeterminate(false);
                barra.setString("");
                List<JSONObject> ventas;
                try {
                    ventas=get();
                } catch (Exception e) {
                    GuardarLog("ERROR CONSULTANDO LAS VENTAS: "+CausaDe(e));
                    Finalizar("Ocurrio un error consultando las ventas.", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(sinSesionTNS){
                    Finalizar("No fue posible iniciar sesion en TNS. Revise los datos tns.* de "+ARCHIVOCONFIG+".", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                GuardarLog("TOTAL VENTAS POR SUBIR: "+ventas.size()+" (YA SUBIDAS ANTES: "+yaSubidas+", YA EXISTEN EN TNS: "+yaEnTNS+")");
                String aviso="";
                if(yaSubidas>0){
                    aviso+="\n"+yaSubidas+" venta(s) ya fueron subidas anteriormente y se omiten.";
                }
                if(yaEnTNS>0){
                    aviso+="\n"+yaEnTNS+" venta(s) ya existen en TNS (recibo en la observacion) y se omiten.";
                }
                if(sinVerificacionTNS){
                    aviso+="\n\nAtencion: no fue posible verificar en TNS si las ventas ya existen (ver log).\nSi continua podrian quedar ventas duplicadas.";
                }
                if(diasConError>0){
                    aviso+="\n\nAtencion: "+diasConError+" dia(s) no se pudieron consultar (ver log).";
                }
                if(ventas.isEmpty()){
                    Finalizar("No hay ventas por subir entre "+inicio.format(FORMATOFECHA)+" y "+fin.format(FORMATOFECHA)+"."+aviso, JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                int opcion=JOptionPane.showConfirmDialog(ventana,
                    "Se encontraron "+ventas.size()+" ventas por subir entre "+inicio.format(FORMATOFECHA)+" y "+fin.format(FORMATOFECHA)+"."+aviso+"\n\nDesea subirlas a TNS?",
                    "Confirmar carga", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if(opcion!=JOptionPane.YES_OPTION){
                    GuardarLog("CARGA CANCELADA POR EL USUARIO");
                    Finalizar("Carga cancelada. No se subio ninguna venta.", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                SubirVentas(ventas);
            }
        }.execute();
    }

    /**
     * Paso 2: sube las ventas confirmadas a TNS mostrando el avance.
     */
    private static void SubirVentas(List<JSONObject> ventas){
        barra.setMinimum(0);
        barra.setMaximum(ventas.size());
        barra.setValue(0);
        new SwingWorker<int[], Integer>() {
            @Override
            protected int[] doInBackground() throws Exception {
                int subidas=0;
                int conError=0;
                Estado("Iniciando sesion en TNS...");
                if(!LoginTNS()){
                    return null;
                }
                for (int i = 0; i < ventas.size(); i++) {
                    Estado("Subiendo venta "+(i+1)+" de "+ventas.size()+"...");
                    try {
                        if(SubirVenta(ventas.get(i))){
                            subidas++;
                        }else{
                            conError++;
                        }
                    } catch (Exception e) {
                        conError++;
                        GuardarLog("ERROR PROCESANDO EL RECIBO "+ventas.get(i).optString("Recibo")+": "+CausaDe(e));
                    }
                    publish(i+1);
                }
                return new int[]{subidas, conError};
            }
            @Override
            protected void process(List<Integer> avance) {
                int valor=avance.get(avance.size()-1);
                barra.setValue(valor);
                barra.setString(valor+" / "+ventas.size());
            }
            @Override
            protected void done() {
                int[] resultado;
                try {
                    resultado=get();
                } catch (Exception e) {
                    GuardarLog("ERROR SUBIENDO LAS VENTAS: "+CausaDe(e));
                    Finalizar("Ocurrio un error subiendo las ventas.", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(resultado==null){
                    Finalizar("No fue posible iniciar sesion en TNS. Revise los datos tns.* de "+ARCHIVOCONFIG+".", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                GuardarLog("FIN DE LA CARGA. SUBIDAS: "+resultado[0]+" CON ERROR: "+resultado[1]);
                Finalizar("Carga terminada.\n\nVentas leidas: "+ventas.size()+"\nSubidas a TNS: "+resultado[0]+"\nCon error: "+resultado[1],
                    resultado[1]>0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    private static void HabilitarControles(boolean habilitar){
        botonProcesar.setEnabled(habilitar);
        fechaInicial.setEnabled(habilitar);
        fechaFinal.setEnabled(habilitar);
    }
    /**
     * Cierra el log, muestra el resumen y abre el archivo de log.
     */
    private static void Finalizar(String mensaje,int tipo){
        File archivo=archivoLog;
        CerrarLog();
        estado.setText("Proceso terminado. Log: "+(archivo!=null ? archivo.getName() : ""));
        HabilitarControles(true);
        JOptionPane.showMessageDialog(ventana, mensaje, "Ventas Caes", tipo);
        AbrirArchivo(ventana, archivo);
    }
    /**
     * Abre el archivo (log) con el programa predeterminado o con el Bloc de notas.
     */
    static void AbrirArchivo(java.awt.Component padre,File archivo){
        if(archivo==null){
            return;
        }
        try {
            Desktop.getDesktop().open(archivo);
        } catch (Exception e) {
            if(AbrirConBlocDeNotas(archivo)){
                return;
            }
            JOptionPane.showMessageDialog(padre, "No fue posible abrir el log:\n"+archivo.getAbsolutePath(), "Ventas Caes", JOptionPane.WARNING_MESSAGE);
        }
    }
    private static Boolean AbrirConBlocDeNotas(File archivo){
        if(!System.getProperty("os.name","").toLowerCase().contains("windows")){
            return false;
        }
        try {
            new ProcessBuilder("notepad.exe", archivo.getAbsolutePath()).start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    static String CausaDe(Exception e){
        Throwable causa=e.getCause()!=null ? e.getCause() : e;
        return causa.getClass().getSimpleName()+" "+causa.getMessage();
    }

    /**
     * Registro local de recibos ya subidos a TNS (estacion;recibo;fecha;factura;fecha de carga).
     */
    /**
     * Consulta en TNS las facturas del rango (reporte ObtenerVentasDetallada) y devuelve los numeros
     * que aparecen en su observacion, con la factura donde aparecen. Devuelve null si no se pudo consultar.
     */
    public static Map<String,String> ConsultarRecibosEnTNS(LocalDate inicio,LocalDate fin) throws IOException{
        // se amplia un dia a cada lado porque la fecha de la factura puede diferir de la del recibo
        HttpUrl urlReporte=HttpUrl.parse(URLTNS+"/v2/facturacion/Reportes/ObtenerVentasDetallada").newBuilder()
            .addQueryParameter("fechaInicial", inicio.minusDays(1).format(formatoFechaReporte))
            .addQueryParameter("fechaFin", fin.plusDays(1).format(formatoFechaReporte))
            .addQueryParameter("codigosucursal", sucursalTNS).build();
        Response response=EjecutarTNS(urlReporte, null);
        String respuesta=response.body().string();
        JSONArray filas=null;
        try {
            Object valor=new JSONTokener(respuesta.trim()).nextValue();
            if(valor instanceof String){
                valor=new JSONTokener(((String) valor).trim()).nextValue();
            }
            if(valor instanceof JSONArray){
                filas=(JSONArray) valor;
            }else if(valor instanceof JSONObject){
                filas=((JSONObject) valor).optJSONArray("data");
            }
        } catch (JSONException e) {
            filas=null;
        }
        if(response.code()!=200 || filas==null){
            GuardarLog("ERROR "+response.code()+" CONSULTANDO EN TNS LAS VENTAS EXISTENTES: "+Recortar(respuesta));
            return null;
        }
        Map<String,String> recibos=new HashMap<>();
        for (int i = 0; i < filas.length(); i++) {
            JSONObject fila=filas.optJSONObject(i);
            if(fila==null){
                continue;
            }
            String factura=fila.optString("codprefijo").trim()+fila.optString("numero").trim();
            for (String numero : fila.optString("observ").split("[^0-9]+")) {
                if(!numero.isEmpty()){
                    recibos.putIfAbsent(NormalizarNumero(numero), factura);
                }
            }
        }
        GuardarLog("FACTURAS CONSULTADAS EN TNS PARA VERIFICAR DUPLICADOS: "+filas.length()+" LINEAS");
        return recibos;
    }
    /**
     * Quita espacios y ceros a la izquierda para comparar numeros de recibo.
     */
    private static String NormalizarNumero(String numero){
        String limpio=numero.trim().replaceFirst("^0+(?=.)", "");
        return limpio;
    }
    private static File ArchivoRecibos(){
        return new File(CarpetaAplicacion(), ARCHIVORECIBOS);
    }
    private static String LlaveRecibo(JSONObject venta){
        return idEstacion+";"+venta.optString("Recibo").trim();
    }
    private static synchronized void CargarRecibosSubidos() throws IOException{
        recibosSubidos.clear();
        File archivo=ArchivoRecibos();
        if(!archivo.exists()){
            return;
        }
        try (BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea=br.readLine())!=null) {
                String[] partes=linea.split(";");
                if(partes.length>=2){
                    recibosSubidos.add(partes[0].trim()+";"+partes[1].trim());
                }
            }
        }
    }
    private static synchronized void RegistrarReciboSubido(JSONObject venta,String fecha,String factura){
        String llave=LlaveRecibo(venta);
        recibosSubidos.add(llave);
        try (Writer w=new OutputStreamWriter(new FileOutputStream(ArchivoRecibos(), true), StandardCharsets.UTF_8)) {
            w.write(llave+";"+fecha+";"+factura+";"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))+"\r\n");
        } catch (IOException e) {
            GuardarLog("NO FUE POSIBLE REGISTRAR EL RECIBO "+venta.optString("Recibo")+" EN "+ARCHIVORECIBOS+": "+e.getMessage());
        }
    }

    private static void AbrirLog() throws IOException{
        AbrirLog("caes");
    }
    /**
     * Crea logs/<nombre>_<fecha>.log en la carpeta del jar y devuelve el archivo.
     */
    static File AbrirLog(String nombre) throws IOException{
        File carpeta=new File(CarpetaAplicacion(), "logs");
        carpeta.mkdirs();
        archivoLog=new File(carpeta, nombre+"_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))+".log");
        log=new PrintWriter(new OutputStreamWriter(new FileOutputStream(archivoLog), StandardCharsets.UTF_8), true);
        tercerosConsultados.clear();
        return archivoLog;
    }
    static synchronized File CerrarLogYDevolverArchivo(){
        File archivo=archivoLog;
        CerrarLog();
        return archivo;
    }
    static synchronized void CerrarLog(){
        if(log!=null){
            log.close();
            log=null;
        }
        archivoLog=null;
    }
    public static synchronized void GuardarLog(String observacionString){
        String linea=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))+"  "+observacionString;
        System.out.println(linea);
        if(log!=null){
            log.println(linea);
        }
    }

    /**
     * Consulta las ventas de un dia en el API de la estacion. Devuelve null si hubo error.
     */
    public static JSONArray ConsultarVentasDia(String fechaString) throws IOException{
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        OkHttpClient client=builder.build();
        Request request = new Request.Builder().url(url+"/api/Estaciones/"+idEstacion+"/ventas/date/"+fechaString).get()    
        .addHeader("Authorization", "Basic "+token)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            String respuesta=response.body().string();
            if(response.code()!=200){
                GuardarLog("ERROR "+response.code()+" CONSULTANDO LAS VENTAS DEL "+fechaString+": "+Recortar(respuesta));
                return null;
            }
            JSONArray resultado=new JSONObject(respuesta).optJSONArray("Resultado");
            return resultado==null ? new JSONArray() : resultado;
        } catch (IOException | JSONException e) {
            GuardarLog("ERROR CONSULTANDO LAS VENTAS DEL "+fechaString+": "+e.getMessage());
            return null;
        }
    }
    /**
     * Arma el mensaje de error de una respuesta de TNS: data.response, message, o el detalle
     * de validacion (errors/title) que TNS devuelve con el error 400.
     */
    static String MensajeErrorTNS(JSONObject rta,String respuesta){
        JSONObject datos=rta.optJSONObject("data");
        if(datos!=null && !datos.optString("response").trim().isEmpty()){
            return datos.optString("response").trim();
        }
        StringBuilder mensaje=new StringBuilder(rta.optString("message").trim());
        JSONObject errores=rta.optJSONObject("errors");
        if(errores!=null){
            for (String campo : errores.keySet()) {
                Object detalle=errores.opt(campo);
                if(detalle instanceof JSONArray){
                    JSONArray lista=(JSONArray) detalle;
                    List<String> textos=new ArrayList<>();
                    for (int i = 0; i < lista.length(); i++) {
                        textos.add(lista.optString(i));
                    }
                    detalle=String.join(" / ", textos);
                }
                mensaje.append(" [").append(campo).append(": ").append(detalle).append("]");
            }
        }
        if(mensaje.length()==0){
            mensaje.append(rta.optString("title")).append(" ").append(rta.optString("detail"));
        }
        if(mensaje.toString().trim().isEmpty()){
            return Recortar(respuesta);
        }
        return mensaje.toString().trim();
    }
    static String Recortar(String texto){
        return texto.length()>200 ? texto.substring(0, 200) : texto;
    }

    /**
     * Sube una venta a TNS con la logica de consularVentasSauceMonterrey. Devuelve true si TNS la acepto.
     */
    public static Boolean SubirVenta(JSONObject sale) throws IOException{
        String teridString=ConsultarTerid(sale,"cliente");  
        String vendedorIdString=ConsultarTerid(sale, "vendedor");
        String observaciones=sale.getString("Recibo")+" "+sale.getString("Placa");
        String formapago="CO";
        String plazoDias="0";
        String numero="";
        String recibo=sale.getString("Recibo").trim();
        // forma de pago con la misma logica de Monterrey
        if(sale.get("FacturacionElectronica").equals(null)){
            if(!ObtenerCampo(sale, "Kilometraje").equals("6")){
                formapago="CR";
            }
            if(!sale.get("Pagos").equals(null)){
                for (int j = 0; j < sale.getJSONArray("Pagos").length(); j++) {
                    if(sale.getJSONArray("Pagos").getJSONObject(j).getString("FormaPago").contains("Credito")){
                        formapago="CR";
                    }                       
                }
            }
            // el numero lo asigna TNS con el consecutivo del prefijo
            numero="";
        }else {
            numero=sale.getJSONArray("FacturacionElectronica").getJSONObject(0).getString("Numero");
        }
        if(formapago.equals("CR")){
            plazoDias="15";
        }
        String banco=ObtenerCampo(sale, "Kilometraje").trim();
        if(banco.isEmpty()){
            banco=bancoTNS;
        }
        String fechaVentaString=sale.getString("HoraFin").split("T")[0];
        fechaVentaString=fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(0, 4);
        String matidString=BuscarMaterialAPI(sale.getJSONObject("Producto").getString("Nombre"));
        if(matidString.equals("00")){
            GuardarLog("EL ARTICULO "+sale.getJSONObject("Producto").getString("Nombre")+" NO EXISTE, EN EL RECIBO "+recibo);
            return false;
        }
        Float Cantidad=sale.getFloat("Valor")/sale.getFloat("Precio");
        Float precioBaseFloat=sale.getFloat("Precio");
        JsonObject venta = new JsonObject();
        JsonArray itemsPedido = new JsonArray();
        JsonObject itempedido = new JsonObject();
        JsonArray itemsFormaPago = new JsonArray();
        JsonObject itemformapago = new JsonObject();
        venta.addProperty("codigoPrefijo", prefijo); 
        venta.addProperty("numero", numero);
        venta.addProperty("fecha", fechaVentaString);  
        venta.addProperty("codTercero", teridString);    
        venta.addProperty("codVendedor", vendedorIdString);  
        venta.addProperty("codDespachar", teridString);    
        venta.addProperty("codFormaPago", formapago);    
        venta.addProperty("codBanco", banco);    
        venta.addProperty("fechaVence", fechaVentaString);
        venta.addProperty("plazoDias", Integer.parseInt(plazoDias));
        venta.addProperty("observacion", observaciones);
        venta.addProperty("codigoCentroCosto", centroCosto);
        itempedido.addProperty("codMat",matidString);
        itempedido.addProperty("codBodega", bodega);
        itempedido.addProperty("codTalla", talla);
        itempedido.addProperty("codColor", color);
        itempedido.addProperty("cantidad", Cantidad);
        itempedido.addProperty("tipoUnidad", "D");
        itempedido.addProperty("descuento", 0);
        itempedido.addProperty("centrosCostos", centroCosto);
        itempedido.addProperty("porcIva", 0);
        itempedido.addProperty("valor", precioBaseFloat);
        itempedido.addProperty("impConsumo", 0);
        itempedido.addProperty("observacion", "");
        itemsPedido.add(itempedido);
        venta.add("detallePedido", itemsPedido);
        itemformapago.addProperty("codigoFormaPago", formapago);
        itemformapago.addProperty("plazoDias", plazoDias);
        itemformapago.addProperty("fechaVencimiento", fechaVentaString);
        itemformapago.addProperty("centroCosto", centroCosto);
        itemformapago.addProperty("valor", sale.get("Valor").toString());
        itemsFormaPago.add(itemformapago);
        venta.add("detalleFormaPago", itemsFormaPago);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(venta);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        HttpUrl urlVenta=HttpUrl.parse(URLTNS+"/v2/facturacion/Ventas/Crear").newBuilder()
            .addQueryParameter("codigosucursal", sucursalTNS).build();
        Response response2 = EjecutarTNS(urlVenta, body);
        String respuesta2=response2.body().string();
        JSONObject objRespues=LeerRespuestaTNS(respuesta2);
        JSONObject datos=objRespues.optJSONObject("data");
        if (response2.code()!=200 || !objRespues.optBoolean("status") || (datos!=null && !datos.optBoolean("success"))){
            GuardarLog("ERROR "+response2.code()+" "+MensajeErrorTNS(objRespues, respuesta2)+" en el recibo "+recibo);
            return false;
        }
        String factura=numero;
        if(datos!=null && !datos.optString("consecutivo").trim().isEmpty()){
            factura=datos.optString("consecutivo").trim();
        }
        RegistrarReciboSubido(sale, fechaVentaString, prefijo+factura);
        GuardarLog("RECIBO "+recibo+" SUBIDO A TNS COMO FACTURA "+prefijo+factura);
        return true;
    }

    public static String ObtenerCampo(JSONObject jsonObject,String campo){
        String respuestaString="";
        if (jsonObject.has(campo) && !jsonObject.get(campo).equals(null)){
            respuestaString=jsonObject.get(campo).toString();
        }
        return respuestaString;
    }

    public static String BuscarMaterialAPI(String filtro) throws IOException{
        String codigoString = "00";
        String nombre="";
        float existencia=0;
        String[] palabras=filtro.trim().split(" ");
        nombre=palabras.length>1 ? palabras[0]+" "+palabras[1] : palabras[0];
        JSONObject rta = new JSONObject();
        JSONArray Articulos = new JSONArray();
        JSONObject Articulo = new JSONObject();
        HttpUrl urlMaterial=HttpUrl.parse(URLTNS+"/v2/tablas/Material/Listar").newBuilder()
            .addQueryParameter("codigosucursal", sucursalTNS)
            .addQueryParameter("filtro", nombre).build();
        Response response = EjecutarTNS(urlMaterial, null);
        rta=LeerRespuestaTNS(response.body().string());
        try {
            Articulos=rta.getJSONArray("data");
            for (int i = 0; i < Articulos.length(); i++) {
                Articulo=Articulos.getJSONObject(i);
                existencia=Float.parseFloat(Articulo.optString("existencias","0"));
                if(existencia>1){
                    codigoString=Articulo.getString("codigo");    
                }                
            }
        } catch (Exception e) {
            codigoString="00";
            GuardarLog("el articulo no se encontro "+filtro);
        }
        return codigoString;
    }

    /**
     * Busca el tercero en TNS (primero por documento y luego por nombre) con la misma logica
     * de consularVentasSauceLosAngeles. Si no existe lo crea en TNS por API:
     * el cliente con los datos de caes.eds.com.co (por la placa) y el vendedor con los datos del empleado.
     */
    public static String ConsultarTerid(JSONObject venta,String tipo) throws IOException{
        String teridString="";
        String documento="";
        String nombre="";
        String placa=ObtenerCampo(venta, "Placa").trim().toUpperCase();
        if(!placa.isEmpty()){
            placa=placa.split(" ")[0];
        }
        if(tipo.equals("cliente")){
            if(venta.get("Cliente").equals(null)){
                if(placa.isEmpty()){
                    GuardarLog("EL RECIBO "+venta.getString("Recibo")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL");
                    return CONSUMIDORFINAL;
                }
                documento=placa;
            }else{
                documento=ObtenerCampo(venta.getJSONObject("Cliente"), "NumeroDocumento").trim();
                nombre=ObtenerCampo(venta.getJSONObject("Cliente"), "Nombre").toUpperCase().trim();
            }
        }else{
            if(venta.get("Empleado").equals(null)){
                return "0";
            }
            documento=ObtenerCampo(venta.getJSONObject("Empleado"), "Cedula").trim();
            nombre=ObtenerCampo(venta.getJSONObject("Empleado"), "Nombre").toUpperCase().trim();
        }
        String llave=tipo+"|"+documento+"|"+nombre+"|"+placa;
        if(tercerosConsultados.containsKey(llave)){
            teridString=tercerosConsultados.get(llave);
            if(tipo.equals("cliente") && teridString.equals(CONSUMIDORFINAL)){
                GuardarLog("EL RECIBO "+venta.getString("Recibo")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL");
            }
            return teridString;
        }
        teridString=BuscarTerceroTNS(documento, nombre);
        if(teridString.isEmpty()){
            if(tipo.equals("cliente")){
                if(!placa.isEmpty()){
                    teridString=CrearClienteCaes(placa);
                }
                if(teridString.isEmpty()){
                    GuardarLog("EL RECIBO "+venta.getString("Recibo")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL");
                    teridString=CONSUMIDORFINAL;
                }
            }else{
                teridString=CrearVendedorTNS(venta.getJSONObject("Empleado"));
                if(teridString.isEmpty()){
                    teridString="0";
                }
            }
        }
        tercerosConsultados.put(llave, teridString);
        return teridString;
    }
    /**
     * Devuelve el codigo del tercero en TNS buscando primero por documento (nit/codigo) y luego por nombre.
     */
    public static String BuscarTerceroTNS(String documento,String nombre) throws IOException{
        JSONArray terceros;
        JSONObject tercero;
        if(!documento.isEmpty()){
            terceros=ListarTercerosTNS(documento);
            for (int i = 0; i < terceros.length(); i++) {
                tercero=terceros.getJSONObject(i);
                String nit=tercero.optString("nit").trim();
                if (nit.equals(documento) || nit.startsWith(documento+"-") || tercero.optString("codigo").trim().equals(documento)){
                    return tercero.optString("codigo");
                }
            }
        }
        if(!nombre.isEmpty()){
            terceros=ListarTercerosTNS(nombre);
            for (int i = 0; i < terceros.length(); i++) {
                tercero=terceros.getJSONObject(i);
                if (tercero.optString("nombre").toUpperCase().trim().equals(nombre)){
                    return tercero.optString("codigo");
                }
            }
        }
        return "";
    }
    public static JSONArray ListarTercerosTNS(String filtro) throws IOException{
        HttpUrl urlTercero=HttpUrl.parse(URLTNS+"/v2/tablas/Tercero/Listar").newBuilder()
            .addQueryParameter("filtro", filtro).build();
        Response response = EjecutarTNS(urlTercero, null);
        JSONObject rta=LeerRespuestaTNS(response.body().string());
        JSONArray terceros=rta.optJSONArray("data");
        if(terceros==null){
            terceros=new JSONArray();
        }
        return terceros;
    }
    /**
     * Consulta el cliente en caes.eds.com.co por la placa (identificacion) y lo crea en TNS.
     */
    public static String CrearClienteCaes(String placa) throws IOException{
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
        .url("https://caes.eds.com.co/api/thirds/?identification="+placa+"&type=CC")
        .get()
        .addHeader("Accept", "*/*")
        .addHeader("User-Agent", "Thunder Client (https://www.thunderclient.com)")
        .build();
        try {
            Response response = client.newCall(request).execute();
            JSONObject tercero =new JSONObject(response.body().string());
            if(tercero.optInt("count")>0){
                JSONObject datos=tercero.getJSONArray("results").getJSONObject(0);
                String documento=datos.optString("identification").trim();
                if(documento.isEmpty()){
                    documento=placa;
                }
                String nombre=(datos.optString("last_name")+" "+datos.optString("second_last_name")+" "+datos.optString("name")+" "+datos.optString("second_name")).trim().replaceAll(" +", " ").toUpperCase();
                JsonObject nuevo=TerceroBase(documento, nombre, datos.optString("address"), "", datos.optString("email"));
                nuevo.addProperty("nombre1", datos.optString("name"));
                nuevo.addProperty("nombre2", datos.optString("second_name"));
                nuevo.addProperty("apellido1", datos.optString("last_name"));
                nuevo.addProperty("apellido2", datos.optString("second_last_name"));
                nuevo.addProperty("cliente", "S");
                if(CrearTerceroTNS(nuevo)){
                    return documento;
                }
            }
        } catch (IOException | JSONException e) {
            GuardarLog("NO FUE POSIBLE CONSULTAR EL CLIENTE "+placa+" EN CAES");
        }
        return "";
    }
    /**
     * Crea el vendedor en TNS con los datos del empleado de la venta.
     */
    public static String CrearVendedorTNS(JSONObject empleado) throws IOException{
        String documento=ObtenerCampo(empleado, "Cedula").trim();
        if(documento.isEmpty()){
            return "";
        }
        JsonObject nuevo=TerceroBase(documento, ObtenerCampo(empleado, "Nombre").toUpperCase().trim(),
            ObtenerCampo(empleado, "Direccion"), ObtenerCampo(empleado, "Telefono"), "");
        nuevo.addProperty("empleado", "S");
        nuevo.addProperty("vended", "S");
        if(CrearTerceroTNS(nuevo)){
            return documento;
        }
        return "";
    }
    private static JsonObject TerceroBase(String documento,String nombre,String direccion,String telefono,String email){
        JsonObject tercero=new JsonObject();
        tercero.addProperty("codigo", documento);
        tercero.addProperty("natJuridica", "N");
        tercero.addProperty("tipoDocumento", "C");
        tercero.addProperty("nit", documento);
        tercero.addProperty("nombre", nombre);
        tercero.addProperty("nomRegTri", nombre);
        tercero.addProperty("direccion", direccion.trim().isEmpty() ? "SIN DIRECCION" : direccion.trim());
        tercero.addProperty("codigoCiudad", "00");
        tercero.addProperty("nombreCiudad", "SIN CIUDAD");
        tercero.addProperty("zona1", "00");
        tercero.addProperty("clasificacion", "00");
        tercero.addProperty("codigoBarrio", "00");
        tercero.addProperty("inactivo", false);
        tercero.addProperty("privada", "N");
        tercero.addProperty("mixta", "N");
        tercero.addProperty("telefono", telefono.trim().isEmpty() ? "SIN TELEFONO" : telefono.trim());
        tercero.addProperty("email", email.trim().isEmpty() ? "SIN EMAIL" : email.trim());
        tercero.addProperty("comision", 0);
        return tercero;
    }
    public static Boolean CrearTerceroTNS(JsonObject tercero) throws IOException{
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(tercero));
        Response response = EjecutarTNS(HttpUrl.parse(URLTNS+"/v2/tablas/Tercero/Crear"), body);
        JSONObject rta=LeerRespuestaTNS(response.body().string());
        if(response.code()!=200 || !rta.optBoolean("status")){
            GuardarLog("NO FUE POSIBLE CREAR EL TERCERO "+tercero.get("nit").getAsString()+" EN TNS "+response.code()+" "+rta.optString("message"));
            return false;
        }
        return true;
    }
    /**
     * Inicia sesion en el API v2 de TNS y guarda el token (Bearer) en tokenTNS.
     */
    public static Boolean LoginTNS() throws IOException{
        JsonObject login = new JsonObject();
        login.addProperty("codigoEmpresa", empresaTNS);
        login.addProperty("nombreUsuario", usuarioTNS);
        login.addProperty("contrasenia", passwordTNS);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(login));
        Request request = new Request.Builder().url(URLTNS+"/v2/Acceso/Login").post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json").build();
        Response response = ClienteTNS().newCall(request).execute();
        JSONObject rta=LeerRespuestaTNS(response.body().string());
        tokenTNS="";
        if(response.code()==200 && rta.optBoolean("status")){
            tokenTNS=rta.optString("data");
        }
        if(tokenTNS.isEmpty()){
            GuardarLog("NO FUE POSIBLE INICIAR SESION EN TNS "+response.code()+" "+rta.optString("message"));
            return false;
        }
        return true;
    }
    /**
     * Ejecuta una peticion al API de TNS con el token Bearer (GET si body es null, POST si no).
     * Si el token vencio (401) inicia sesion de nuevo y reintenta una vez.
     */
    public static Response EjecutarTNS(HttpUrl urlTNS, RequestBody body) throws IOException{
        Response response=null;
        for (int intento = 0; intento < 2; intento++) {
            Request.Builder requestBuilder = new Request.Builder().url(urlTNS)
                .addHeader("Authorization", "Bearer "+tokenTNS)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json");
            if(body==null){
                requestBuilder.get();
            }else{
                requestBuilder.post(body);
            }
            response = ClienteTNS().newCall(requestBuilder.build()).execute();
            if(response.code()!=401 || intento==1 || !LoginTNSDeNuevo(response)){
                break;
            }
        }
        return response;
    }
    private static Boolean LoginTNSDeNuevo(Response response) throws IOException{
        response.close();
        return LoginTNS();
    }
    /**
     * Convierte la respuesta del API de TNS en JSON, aunque venga serializada como texto.
     */
    public static JSONObject LeerRespuestaTNS(String respuesta){
        JSONObject rta;
        try {
            Object valor=new JSONTokener(respuesta.trim()).nextValue();
            if(valor instanceof String){
                valor=new JSONTokener(((String) valor).trim()).nextValue();
            }
            rta=(JSONObject) valor;
        } catch (Exception e) {
            rta=new JSONObject();
            rta.put("status", false);
            rta.put("message", respuesta.length()>200 ? respuesta.substring(0, 200) : respuesta);
        }
        return rta;
    }
    private static OkHttpClient ClienteTNS(){
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        return builder.build();
    }
    private static OkHttpClient.Builder configureToIgnoreCertificate(OkHttpClient.Builder builder) {
    try {

            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException{
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException{
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
        }
        return builder;
    }
    
}
