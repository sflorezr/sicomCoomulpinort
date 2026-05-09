package com.test.java.com.sergio.facelectronica;
import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.sql.ResultSet;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sergio.prueba.ConnectionFirebird;

import okhttp3.*;

public class consularVentasTerpelKiosko {
    private static ConnectionFirebird Tns=null;
    private static String url="";
    private static String usuario="";
    private static String password="";    
    private static String token="";
    private static JSONObject obj = new JSONObject();
    private static JSONObject sale = new JSONObject();    
    private static JSONObject detail = new JSONObject();    
    private static JSONObject detailIva = new JSONObject();    
    private static JSONObject details = new JSONObject();    
    private static JsonObject object = new JsonObject(); 
    private static String json =null; 
    private static String sqlString="";

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException,JSONException, InterruptedException{
        File tempo = new File("c:\\tempo\\terpel.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String ruta=data.split("\\|")[0];
        String ip=data.split("\\|")[1];
        String fecha=data.split("\\|")[2];
        String fechaFin=data.split("\\|")[3];
        String insertar=data.split("\\|")[4];
        String idString=data.split("\\|")[5];
        String sucid=data.split("\\|")[6];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario();
        ConsultarPedidos(fecha,fechaFin,insertar,idString,sucid);
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
    public static void ConsultarPedidos(String fechaString,String fechaFinString,String insertaString,String idString,String sucid) throws IOException, ClassNotFoundException, JSONException, SQLException, InterruptedException{
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
    String banco="";
    String cencos="";
    String porciva="";
    Float precioBaseFloat=(float) 0;
    Float precioVenta=(float) 0;
    Float PrecioIva=(float) 0;
    Float ImpuestoConsumo=(float) 0;
    String grupo="";
    ConsultarToken();   
    Tns.actualizar("delete from logterpel");
    object.addProperty("fecha_inicial", fechaString); 
    object.addProperty("fecha_final", fechaFinString); 
    object.addProperty("identificadorEstacion", idString); 
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    json = gson.toJson(object);
    System.out.println(json);
    HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url+":7008/api/reportes/VENTAS_LITE/kiosco"))
    .header("Accept", "application/json, text/plain, */*")    
    .header("Accept-Language", "es,es-ES;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
    .header("Content-Type", "application/json")
    .header("aplicacion", "CENTURY_APP")
    .header("fecha", "2024-02-21T15:24:41-05:00")
    .header("identificadordispositivo", "CENTURY_APP")
    .header("versionapp", "6.2.0.0")
    .header("versioncode", "6.2.0.0")
    .header("Authorization", "Bearer "+token)
    .method("POST", HttpRequest.BodyPublishers.ofString(json))
    .build();
HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());

    String respuesta=response.body().toString();
    obj =new JSONObject(respuesta);
        if(insertaString.equals("N")){
            if(response.statusCode()==200){
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
            //for (int i = 0; i < 5; i++) { 
                teridString=""; 
                fechaVentaString="";
                vendedorIdString="";
                kardexidString="";
                codcomp="";
                observaciones="";
                banco="1";
                cencos="";
                sale=obj.getJSONArray("data").getJSONObject(i);  
                teridString="1";  
                vendedorIdString=ConsultarTerid(sale, "vendedor");
                observaciones=sale.getString("promotor");
                Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
                if (BuscarVenta(sale)) {
                    GuardarLog("LA FACTURA "+sale.getString("consecutivo")+" YA EXISTE");                    
                } else {
                    prefijo=sale.getString("consecutivo_prefijo");                        
                    prefijo=BuscarPrefijo(prefijo);
                    numero=sale.getString("consecutivo");       
                    codcomp="FV";
                    fechaVentaString=sale.getString("fecha").split(" ")[0];
                    fechaVentaString=fechaVentaString.substring(5,7)+"/"+fechaVentaString.substring(8,10)+"/"+fechaVentaString.substring(0,4);
                    hora=sale.getString("fecha").split(" ")[1];
                    hora=hora.substring(0, 5);                         
                    //fechaVentaString=fechaString;
                  //  fechaVentaString=fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);
                  if(prefijo.length()>2) {
                    GuardarLog("la factura "+numero+" tiene un prefijo que no existe en tns "+prefijo);
                    continue;
                  }
                  banco=ConsultarBanco("00");
                  cencos=ConsultarCentro("00");
                    sqlString="insert into kardex(codcomp,codprefijo,numero,observ,fecha,periodo,cenid,areadid,sucid,cliente,vendedor,formapago,bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto,netobase,hora)"+
                              "values('"+codcomp+"'"+
                              ",'"+prefijo+"'"+
                              ",'"+numero+"'"+
                              ",'"+observaciones+"'"+
                              ",'"+fechaVentaString+"'"+
                              ",'"+fechaVentaString.substring(0, 2)+"'"+
                              ",'"+cencos+"'"+
                              ",1"+
                              ","+sucid+
                              ",'"+teridString+"'"+
                              ",'"+vendedorIdString+"'"+
                              ",'CO'"+
                              ",'"+banco+"'"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",'"+sale.get("totalventa").toString()+"'"+
                              ",0"+
                              ",'"+sale.get("totalventa").toString()+"'"+
                              ",'"+sale.get("totalventa").toString()+"'"+
                              ",0"+
                              ",'"+teridString+"'"+
                              ",1"+
                              ",'"+sale.get("totalventa").toString()+"'"+
                              ",'"+sale.get("totalventa").toString()+"'"+
                              ",'"+sale.get("totalventa").toString()+"'"+
                              ",'"+hora+"'"+
                              ")";
                    Tns.actualizar(sqlString);
                    kardexidString=ConsultarVenta(codcomp,numero,prefijo);
                    OkHttpClient client =new OkHttpClient();
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                    builder = configureToIgnoreCertificate(builder);
                    Request requestSegundo = new Request.Builder().url(url+":7008/api/reportes/obtener-detalles/"+sale.get("id").toString()).get()
                    .addHeader("Authorization", "Bearer "+token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json").build();
                    try{
                        Response responseSegundo = client.newCall(requestSegundo).execute();
                        //System.out.println(responseSegundo.code());
                        if(responseSegundo.code()!=200){
                            GuardarLog("ocurrio un error con el webservice "+prefijo+numero);
                            Tns.actualizar("delete from kardex where kardexid="+kardexidString);
                            continue;
                        }
                        //System.out.println(responseSegundo.body().string());
                        respuesta=responseSegundo.body().string();
                    }catch(SSLHandshakeException e){
                        GuardarLog("ocurrio un error con el webservice "+prefijo+numero);
                        Tns.actualizar("delete from kardex where kardexid="+kardexidString);
                        continue;
                    }catch(SocketTimeoutException s){
                        GuardarLog("ocurrio un error con el webservice "+prefijo+numero);
                        Tns.actualizar("delete from kardex where kardexid="+kardexidString);
                        continue;
                    }

                    //System.out.println(respuesta);
                    details =new JSONObject(respuesta);
                    for (int j = 0; j < details.getJSONObject("data").getJSONArray("detalles").length(); j++) {
                        precioBaseFloat=(float) 0;
                        precioVenta=(float) 0;
                        PrecioIva=(float) 0;
                        ImpuestoConsumo=(float) 0;
                        porciva="0";
                        grupo="";
                       detail=details.getJSONObject("data").getJSONArray("detalles").getJSONObject(j);
                       if(BuscarMaterial(detail.getString("descripcion"),detail.getString("plu"))){
                        matidString=ConsultarMaterial(detail.getString("descripcion"),detail.getString("plu"));
                        detailIva=detail.getJSONObject("iva");
                        precioBaseFloat=detail.getFloat("venta_bruta");
                        precioVenta=detail.getFloat("venta_neta")/detail.getFloat("cantidad");
                        PrecioIva=detailIva.getFloat("impuesto_valor");
                        if (PrecioIva>0){
                            porciva="19";
                            grupo=ConsultarGrupo(detail.getString("descripcion"),detail.getString("plu"));
                            if(grupo.contains("CERVEZAS")||grupo.contains("GASEOSA")||grupo.contains("CIGARRILLOS")){
                                precioBaseFloat=(PrecioIva*100)/19;
                                ImpuestoConsumo=detail.getFloat("venta_bruta")-(PrecioIva+precioBaseFloat);                                
                            }                            
                        }
                        sqlString="insert into dekardex(kardexid,matid,bodid,prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta,preciobase"+
                                    ",precioiva,precioneto,parcvta,preciotasa,parctasa,precioiconsumo)"+
                                    "values('"+kardexidString+"'"+
                                    ",'"+matidString+"'"+
                                    ",'"+ConsultarBodega("03")+"'"+
                                    ",7,0,'D',"+porciva+",0"+
                                    ",'"+detail.get("cantidad").toString()+"'"+
                                    ",'"+detail.get("cantidad").toString()+"'"+
                                    ",'"+precioVenta+"'"+
                                    ",'"+precioVenta+"'"+
                                    ",'"+precioVenta+"'"+
                                    ",'"+PrecioIva+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+precioVenta+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+ImpuestoConsumo+"'"+
                                    ")";
                                    Tns.actualizar(sqlString);
                                    sqlString="UPDATE KARDEX SET netoiva=netoiva+"+PrecioIva+",VRIVA=VRIVA+"+PrecioIva+" where kardexid='"+kardexidString+"'";
                                    Tns.actualizar(sqlString);
                    }else{
                        GuardarLog("EL ARTICULO "+detail.getString("descripcion")+" NO EXISTE, EN LA FACTURA "+sale.getString("consecutivo"));
                    } 
                    }                                      
                   //System.out.println(i +" de "+obj.getJSONArray("data").length()+1); 
                }
            }
        }
    }
    public static String ConsultarBanco(String codigo) throws ClassNotFoundException, SQLException{
        String banco="1";
        sqlString="select bcoid from banco where codigo='"+codigo+"' ";
        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            banco=rs.getString("bcoid");
        }
        return banco;
    }    
    public static String ConsultarCentro(String codigo) throws ClassNotFoundException, SQLException{
        String banco="1";
        sqlString="select cenid from centros where nro='"+codigo+"' ";
        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            banco=rs.getString("cenid");
        }
        return banco;
    }    
    public static String ConsultarBodega(String codigo) throws ClassNotFoundException, SQLException{
        String bodegaString="1";
        sqlString="select bodid from bodega where codigo='"+codigo+"' ";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            bodegaString=rs.getString("bodid");
        }
        return bodegaString;
    }

    public static String ConsultarVenta(String codcomp,String numero,String prefijo) throws ClassNotFoundException, SQLException{
        String kardexidString="";
        sqlString="select kardexid from kardex where codcomp='"+codcomp+"' and codprefijo='"+prefijo+"' and numero='"+numero+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            kardexidString=rs.getString("kardexid");
        }
        return kardexidString;
    }
    public static Boolean BuscarMaterial(String productJsonObject,String codigo) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        sqlString="select matid from material where (descrip like '%"+productJsonObject+"%' or referencia ='"+codigo+"'  or codigo ='"+codigo+"' or referencia like '%"+productJsonObject+"%')";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            existe=true;
        }
        return existe;
    }
    public static String ConsultarMaterial(String productJsonObject,String codigo) throws ClassNotFoundException, SQLException{
        String matidString="";
        sqlString="select matid from material where (descrip like '%"+productJsonObject+"%' or referencia ='"+codigo+"'  or codigo ='"+codigo+"' or referencia like '%"+productJsonObject+"%')";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("matid");
        }
        return matidString;
    }

    public static String ConsultarGrupo(String productJsonObject,String codigo) throws ClassNotFoundException, SQLException{
        String matidString="";
        sqlString="select g.descrip from material m inner join grupmat g on g.grupmatid=m.grupmatid where (m.descrip like '%"+productJsonObject+"%' or m.referencia ='"+codigo+"'  or m.codigo ='"+codigo+"' or m.referencia like '%"+productJsonObject+"%')";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("descrip");
        }
        return matidString;
    }

    public static Boolean BuscarVenta(JSONObject ventaJsonObject) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        String prefijo="";
        String numero="";

        prefijo=sale.getString("consecutivo_prefijo");                        
        prefijo=BuscarPrefijo(prefijo);
        numero=sale.getString("consecutivo");            
        sqlString="select kardexid from kardex where codcomp='FV' and codprefijo='"+prefijo+"' and numero='"+numero+"'"; 

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
        String nombreString="";
        if(tipo.equals("cliente")){
            sqlString="select terid from terceros where (nit ='"+venta.getString("numero_documento_fe")+"' or nittri ='"+venta.getString("numero_documento_fe")+"' or nombre ='"+venta.getString("nombre_cliente")+"' )";
        }else{
            nombreString=ObtenerCampo(venta, "promotor");
            if(nombreString.split(" ").length==4){
                nombreString=nombreString.split(" ")[2]+" "+nombreString.split(" ")[3]+" "+nombreString.split(" ")[0]+" "+nombreString.split(" ")[1];
            }else{
                nombreString=nombreString.split(" ")[2]+" "+nombreString.split(" ")[0]+" "+nombreString.split(" ")[1];
            }
            sqlString="select terid from terceros where nombre ='"+nombreString+"' ";
        }
        
        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            teridString=rs.getString("terid");
        }
        if(teridString.isEmpty()){
            if(tipo.equals("cliente")){
                if (!venta.getString("numero_documento_fe").equals("0")){
                    sqlString="insert into terceros(nit,nittri,nombre,direcc1,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
                    "values('"+venta.getString("numero_documento_fe")+"','"+venta.getString("numero_documento_fe")+"'"+
                    ",'"+venta.getString("nombre_cliente")+"','SIN DIRECCION'"+
                    ",'SIN TELEFONO','','C','1','S','now',1,1)";
                    Tns.actualizar(sqlString);   
                    sqlString="select terid from terceros where (nit ='"+venta.getString("numero_documento_fe")+"' or nittri ='"+venta.getString("numero_documento_fe")+"' )"; 
                    rs =Tns.consultar(sqlString);
                    while(rs.next()){
                        teridString=rs.getString("terid");
                    }                    
                }else{
                    GuardarLog("LA FACTURA "+venta.getString("consecutivo_factura")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL ");
                    sqlString="select terid from terceros where (nit like '%2222222222%' or NOMBRE ='CONSUMIDOR FINAL' )";
                        rs =Tns.consultar(sqlString);    
                        if (rs.next()){
                            teridString=rs.getString("terid");
                        } 
                }           

            }else{
                    teridString="1";       
            }       
        }

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
        object.remove("user");
        object.remove("pass");
        object.remove("identificadorNegocio");
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
