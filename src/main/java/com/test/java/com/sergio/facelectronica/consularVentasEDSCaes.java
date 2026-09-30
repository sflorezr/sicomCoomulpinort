package com.test.java.com.sergio.facelectronica;
import java.io.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.sql.ResultSet;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sergio.prueba.ConnectionFirebird;

import okhttp3.*;

/**
 * Consulta las ventas como consularVentasSauceMonterrey (api/Estaciones/{id}/ventas/date/{fecha})
 * y las inserta en TNS por API v2 (api.tns.co/v2/facturacion/Ventas/Crear) con token Bearer
 * obtenido en /v2/Acceso/Login con empresa, usuario y clave guardados en varios.
 */
public class consularVentasEDSCaes {
    private static final String URLTNS="https://api.tns.co";
    private static ConnectionFirebird Tns=null;
    private static String url="";
    private static String usuario="";
    private static String usuarioTNS="";
    private static String password="";    
    private static String passwordTNS="";    
    private static String empresaTNS="";  
    private static String token="";
    private static String tokenTNS="";
    private static JSONObject obj = new JSONObject();
    private static JSONObject sale = new JSONObject();
    private static String json =null; 
    private static String sqlString="";

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException,JSONException{
        File tempo = new File("c:\\tempo\\terpel.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String ruta=data.split("\\|")[0];
        String ip=data.split("\\|")[1];
        String fecha=data.split("\\|")[2];
        String id=data.split("\\|")[3];
        String insertar=data.split("\\|")[4];
        String sucid=data.split("\\|")[5];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario();
        ConsultarPedidos(fecha,id,insertar,sucid);
        Tns.cerrarConexion();
        br.close();
        System.out.println("termine");
    }

    public static void ConsultarPedidos(String fechaString,String idString,String insertaString,String sucid) throws IOException, ClassNotFoundException, JSONException, SQLException{
    String teridString="";
    String vendedorIdString="";
    String fechaVentaString="";
    String matidString="";
    String prefijo="FE";
    String numero="";    
    String observaciones="";
    String formapago="";
    String plazoDias="";
    String banco="";
    String bodega="";
    String centroCosto="";
    Float Cantidad=(float) 0;
    Float precioBaseFloat=(float) 0;
    OkHttpClient client =new OkHttpClient();
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder = configureToIgnoreCertificate(builder);
    builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
    MediaType mediaType = MediaType.parse("application/json");
    client=builder.build();
    Request request = new Request.Builder().url(url+"/api/Estaciones/"+idString+"/ventas/date/"+fechaString+"").get()    
    .addHeader("Authorization", "Basic "+token)
    .addHeader("Content-Type", "application/json")
    .addHeader("Accept", "application/json").build();
    Response response = client.newCall(request).execute();
    System.out.println(response.code());
    String respuesta=response.body().string();
    obj =new JSONObject(respuesta);
        if(insertaString.equals("N")){
            if(response.code()==200){
                System.out.println(obj.getJSONArray("Resultado").length());
                sqlString="delete from varios where variab='CANTIDADTERPEL'";
                Tns.actualizar(sqlString);
                sqlString="insert into varios (contenido,variab) values('"+obj.getJSONArray("Resultado").length()+"','CANTIDADTERPEL')";  
                Tns.actualizar(sqlString);
            }
        }else{
            sqlString="delete from varios where variab='CANTIDADTERPEL'";
            Tns.actualizar(sqlString);
            sqlString="insert into varios (contenido,variab) values('"+obj.getJSONArray("Resultado").length()+"','CANTIDADTERPEL')";  
            Tns.actualizar(sqlString);
            sqlString="delete from varios where variab='CANTIDADTERPELSUBIDA'";
            Tns.actualizar(sqlString);
            sqlString="delete from logterpel";
            Tns.actualizar(sqlString);
            sqlString="insert into varios (contenido,variab) values('0','CANTIDADTERPELSUBIDA')";  
            Tns.actualizar(sqlString);    
            if(!LoginTNS()){
                return;
            }
            // bodega y centro de costos segun la sucursal, igual que en Monterrey
            if(sucid.equals("1")){
                bodega=ConsultarCodigoBodega("1");
                centroCosto=ConsultarCodigoCentro("1");
            }else{
                bodega=ConsultarCodigoBodega("2");
                centroCosto=ConsultarCodigoCentro("3");
            }
            for (int i = 0; i < obj.getJSONArray("Resultado").length(); i++) {
                teridString=""; 
                fechaVentaString="";
                vendedorIdString="";
                numero="";
                observaciones="";
                formapago="CO";
                plazoDias="0";
                banco="";
                sale=obj.getJSONArray("Resultado").getJSONObject(i);  
                teridString=ConsultarTerid(sale,"cliente");  
                vendedorIdString=ConsultarTerid(sale, "vendedor");
                observaciones=sale.getString("Recibo")+" "+sale.getString("Placa");
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
                    numero=sale.getString("Recibo").trim();
                }else {
                    numero=sale.getJSONArray("FacturacionElectronica").getJSONObject(0).getString("Numero");
                }
                if(formapago.equals("CR")){
                    plazoDias="15";
                }
                banco=ConsultarCodigoBanco(ObtenerCampo(sale, "Kilometraje"));
                fechaVentaString=sale.getString("HoraFin").split("T")[0];
                fechaVentaString=fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(0, 4);
                matidString=BuscarMaterialAPI(sale.getJSONObject("Producto").getString("Nombre"));
                if(!matidString.equals("00")){
                    Cantidad=sale.getFloat("Valor")/sale.getFloat("Precio");
                    precioBaseFloat=sale.getFloat("Precio");
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
                    itempedido.addProperty("codTalla", "");
                    itempedido.addProperty("codColor", "");
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
                    RequestBody body = RequestBody.create(mediaType, json);
                    HttpUrl urlVenta=HttpUrl.parse(URLTNS+"/v2/facturacion/Ventas/Crear").newBuilder()
                        .addQueryParameter("codigosucursal", "00").build();
                    Response response2 = EjecutarTNS(urlVenta, body);
                    System.out.println(response2.code());
                    JSONObject objRespues=LeerRespuestaTNS(response2.body().string());
                    JSONObject datos=objRespues.optJSONObject("data");
                    if (response2.code()!=200 || !objRespues.optBoolean("status") || (datos!=null && !datos.optBoolean("success"))){
                        String mensaje=objRespues.optString("message");
                        if(datos!=null && !datos.optString("response").isEmpty()){
                            mensaje=datos.optString("response");
                        }
                        GuardarLog("ERROR "+response2.code()+" "+mensaje+" en la factura "+prefijo+numero);
                    }
                }else{
                    GuardarLog("EL ARTICULO "+sale.getJSONObject("Producto").getString("Nombre")+" NO EXISTE, EN LA FACTURA "+prefijo+numero);
                }
                Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
                System.out.println(String.valueOf(i+1) +" de "+obj.getJSONArray("Resultado").length()); 
            }
        }
    }

