package com.test.java.com.sergio.facelectronica;
import java.io.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.List;
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

public class consularVentasEDSPalustre {
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

    private static JSONArray productos = new JSONArray();
    private static JsonObject object = new JsonObject(); 
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
        String fechaFin=data.split("\\|")[3];
        String insertar=data.split("\\|")[4];
        String establecimiento=data.split("\\|")[5];
        String sucid="";
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario();
        ConsultarPedidos(fecha,fechaFin,insertar,establecimiento,sucid);
        Tns.cerrarConexion();
        br.close();
        System.out.println("termine");
    }
    public static String BuscarPrefijo(String prefijo) throws ClassNotFoundException, SQLException{
        String prefijoString="";
        sqlString="select codprefijo from prefijo where preimp='"+prefijo+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            prefijoString=rs.getString("codprefijo");
        }
        if(prefijoString.equals("")){prefijoString=prefijo;}
        return prefijoString;
    }
    
    public static void ConsultarPedidos(String fechaString,String fechaFinString,String insertaString,String establecimiento,String sucid) throws IOException, ClassNotFoundException, JSONException, SQLException{
    
    System.out.println(establecimiento);    
    String teridString="";
    String vendedorIdString="";
    String fechaVentaString="";
    String kardexidString="";
    String matidString="";
    String codcomp="";
    String prefijo="";
    String numero="";    
    String observaciones="";
    String hora="";
    String bancoId="";
    String formapago="";
    Float precioBaseFloat=(float) 0;
    ConsultarToken();    
    OkHttpClient client =new OkHttpClient();
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder = configureToIgnoreCertificate(builder);
    builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
    MediaType mediaType = MediaType.parse("application/json");
    client=builder.build();
    Request request = new Request.Builder().url(url+":7008/api/v2/venta_detallada?fecha_inicial="+fechaString+"&fecha_final="+fechaFinString+"&identificadorEstacion="+establecimiento).get()    
    .addHeader("Authorization", "Bearer "+token)
    .addHeader("Content-Type", "application/json")
    .addHeader("Accept", "application/json").build();
    Response response = client.newCall(request).execute();
    System.out.println(response.code());
   // System.out.println(response.body().string());
    String respuesta=response.body().string();
    obj =new JSONObject(respuesta);
        if(insertaString.equals("N")){
            if(response.code()==200){
                System.out.println(obj.getJSONArray("data").length());
                sqlString="delete from varios where variab='CANTIDADTERPEL'";
                Tns.actualizar(sqlString);
                sqlString="insert into varios (contenido,variab) values('"+obj.getJSONArray("data").length()+"','CANTIDADTERPEL')";  
                Tns.actualizar(sqlString);
            } else {
                
            }
        }else{
            sqlString="delete from varios where variab='CANTIDADTERPEL'";
            Tns.actualizar(sqlString);
            sqlString="insert into varios (contenido,variab) values('"+obj.getJSONArray("data").length()+"','CANTIDADTERPEL')";  
            Tns.actualizar(sqlString);
            sqlString="delete from varios where variab='CANTIDADTERPELSUBIDA'";
            Tns.actualizar(sqlString);
            sqlString="delete from logterpel";
            Tns.actualizar(sqlString);
            sqlString="insert into varios (contenido,variab) values('0','CANTIDADTERPELSUBIDA')";  
            Tns.actualizar(sqlString);    
            for (int i = 0; i < obj.getJSONArray("data").length(); i++) {
                sale=obj.getJSONArray("data").getJSONObject(i); 
                //System.out.println(String.valueOf(i)+" - "+sale.getString("consecutivo_factura"));
                if(sale.getString("consecutivo_factura").contains("6896")){
                    System.out.println("por aqui");
                }
                if(sale.getString("metodo_pago").equals("EFECTIVO")) {
         //   for (int i = 0; i < 5; i++) { 
                teridString=""; 
                fechaVentaString="";
                vendedorIdString="";
                kardexidString="";
                codcomp="";
                observaciones="";
                formapago="CO";
                 
                teridString=ConsultarTerid(sale,"cliente");  
                vendedorIdString=ConsultarTerid(sale, "vendedor");
                observaciones=sale.getString("consecutivo_factura")+" "+sale.getString("placa");
                
                matidString=BuscarMaterialAPI(sale.getString("producto"));        
                
                    prefijo=sale.getString("consecutivo_factura").split("-")[0].trim();                        
                    numero=sale.getString("consecutivo_factura").split("-")[1].trim();
                                  
                
                codcomp="FV";
                fechaVentaString=sale.getString("fecha");
                hora=sale.getString("hora");
                    if (sale.getString("hora").split(" ")[1].equals("pm")){
                        switch (hora.substring(0, 2)) {
                            case "01": hora="13:"+hora.substring(3,5); break;                        
                            case "02": hora="14:"+hora.substring(3, 5); break;                        
                            case "03": hora="15:"+hora.substring(3, 5); break;                        
                            case "04": hora="16:"+hora.substring(3, 5); break;                        
                            case "05": hora="17:"+hora.substring(3, 5); break;                        
                            case "06": hora="18:"+hora.substring(3, 5); break;                        
                            case "07": hora="19:"+hora.substring(3, 5); break;                        
                            case "08": hora="20:"+hora.substring(3, 5); break;                        
                            case "09": hora="21:"+hora.substring(3, 5); break;                        
                            case "10": hora="22:"+hora.substring(3, 5); break;                        
                            case "11": hora="23:"+hora.substring(3, 5); break;                        
                            case "12": hora=hora.substring(0, 5);                      
                            default:
                                break;
                        }
                    }else{
                        if(hora.substring(0, 2).equals("12")){
                            hora="00:"+hora.substring(3, 5); 
                        }else{
                            hora=hora.substring(0, 5);                         
                        }                   
                    }
                    //fechaVentaString=fechaString;                    
                    fechaVentaString=fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(0, 4);
                    precioBaseFloat=sale.getFloat("valor_venta_total")/sale.getFloat("cantidad");
                    JsonObject venta = new JsonObject();
                    JsonArray itemsPedido = new JsonArray();
                    JsonObject itempedido = new JsonObject();
                    JsonArray itemsFormaPago = new JsonArray();
                    JsonObject itemformapago = new JsonObject();
                    JsonArray itemsDescuentos = new JsonArray();
                    JsonObject itemDescuento = new JsonObject();
                    if(!matidString.equals("00")){
                        precioBaseFloat=sale.getFloat("valor_venta_total")/sale.getFloat("cantidad");
                       venta.addProperty("codPrefijo", prefijo); 
                       venta.addProperty("numero", numero);
                       venta.addProperty("fecha", fechaVentaString);  
                       venta.addProperty("codTercero", teridString);    
                       venta.addProperty("codVendedor", vendedorIdString);  
                       venta.addProperty("codDespachar", teridString);    
                       venta.addProperty("codFormaPago", formapago);    
                       venta.addProperty("codBanco", "00");    
                       venta.addProperty("fechaVence", fechaVentaString);
                       venta.addProperty("plazoDias", "0");
                       venta.addProperty("observacion", observaciones);
                       itempedido.addProperty("codMat",matidString);
                       itempedido.addProperty("codBodega", "00");
                       itempedido.addProperty("codTalla", "");
                       itempedido.addProperty("codColor", "");
                       itempedido.addProperty("cantidad", sale.getFloat("cantidad"));
                       itempedido.addProperty("tipoUnidad", "D");
                       itempedido.addProperty("descuento", "0");
                       itempedido.addProperty("centrosCostos", "00");
                       itempedido.addProperty("porcIva", "0");
                       itempedido.addProperty("valor", precioBaseFloat);
                       itempedido.addProperty("impConsumo", "0");
                       itempedido.addProperty("observacion", "");
                       itemsPedido.add(itempedido);
                       venta.add("itemsPedido", itemsPedido);
                       itemformapago.addProperty("codFormaPago", "CO");
                       itemformapago.addProperty("plazoDias", "0");
                       itemformapago.addProperty("fechaVencimiento", fechaVentaString);
                       itemformapago.addProperty("valor", sale.get("valor_venta_total").toString());
                       itemsFormaPago.add(itemformapago);
                       venta.add("itemsFormaPago", itemsFormaPago);
                       itemDescuento.addProperty("codconcepto", "00");
                       itemDescuento.addProperty("valordto", "0");
                       itemDescuento.addProperty("baseretd", "0");
                       itemDescuento.addProperty("porcretd", "0");
                       itemDescuento.addProperty("porivad", "0");
                       itemDescuento.addProperty("centrocostos", "00");
                       itemDescuento.addProperty("codarea", "00");
                       itemsDescuentos.add(itemDescuento);
                       //venta.add("itemsDescuentos", itemsDescuentos);
                       Gson gson = new GsonBuilder().setPrettyPrinting().create();
                       json = gson.toJson(venta);
                      // System.out.println(json);
                       OkHttpClient client2 =new OkHttpClient().newBuilder().connectTimeout(240, TimeUnit.SECONDS).readTimeout(240, TimeUnit.SECONDS).build();
                       OkHttpClient.Builder builder2 = new OkHttpClient.Builder();
                       builder2 = configureToIgnoreCertificate(builder2);
                       MediaType mediaType2= MediaType.parse("application/json");
                       RequestBody body = RequestBody.create(mediaType, json);
                       builder2.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
                       client2=builder2.build();
                       //if(sale.getString("tipo_factura").equals("FE")) {

                            Request request2 = new Request.Builder().url("https://api.tns.co/api/Ventas/Crear?empresa="+empresaTNS+"&usuario="+usuarioTNS+"&password="+passwordTNS+"&tnsapitoken="+tokenTNS+"&codsucursal=00").method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").build();
                            Response response2 = client2.newCall(request2).execute();
                            System.out.println(response2.code());
                            if (response2.code()!=200){
                                String respuesta2=response2.body().string();
                                respuesta2=respuesta2.replaceAll("\\\\","");
                                respuesta2=respuesta2.substring(1);
                                respuesta2=respuesta2.substring(0, respuesta2.length()-1);
                                objRespues =new JSONObject(respuesta2);
                                GuardarLog(objRespues.getJSONObject("results").getString("response")+ " en la factura "+prefijo+numero);
                            }
                       // }
                       
                    
                       //System.out.println(venta);


                    }                   
                   //System.out.println(i +" de "+obj.getJSONArray("data").length()+1);         
                }
                Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
            }
        }
    }


    public static String ObtenerCampo(JSONObject jsonObject,String campo){
        String respuestaString="";
        if (!jsonObject.get(campo).equals(null)){
            respuestaString=jsonObject.getString(campo);
        }
        return respuestaString;
    }

    public static String BuscarMaterialAPI(String filtro) throws ClassNotFoundException, SQLException, IOException{
        String codigoString = "00";
        String nombre="";
        float existencia=0;
        nombre=filtro.split(" ")[0]+" "+filtro.split(" ")[1];
        JSONObject rta = new JSONObject();
        JSONArray Articulos = new JSONArray();
        JSONObject Articulo = new JSONObject();
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        MediaType mediaType = MediaType.parse("application/json");
        client=builder.build();
        Request request = null;        
        request=new Request.Builder().url("https://api.tns.co/api/Material/Listar?empresa="+empresaTNS+"&filtro="+nombre+"&usuario="+usuarioTNS+"&password="+passwordTNS+"&tnsapitoken="+tokenTNS+"&codsuc=00").get()    
        .addHeader("Authorization", "Bearer "+token)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json").build();
        Response response = client.newCall(request).execute();
        String respuesta=response.body().string();        
        respuesta=respuesta.replaceAll("\\\\","");
        respuesta=respuesta.substring(1);
        respuesta=respuesta.substring(0, respuesta.length() - 1);
        rta=new JSONObject(respuesta);        
        try {
            Articulos=rta.getJSONArray("results");
            for (int i = 0; i < Articulos.length(); i++) {
               Articulo=Articulos.getJSONObject(i);
                existencia=Float.parseFloat(Articulo.getString("OEXISTENC"));
                if(existencia>1){
                    codigoString=Articulo.getString("OCODIGO");    
                }                
            }
        } catch (JSONException e) {
            codigoString="00";
            GuardarLog("el articulo no se encontro"+filtro);
        }
       // System.out.println(codigoString);
        return codigoString;
    }

    public static String ConsultarTerid(JSONObject venta,String tipo) throws ClassNotFoundException, SQLException, IOException{
        String teridString="";
        JSONObject rta = new JSONObject();
        JSONArray clientes = new JSONArray();
        JSONObject cliente = new JSONObject();
        JsonObject tercero = new JsonObject();
        String nombre="";
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        MediaType mediaType = MediaType.parse("application/json");
        client=builder.build();
        Request request = null;
        if (tipo.equals("cliente")){
            request=new Request.Builder().url("https://api.tns.co/api/Tercero/Listar?empresa="+empresaTNS+"&filtro="+venta.getString("nombre_cliente")+"&usuario="+usuarioTNS+"&password="+passwordTNS+"&tnsapitoken="+tokenTNS+"&codsucursal=00").get()    
            .addHeader("Authorization", "Bearer "+token)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json").build();
           // System.out.println(venta.getString("nombre_cliente"));
        }else{
            request=new Request.Builder().url("https://api.tns.co/api/Tercero/Listar?empresa="+empresaTNS+"&filtro="+venta.getString("promotor")+"&usuario="+usuarioTNS+"&password="+passwordTNS+"&tnsapitoken="+tokenTNS+"&codsucursal=00").get()    
            .addHeader("Authorization", "Bearer "+token)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json").build(); 
          //  System.out.println(venta.getString("promotor"));           
        }
        
        Response response = client.newCall(request).execute();
        String respuesta=response.body().string();        
        respuesta=respuesta.replaceAll("\\\\","");
        respuesta=respuesta.substring(1);
        respuesta=respuesta.substring(0, respuesta.length() - 1);
        rta=new JSONObject(respuesta.replaceAll("\"A\"",""));
        try {                
            clientes=rta.getJSONArray("results");
            for (int i = 0; i < clientes.length(); i++) {
                cliente=clientes.getJSONObject(i);
                if(tipo.equals("cliente")) {
                    if (cliente.getString("ONOMBRE").equals(venta.getString("nombre_cliente"))){
                        teridString=cliente.getString("OCODIGO");                
                    }else{
                        teridString="222222222222";
                        tercero.addProperty("codigo", venta.getString("numero_documento_fe"));
                        tercero.addProperty("natJuridica", "N");
                        tercero.addProperty("tipoDocIden", "C");
                        tercero.addProperty("nit", venta.getString("numero_documento_fe"));
                        tercero.addProperty("ciudadExp", "-");
                        tercero.addProperty("nombre", venta.getString("nombre_cliente"));
                        tercero.addProperty("nomRegTri", venta.getString("nombre_cliente"));
                        tercero.addProperty("codCiudane", "00");
                        tercero.addProperty("ciudad", "SIN CIUDAD");
                        tercero.addProperty("zona1", "00");
                        tercero.addProperty("clasificacion", "00");
                        tercero.addProperty("inactivo", false);
                        tercero.addProperty("privada", "N");
                        tercero.addProperty("mixta", "N");
                        tercero.addProperty("cliente", "N");
                        
                    }
                }else{
                    if (cliente.getString("ONOMBRE").equals(venta.getString("promotor"))){
                        teridString=cliente.getString("OCODIGO");                
                    }else{
                        teridString="0";
                    }
                }

            }
        }
        catch (JSONException e) {
            teridString="222222222222";
        }    
     //  System.out.println(teridString);
        return teridString;
    }
    public static void GuardarLog(String observacionString) throws SQLException{
        Tns.actualizar("INSERT INTO LOGTERPEL(FECHA,OBSERVACIONES)values('now','"+observacionString+"')");
    }
    public static void ConsultarToken()throws IOException, ClassNotFoundException, JSONException, SQLException{
        object.addProperty("user", usuario);
        object.addProperty("pass", password);
        object.addProperty("identificadorNegocio", 14);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(object);
        System.out.println(json);
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);
        client=builder.build();
        Request request = new Request.Builder().url(url+":7010/api/v1/signin").method("POST", body)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json").build();
        Response response = client.newCall(request).execute();
        System.out.println(response.code());
        String respuesta=response.body().string();
        obj =new JSONObject(respuesta);
        token= obj.getJSONObject("data").getString("token");
        System.out.println(token);
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
            System.out.println(rs.getString("contenido"));
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
        System.out.println(usuario+" "+password);
        System.out.println(token);
    }
    
}
