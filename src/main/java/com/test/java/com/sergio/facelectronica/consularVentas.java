package com.test.java.com.sergio.facelectronica;
import java.io.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
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
import com.google.gson.JsonObject;
import com.sergio.prueba.ConnectionFirebird;

import okhttp3.*;

public class consularVentas {
    private static ConnectionFirebird Tns=null;
    private static String url="";
    private static String usuario="";
    private static String password="";    
    private static String token="";
    private static JSONObject obj = new JSONObject();
    private static JSONObject sale = new JSONObject();
    private static JSONObject producto = new JSONObject();
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
        String insertar=data.split("\\|")[3];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario();
        ConsultarPedidos(fecha,insertar);
        Tns.cerrarConexion();
        br.close();
        System.out.println("termine");
    }
    public static void ConsultarPedidos(String fechaString,String insertaString) throws IOException, ClassNotFoundException, JSONException, SQLException{
    String teridString="";
    String vendedorIdString="";
    String fechaVentaString="";
    String kardexidString="";
    String matidString="";
    String prefijoString="";
    Float precioBaseFloat=(float) 0;
    ConsultarToken();    
    object=new JsonObject();
    object.addProperty("date", fechaString);
    object.addProperty("branch_id", 840);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    json = gson.toJson(object);
    System.out.println(json);
    OkHttpClient client =new OkHttpClient();
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder = configureToIgnoreCertificate(builder);
    MediaType mediaType = MediaType.parse("application/json");
    RequestBody body = RequestBody.create(mediaType, json);
    client=builder.build();
    Request request = new Request.Builder().url(url+"/integrations/administrative/dominus/sales?date="+fechaString+"&branch_id=840").get()    
    .addHeader("Authorization", "Bearer "+token)
    .addHeader("Content-Type", "application/json")
    .addHeader("Accept", "application/json").build();
    Response response = client.newCall(request).execute();
    System.out.println(response.code());
    String respuesta=response.body().string();
    obj =new JSONObject(respuesta);
        if(insertaString.equals("N")){
            if(obj.getJSONObject("messages").get("error").equals(0)){
                System.out.println(obj.getJSONObject("data").getJSONArray("sales").length());
                sqlString="delete from varios where variab='CANTIDADTERPEL'";
                Tns.actualizar(sqlString);
                sqlString="insert into varios (contenido,variab) values('"+obj.getJSONObject("data").getJSONArray("sales").length()+"','CANTIDADTERPEL')";  
                Tns.actualizar(sqlString);
            } else {
                
            }
        }else{
            sqlString="delete from varios where variab='CANTIDADTERPEL'";
            Tns.actualizar(sqlString);
            sqlString="insert into varios (contenido,variab) values('"+obj.getJSONObject("data").getJSONArray("sales").length()+"','CANTIDADTERPEL')";  
            Tns.actualizar(sqlString);
            sqlString="delete from varios where variab='CANTIDADTERPELSUBIDA'";
            Tns.actualizar(sqlString);
            sqlString="delete from logterpel";
            Tns.actualizar(sqlString);
            sqlString="insert into varios (contenido,variab) values('0','CANTIDADTERPELSUBIDA')";  
            Tns.actualizar(sqlString);    
            for (int i = 0; i < obj.getJSONObject("data").getJSONArray("sales").length(); i++) {
            //for (int i = 0; i < 5; i++) { 
                teridString=""; 
                fechaVentaString="";
                vendedorIdString="";
                kardexidString="";
                sale=obj.getJSONObject("data").getJSONArray("sales").getJSONObject(i);  
                teridString=ConsultarTerid(sale,"cliente");  
                vendedorIdString=ConsultarTerid(sale, "vendedor");
                if (BuscarVenta(sale)) {
                    GuardarLog("LA FACTURA "+sale.get("document").toString()+" YA EXISTE");
                } else {
                    if(ObtenerCampo(sale, "prefix").equals("")){
                        prefijoString="00";
                        GuardarLog("LA FACTURA "+sale.get("document").toString()+" se creo con el prefijo 00");
                    }else{
                        prefijoString=ObtenerCampo(sale, "prefix");
                    }
                    fechaVentaString=sale.getString("date_sale").split(" ")[0];
                    fechaVentaString=fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);
                    sqlString="insert into kardex(codcomp,codprefijo,numero,fecha,periodo,cenid,areadid,sucid,cliente,vendedor,formapago,bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto,netobase)"+
                              "values('FV'"+
                              ",'"+prefijoString+"'"+
                              ",'"+sale.get("document").toString()+"'"+
                              ",'"+fechaVentaString+"'"+
                              ",'"+fechaVentaString.substring(0, 2)+"'"+
                              ",1"+
                              ",1"+
                              ",1"+
                              ",'"+teridString+"'"+
                              ",'"+vendedorIdString+"'"+
                              ",'CO'"+
                              ",34"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",'"+sale.get("total").toString()+"'"+
                              ",0"+
                              ",'"+sale.get("total").toString()+"'"+
                              ",'"+sale.get("total").toString()+"'"+
                              ",0"+
                              ",'"+teridString+"'"+
                              ",1"+
                              ",'"+sale.get("total").toString()+"'"+
                              ",'"+sale.get("total").toString()+"'"+
                              ",'"+sale.get("total").toString()+"'"+
                              ")";
                    Tns.actualizar(sqlString);
                    productos=sale.getJSONArray("products");
                    kardexidString=ConsultarVenta(sale.get("document").toString(),prefijoString);
                    for (int j = 0; j < productos.length(); j++) {
                        precioBaseFloat=(float) 0;
                        producto=productos.getJSONObject(j);
                        if(BuscarMaterial(producto)){
                            matidString=ConsultarMaterial(producto);
                            precioBaseFloat=producto.getFloat("total")/producto.getFloat("quantity");
                            sqlString="insert into dekardex(kardexid,matid,bodid,prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta,preciobase"+
                                      ",precioiva,precioneto,parcvta,preciotasa,parctasa)"+
                                      "values('"+kardexidString+"'"+
                                      ",'"+matidString+"'"+
                                      ",'"+ConsultarBodega(producto)+"'"+
                                      ",7,0,'D',0,0"+
                                      ",'"+producto.get("quantity").toString()+"'"+
                                      ",'"+producto.get("quantity").toString()+"'"+
                                      ",'"+precioBaseFloat+"'"+
                                      ",'"+precioBaseFloat+"'"+
                                      ",'"+precioBaseFloat+"'"+
                                      ",0"+
                                      ",'"+precioBaseFloat+"'"+
                                      ",'"+producto.get("total").toString()+"'"+
                                      ",'"+precioBaseFloat+"'"+
                                      ",'"+producto.get("total").toString()+"'"+
                                      ")";
                                      Tns.actualizar(sqlString);
                        }else{
                            GuardarLog("EL ARTICULO "+producto.getString("product_name")+" NO EXISTE, EN LA FACTURA "+sale.getString("PREFIX")+sale.get("document").toString());
                        }
                    }
                   Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
                   System.out.println(i +" de "+obj.getJSONObject("data").getJSONArray("sales").length()); 
                }
            }
        }
    }
    public static String ConsultarBodega(JSONObject productObject) throws ClassNotFoundException, SQLException{
        String bodegaString="1";
        sqlString="select bodid from bodega where codigo='"+productObject.getString("bodega")+"' ";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            bodegaString=rs.getString("bodid");
        }
        return bodegaString;
    }

    public static String ConsultarVenta(String numero,String prefijo) throws ClassNotFoundException, SQLException{
        String kardexidString="";
        sqlString="select kardexid from kardex where codcomp='FV' and codprefijo='"+prefijo+"' and numero='"+numero+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            kardexidString=rs.getString("kardexid");
        }
        return kardexidString;
    }
    public static Boolean BuscarMaterial(JSONObject productJsonObject) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        sqlString="select matid from material where (codigo ='"+productJsonObject.getString("product_code")+"' or referencia='"+productJsonObject.getString("product_code")+"')";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            existe=true;
        }
        return existe;
    }
    public static String ConsultarMaterial(JSONObject productJsonObject) throws ClassNotFoundException, SQLException{
        String matidString="";
        sqlString="select matid from material where (codigo ='"+productJsonObject.getString("product_code")+"' or referencia='"+productJsonObject.getString("product_code")+"')";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("matid");
        }
        return matidString;
    }
    public static Boolean BuscarVenta(JSONObject ventaJsonObject) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        sqlString="select kardexid from kardex where codcomp='FV' and codprefijo='"+ventaJsonObject.get("prefix").toString()+"' and numero='"+ventaJsonObject.get("document").toString()+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            existe=true;
        }
        return existe;
    }
    public static String ObtenerCampo(JSONObject jsonObject,String campo){
        String respuestaString="";
        if (!jsonObject.get(campo).equals(null)){
            respuestaString=jsonObject.getString(campo);
        }
        return respuestaString;
    }

    public static String ConsultarTerid(JSONObject venta,String tipo) throws ClassNotFoundException, SQLException{
        String teridString="";
        if(tipo.equals("cliente")){
            sqlString="select terid from terceros where (nit ='"+venta.getString("customer_document")+"' or nittri ='"+venta.getString("customer_document")+"' )";
        }else{
            sqlString="select terid from terceros where (nit ='"+ObtenerCampo(venta, "employee_document")+"' or nittri ='"+ObtenerCampo(venta, "employee_document")+"' )";
        }
        
        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            teridString=rs.getString("terid");
        }
        if(teridString.isEmpty()){
            if(tipo.equals("cliente")){
            sqlString="insert into terceros(nit,nittri,nombre,direcc1,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
            "values('"+venta.getString("customer_document")+"','"+venta.getString("customer_document")+"'"+
            ",'"+venta.getString("customer_first_name")+' '+ObtenerCampo(venta, "customer_last_name")+"','"+ObtenerCampo(venta, "customer_address")+"'"+
            ",'"+ObtenerCampo(venta, "customer_phone")+"','','C','1','S','now',1,1)";
            Tns.actualizar(sqlString);
            GuardarLog("SE CREO EL TERCERO CON EL SIGUIENTE NIT: "+venta.getString("customer_document"));
            sqlString="select terid from terceros where (nit ='"+venta.getString("customer_document")+"' or nittri ='"+venta.getString("customer_document")+"' )";
                rs =Tns.consultar(sqlString);    
                if (rs.next()){
                    teridString=rs.getString("terid");
                } 
            }else{
             if(!ObtenerCampo(venta, "employee_document").equals("")){
                    sqlString="insert into terceros(nit,nittri,nombre,direcc1,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
                                "values('"+venta.getString("employee_document")+"','"+venta.getString("employee_document")+"'"+
                                ",'"+venta.getString("employee_first_name")+' '+venta.getString("employee_last_name")+ "','SIN DIRECCION'"+
                                ",'0','','C','1','S','now',1,1)"; 
                                Tns.actualizar(sqlString);
                                GuardarLog("SE CREO EL TERCERO CON EL SIGUIENTE NIT: "+venta.getString("employee_document"));
                                sqlString="select terid from terceros where (nit ='"+venta.getString("employee_document")+"' or nittri ='"+venta.getString("employee_document")+"' )";      
                                rs =Tns.consultar(sqlString);    
                                if (rs.next()){
                                    teridString=rs.getString("terid");
                                }       
                }else{
                    teridString="1";
                }           
            }       
        }

        return teridString;
    }
    public static void GuardarLog(String observacionString) throws SQLException{
        Tns.actualizar("INSERT INTO LOGTERPEL(FECHA,OBSERVACIONES)values('now','"+observacionString+"')");
    }
    public static void ConsultarToken()throws IOException, ClassNotFoundException, JSONException, SQLException{
        object.addProperty("client_id", usuario);
        object.addProperty("client_secret", password);
        object.addProperty("grant_type", "client_credentials");
        object.addProperty("scope", "dominus");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(object);
        System.out.println(json);
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);
        client=builder.build();
        Request request = new Request.Builder().url(url+"/oauth/v2/token").method("POST", body)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json").build();
        Response response = client.newCall(request).execute();
        System.out.println(response.code());
        String respuesta=response.body().string();
        obj =new JSONObject(respuesta);
        token= obj.getString("access_token");
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
    public static void InsertaPedido(JSONObject pedido) throws SQLException{
        String sql="insert into pedidosicom(id,proveedor,proveedornit,autorizacion,codigo_comprador,despacho_codigo,recibo_planta,tipo_transporte,placa,placa_remolque"+
        ",conductor,cedula_conductor,fecha_entrega,fecha,sobrecupo,observaciones,productos,clave,rowversion,Nro_Caso)"+
        "values("+
        "'"+pedido.getString("Id")+"',"+
        "'"+pedido.getString("Proveedor_Codigo_SICOM")+"',"+
        "'"+pedido.getString("Proveedor_NIT")+"',"+
        "'"+pedido.getString("OP_Autorizacion_Codigo")+"',"+
        "'"+pedido.getString("Comprador_Codigo_Externo")+"',"+
        "'"+pedido.getString("Despacho_Planta_Codigo")+"',"+
        "'"+pedido.get("Recibo_Planta_Codigo").toString()+"',"+
        "'"+pedido.get("Transporte_Tipo_Codigo").toString()+"',"+
        "'"+pedido.get("Transporte_Placa").toString()+"',"+
        "'"+pedido.get("Transporte_Placa_Remolque").toString()+"',"+
        "'"+pedido.get("Transporte_Conductor").toString()+"',"+
        "'"+pedido.get("Transporte_Cedula_Conductor").toString()+"',"+
        "'"+pedido.getString("FechaEntrega")+"',"+
        "'"+pedido.getString("Fecha")+"',"+
        "'"+pedido.getString("SobreCupo")+"',"+
        "'"+pedido.get("Observaciones").toString()+"',"+
        "'"+pedido.getString("Productos")+"',"+
        "'"+pedido.get("Seguridad_Contrasena").toString()+"',"+
        "'"+pedido.getString("RowVersion")+"',"+
        "'"+pedido.getString("Nro_Caso")+"'"+
        ")";
        Tns.actualizar(sql);
    } 
    public static Boolean ConsultarPedido(String idPedido) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        String sql="select * from pedidosicom where Id='"+idPedido+"'";
        ResultSet rs =Tns.consultar(sql);
        while(rs.next()){
            existe=true;
        }
        return existe;
    }
    public static void ConsultarDatosUsuario()throws SQLException, ClassNotFoundException{
        String sql="select * from varios where variab like '%TERPEL%'";

        ResultSet rs = Tns.consultar(sql);
        while (rs.next()){
            System.out.println(rs.getString("contenido"));
            if (rs.getString("variab").equals("URLTERPEL")) { url = rs.getString("contenido"); }
            if (rs.getString("variab").equals("USERTERPEL")) { usuario = rs.getString("contenido"); }
            if (rs.getString("variab").equals("SECRETOTERPEL")) { password = rs.getString("contenido"); }        
        }
        token=usuario+":"+password;
        token=DatatypeConverter.printBase64Binary(token.getBytes());
        System.out.println(usuario+" "+password);
        System.out.println(token);
    }
    
}
