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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sergio.prueba.ConnectionFirebird;

import okhttp3.*;

/**
 * Consulta las ventas como consularVentasSauceMonterrey (api/Estaciones/{id}/ventas/date/{fecha})
 * y las inserta en TNS por API como consularVentasEDSPalustre (api.tns.co/api/Ventas/Crear).
 */
public class consularVentasEDSCaes {
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
    private static JSONObject objRespues = new JSONObject();
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
                    venta.addProperty("codPrefijo", prefijo); 
                    venta.addProperty("numero", numero);
                    venta.addProperty("fecha", fechaVentaString);  
                    venta.addProperty("codTercero", teridString);    
                    venta.addProperty("codVendedor", vendedorIdString);  
                    venta.addProperty("codDespachar", teridString);    
                    venta.addProperty("codFormaPago", formapago);    
                    venta.addProperty("codBanco", banco);    
                    venta.addProperty("fechaVence", fechaVentaString);
                    venta.addProperty("plazoDias", plazoDias);
                    venta.addProperty("observacion", observaciones);
                    itempedido.addProperty("codMat",matidString);
                    itempedido.addProperty("codBodega", bodega);
                    itempedido.addProperty("codTalla", "");
                    itempedido.addProperty("codColor", "");
                    itempedido.addProperty("cantidad", Cantidad);
                    itempedido.addProperty("tipoUnidad", "D");
                    itempedido.addProperty("descuento", "0");
                    itempedido.addProperty("centrosCostos", centroCosto);
                    itempedido.addProperty("porcIva", "0");
                    itempedido.addProperty("valor", precioBaseFloat);
                    itempedido.addProperty("impConsumo", "0");
                    itempedido.addProperty("observacion", "");
                    itemsPedido.add(itempedido);
                    venta.add("itemsPedido", itemsPedido);
                    itemformapago.addProperty("codFormaPago", formapago);
                    itemformapago.addProperty("plazoDias", plazoDias);
                    itemformapago.addProperty("fechaVencimiento", fechaVentaString);
                    itemformapago.addProperty("valor", sale.get("Valor").toString());
                    itemsFormaPago.add(itemformapago);
                    venta.add("itemsFormaPago", itemsFormaPago);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    json = gson.toJson(venta);
                    OkHttpClient.Builder builder2 = new OkHttpClient.Builder();
                    builder2 = configureToIgnoreCertificate(builder2);
                    builder2.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
                    OkHttpClient client2=builder2.build();
                    RequestBody body = RequestBody.create(mediaType, json);
                    Request request2 = new Request.Builder().url("https://api.tns.co/api/Ventas/Crear?empresa="+empresaTNS+"&usuario="+usuarioTNS+"&password="+passwordTNS+"&tnsapitoken="+tokenTNS+"&codsucursal=00").method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").build();
                    Response response2 = client2.newCall(request2).execute();
                    System.out.println(response2.code());
                    if (response2.code()!=200){
                        String respuesta2=response2.body().string();
                        try {
                            respuesta2=respuesta2.replaceAll("\\\\","");
                            respuesta2=respuesta2.substring(1);
                            respuesta2=respuesta2.substring(0, respuesta2.length()-1);
                            objRespues =new JSONObject(respuesta2);
                            GuardarLog(objRespues.getJSONObject("results").getString("response")+ " en la factura "+prefijo+numero);
                        } catch (Exception e) {
                            GuardarLog("ERROR "+response2.code()+" en la factura "+prefijo+numero);
                        }
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
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        client=builder.build();
        Request request=new Request.Builder().url("https://api.tns.co/api/Material/Listar?empresa="+empresaTNS+"&filtro="+nombre+"&usuario="+usuarioTNS+"&password="+passwordTNS+"&tnsapitoken="+tokenTNS+"&codsuc=00").get()    
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json").build();
        Response response = client.newCall(request).execute();
        String respuesta=response.body().string();        
        try {
            respuesta=respuesta.replaceAll("\\\\","");
            respuesta=respuesta.substring(1);
            respuesta=respuesta.substring(0, respuesta.length() - 1);
            rta=new JSONObject(respuesta);        
            Articulos=rta.getJSONArray("results");
            for (int i = 0; i < Articulos.length(); i++) {
               Articulo=Articulos.getJSONObject(i);
                existencia=Float.parseFloat(Articulo.getString("OEXISTENC"));
                if(existencia>1){
                    codigoString=Articulo.getString("OCODIGO");    
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
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        client=builder.build();
        Request request=new Request.Builder().url("https://api.tns.co/api/Tercero/Listar?empresa="+empresaTNS+"&filtro="+nombre+"&usuario="+usuarioTNS+"&password="+passwordTNS+"&tnsapitoken="+tokenTNS+"&codsucursal=00").get()    
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json").build();
        Response response = client.newCall(request).execute();
        String respuesta=response.body().string();        
        try {                
            respuesta=respuesta.replaceAll("\\\\","");
            respuesta=respuesta.substring(1);
            respuesta=respuesta.substring(0, respuesta.length() - 1);
            rta=new JSONObject(respuesta.replaceAll("\"A\"",""));
            terceros=rta.getJSONArray("results");
            for (int i = 0; i < terceros.length(); i++) {
                tercero=terceros.getJSONObject(i);
                if (tercero.getString("ONOMBRE").toUpperCase().trim().equals(nombre)){
                    teridString=tercero.getString("OCODIGO");                
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
            if (rs.getString("variab").equals("TOKENTERPEL")) { tokenTNS = rs.getString("contenido"); }        
        }
        token=usuario+":"+password;
        token=DatatypeConverter.printBase64Binary(token.getBytes());
    }
    
}
