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
import org.json.JSONTokener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sergio.prueba.ConnectionFirebird;

import okhttp3.*;

public class consularVentasEDSPalustre {
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
            if(!LoginTNS()){
                return;
            }
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
                       venta.addProperty("codigoPrefijo", prefijo); 
                       venta.addProperty("numero", numero);
                       venta.addProperty("fecha", fechaVentaString);  
                       venta.addProperty("codTercero", teridString);    
                       venta.addProperty("codVendedor", vendedorIdString);  
                       venta.addProperty("codDespachar", teridString);    
                       venta.addProperty("codFormaPago", formapago);    
                       venta.addProperty("codBanco", "00");    
                       venta.addProperty("fechaVence", fechaVentaString);
                       venta.addProperty("plazoDias", 0);
                       venta.addProperty("observacion", observaciones);
                       itempedido.addProperty("codMat",matidString);
                       itempedido.addProperty("codBodega", "00");
                       itempedido.addProperty("codTalla", "");
                       itempedido.addProperty("codColor", "");
                       itempedido.addProperty("cantidad", sale.getFloat("cantidad"));
                       itempedido.addProperty("tipoUnidad", "D");
                       itempedido.addProperty("descuento", 0);
                       itempedido.addProperty("centrosCostos", "00");
                       itempedido.addProperty("porcIva", 0);
                       itempedido.addProperty("valor", precioBaseFloat);
                       itempedido.addProperty("impConsumo", 0);
                       itempedido.addProperty("observacion", "");
                       itemsPedido.add(itempedido);
                       venta.add("detallePedido", itemsPedido);
                       itemformapago.addProperty("codigoFormaPago", "CO");
                       itemformapago.addProperty("plazoDias", "0");
                       itemformapago.addProperty("fechaVencimiento", fechaVentaString);
                       itemformapago.addProperty("valor", sale.get("valor_venta_total").toString());
                       itemsFormaPago.add(itemformapago);
                       venta.add("detalleFormaPago", itemsFormaPago);
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
                       RequestBody body = RequestBody.create(mediaType, json);
                       //if(sale.getString("tipo_factura").equals("FE")) {

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
                                GuardarLog(mensaje+ " en la factura "+prefijo+numero);
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
        } catch (JSONException | NumberFormatException e) {
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
        String filtro="";
        if (tipo.equals("cliente")){
            filtro=venta.getString("nombre_cliente");
        }else{
            filtro=venta.getString("promotor");
        }
        HttpUrl urlTercero=HttpUrl.parse(URLTNS+"/v2/tablas/Tercero/Listar").newBuilder()
            .addQueryParameter("filtro", filtro).build();
        Response response = EjecutarTNS(urlTercero, null);
        rta=LeerRespuestaTNS(response.body().string());
        try {                
            clientes=rta.getJSONArray("data");
            for (int i = 0; i < clientes.length(); i++) {
                cliente=clientes.getJSONObject(i);
                if(tipo.equals("cliente")) {
                    if (cliente.optString("nombre").equals(venta.getString("nombre_cliente"))){
                        teridString=cliente.getString("codigo");                
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
                    if (cliente.optString("nombre").equals(venta.getString("promotor"))){
                        teridString=cliente.getString("codigo");                
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
        }
        token=usuario+":"+password;
        token=DatatypeConverter.printBase64Binary(token.getBytes());
        System.out.println(usuario+" "+password);
        System.out.println(token);
    }
    
}