    public static String ConsultarCodigoBanco(String codigo) throws ClassNotFoundException, SQLException{
        String codigoString="00";    
        sqlString="select codigo from banco where codigo='"+codigo+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            codigoString=rs.getString("codigo");
        }
        return codigoString;
    }
    public static String ConsultarCodigoBodega(String bodid) throws ClassNotFoundException, SQLException{
        String codigoString="00";    
        sqlString="select codigo from bodega where bodid='"+bodid+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            codigoString=rs.getString("codigo");
        }
        return codigoString;
    }
    public static String ConsultarCodigoCentro(String cenid) throws ClassNotFoundException, SQLException{
        String codigoString="00";    
        sqlString="select nro from centros where cenid='"+cenid+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            codigoString=rs.getString("nro");
        }
        return codigoString;
    }

    public static String ObtenerCampo(JSONObject jsonObject,String campo){
        String respuestaString="";
        if (jsonObject.has(campo) && !jsonObject.get(campo).equals(null)){
            respuestaString=jsonObject.get(campo).toString();
        }
        return respuestaString;
    }

    public static String BuscarMaterialAPI(String filtro) throws ClassNotFoundException, SQLException, IOException{
        String codigoString = "00";
        String nombre="";
        float existencia=0;
        String[] palabras=filtro.trim().split(" ");
        nombre=palabras.length>1 ? palabras[0]+" "+palabras[1] : palabras[0];
        JSONObject rta = new JSONObject();
        JSONArray Articulos = new JSONArray();
        JSONObject Articulo = new JSONObject();
        HttpUrl urlMaterial=HttpUrl.parse(URLTNS+"/v2/tablas/Material/Listar").newBuilder()
            .addQueryParameter("codigosucursal", "00")
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

    public static String ConsultarTerid(JSONObject venta,String tipo) throws ClassNotFoundException, SQLException, IOException{
        String teridString="";
        String nombre="";
        JSONObject rta = new JSONObject();
        JSONArray terceros = new JSONArray();
        JSONObject tercero = new JSONObject();
        if(tipo.equals("cliente")){
            teridString="222222222222";
            if(venta.get("Cliente").equals(null)){
                GuardarLog("LA FACTURA "+venta.getString("Recibo")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL ");
                return teridString;
            }
            nombre=venta.getJSONObject("Cliente").getString("Nombre").toUpperCase().trim();
        }else{
            teridString="0";
            if(venta.get("Empleado").equals(null)){
                return teridString;
            }
            nombre=venta.getJSONObject("Empleado").getString("Nombre").toUpperCase().trim();
        }
        HttpUrl urlTercero=HttpUrl.parse(URLTNS+"/v2/tablas/Tercero/Listar").newBuilder()
            .addQueryParameter("filtro", nombre).build();
        Response response = EjecutarTNS(urlTercero, null);
        rta=LeerRespuestaTNS(response.body().string());
        try {                
            terceros=rta.getJSONArray("data");
            for (int i = 0; i < terceros.length(); i++) {
                tercero=terceros.getJSONObject(i);
                if (tercero.optString("nombre").toUpperCase().trim().equals(nombre)){
                    teridString=tercero.getString("codigo");                
                }
            }
        }
        catch (Exception e) {
            // se deja el tercero por defecto
        }    
        if(tipo.equals("cliente") && teridString.equals("222222222222")){
            GuardarLog("LA FACTURA "+venta.getString("Recibo")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL ");
        }
        return teridString;
    }
    /**
     * Inicia sesion en el API v2 de TNS y guarda el token (Bearer) en tokenTNS.
     */
    public static Boolean LoginTNS() throws IOException, SQLException{
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
    public static Response EjecutarTNS(HttpUrl urlTNS, RequestBody body) throws IOException, SQLException{
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
    private static Boolean LoginTNSDeNuevo(Response response) throws IOException, SQLException{
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
    public static void GuardarLog(String observacionString) throws SQLException{
        Tns.actualizar("INSERT INTO LOGTERPEL(FECHA,OBSERVACIONES)values('now','"+observacionString.replaceAll("'", "")+"')");
    }
    private static OkHttpClient.Builder configureToIgnoreCertificate(OkHttpClient.Builder builder) {
    try {

            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
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
    
    public static void ConsultarDatosUsuario()throws SQLException, ClassNotFoundException{
        String sql="select * from varios where variab like '%TERPEL%'";

        ResultSet rs = Tns.consultar(sql);
        while (rs.next()){
            if (rs.getString("variab").equals("URLTERPEL")) { url = rs.getString("contenido"); }
            if (rs.getString("variab").equals("USERTERPEL")) { usuario = rs.getString("contenido"); }
            if (rs.getString("variab").equals("SECRETOTERPEL")) { password = rs.getString("contenido"); }        
            if (rs.getString("variab").equals("USUARIOTNSTERPEL")) { usuarioTNS = rs.getString("contenido"); }        
            if (rs.getString("variab").equals("CLAVETNSTERPEL")) { passwordTNS = rs.getString("contenido"); }        
            if (rs.getString("variab").equals("EMPRESATNSTERPEL")) { empresaTNS = rs.getString("contenido"); }        
        }
        token=usuario+":"+password;
        token=DatatypeConverter.printBase64Binary(token.getBytes());
    }
    
}
