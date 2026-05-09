package com.test.java.com.sergio.facelectronica;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import com.google.gson.JsonObject;
import com.sergio.prueba.ConnectionFirebird;

import okhttp3.*;

public class TerpelPosLibertadores {
    private static ConnectionFirebird Tns=null;
    private static String url="";
    private static String usuario="";
    private static String password="";    
    private static String token="";
    private static JSONObject obj = new JSONObject();
    private static JSONObject sale = new JSONObject();
    private static JSONObject creditos = new JSONObject();
    private static JSONArray productos = new JSONArray();
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
        String establecimiento=data.split("\\|")[5];
        String sucid=data.split("\\|")[6];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario();
        ConsultarPedidos(fecha,fechaFin,insertar,establecimiento,sucid);
        Tns.cerrarConexion();
        br.close();
        System.out.println("termine");
    }
    public static String BuscarPrefijo(String prefijo) throws ClassNotFoundException, SQLException{
        String prefijoString="";
        sqlString="select codprefijo from prefijo where (preimp='"+prefijo+"' OR CODPREFIJO='"+prefijo+"')";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            prefijoString=rs.getString("codprefijo");
        }
        if(prefijoString.equals("")){prefijoString=prefijo;}
        return prefijoString;
    }
    public static void ConsultarPedidos(String fechaString,String fechaFinString,String insertaString,String establecimiento,String sucid) throws IOException, ClassNotFoundException, JSONException, SQLException, InterruptedException{
    
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
    Float Cantidad=(float) 0;
    Integer Cant=0;
    String referencia="";
    String consecutivo="";
    JSONObject articulo= new JSONObject();
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
    ResultSet rsD=null;
    obj =new JSONObject(respuesta);
        if(insertaString.equals("N")){
            if(response.code()==200){
                System.out.println(obj.getJSONArray("data").length());
                sqlString="delete from varios where variab='CANTIDADTERPEL'";
                Tns.actualizar(sqlString);
                Cant=obj.getJSONArray("data").length();
                Cant=Cant+ConsultarCredito(fechaString, fechaFinString, establecimiento).getJSONArray("data").length();
                sqlString="insert into varios (contenido,variab) values('"+String.valueOf(Cant)+"','CANTIDADTERPEL')";  
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
                formapago="";
                referencia="";
                sale=obj.getJSONArray("data").getJSONObject(i);  
                teridString=ConsultarTerid(sale,"cliente");  
                vendedorIdString=ConsultarTerid(sale, "vendedor");
                observaciones=sale.getString("consecutivo_factura")+" "+sale.getString("placa");
                System.out.println(sale.getString("consecutivo_factura"));
                if (BuscarVenta(sale)) {
                    GuardarLog("LA FACTURA "+sale.getString("consecutivo_factura")+" YA EXISTE");
                } else { 
                    if(sale.getString("metodo_pago").equals("CREDITO CLIENTES")){
                        codcomp="RS";
                    }else{
                        codcomp="FV";
                    }                   
                    
                    if(sale.getString("consecutivo_factura").split("-").length > 1){ 
                        prefijo=sale.getString("consecutivo_factura").split("-")[0].trim(); 
                        numero=sale.getString("consecutivo_factura").split("-")[1].trim();                       
                        if (numero.contains("F")||numero.contains("F")){
                            prefijo=sale.getString("consecutivo_factura").split("-")[1].trim();
                            numero=sale.getString("consecutivo_factura").split("-")[0].trim();
                        }
                        prefijo=BuscarPrefijo(prefijo); 
                    } else{
                        numero=sale.get("consecutivo_factura").toString();
                        prefijo="00";
                    }
                    consecutivo=numero;
                    if(sale.getString("tipo_factura").equals("NC")){
                        codcomp="DV";
                        referencia=sale.getString("referencia").trim();
                        referencia=referencia.replaceAll(" ", "");
                        referencia=referencia.replaceAll("-", "");
                        referencia=referencia.substring(2);                        
                    }           
                                          
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
                    formapago="CO";  
                    bancoId="1";
                    if (sucid.equals("1")){
                        if(sale.getString("metodo_pago").contains("EFECTIVO")){
                            bancoId="1";
                        }else{
                            if(sale.getString("metodo_pago").contains("TARJET")){                                
                                formapago="MU";  
                            }else{
                                bancoId="1";
                            }                            
                        }
                    }else{
                        bancoId="1";
                    }                                      
                    if(sale.getString("metodo_pago").contains(",")){
                        formapago="CO";
                        bancoId="1";
                    }
                
                    fechaVentaString=fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);
                    sqlString="insert into kardex(codcomp,codprefijo,numero,nrofactven,observ,fecha,periodo,cenid,areadid,sucid,cliente,vendedor,formapago,bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto,netobase,hora,fecha_terpel)"+
                              "values('"+codcomp+"'"+
                              ",'"+prefijo+"'"+
                              ",'"+consecutivo+"'"+
                              ",'"+referencia+"'"+
                              ",'"+observaciones+"'"+
                              ",'"+fechaVentaString+"'"+
                              ",'"+fechaVentaString.substring(0, 2)+"'"+
                              ",1"+
                              ",1"+
                              ","+sucid+
                              ",'"+teridString+"'"+
                              ",'"+vendedorIdString+"'"+
                              ",'"+formapago+"'"+
                              ","+bancoId+""+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",'"+sale.get("valor_venta_total").toString().replaceAll("-","")+"'"+
                              ",0"+
                              ",'"+sale.get("valor_venta_total").toString().replaceAll("-","")+"'"+
                              ",'"+sale.get("valor_venta_total").toString().replaceAll("-","")+"'"+
                              ",0"+
                              ",'"+teridString+"'"+
                              ",1"+
                              ",'"+sale.get("valor_venta_total").toString().replaceAll("-","")+"'"+
                              ",'"+sale.get("valor_venta_total").toString().replaceAll("-","")+"'"+
                              ",'"+sale.get("valor_venta_total").toString().replaceAll("-","")+"'"+
                              ",'"+hora+"'"+
                              ",'"+fechaString.substring(5,7)+"/"+fechaString.substring(8,10)+"/"+fechaString.substring(0,4)+"'"+
                              ")";
                        Tns.actualizar(sqlString);
                        kardexidString=ConsultarVenta(codcomp,consecutivo,prefijo);
                        Tns.actualizar("insert into terpel(numero)values('"+observaciones+"')");
                        if(formapago.equals("MU")){
                            InsertarFormapago(sale,kardexidString,teridString);
                        }
                       // InsertarFormapago(sale,kardexidString,teridString);
                        if(BuscarMaterial(sale,sucid)){
                            matidString=ConsultarMaterial(sale,sucid);
                            Cantidad=(float) 0;
                            precioBaseFloat=sale.getFloat("precio");
                            if (precioBaseFloat<0) {
                                precioBaseFloat=precioBaseFloat*-1;
                            }
                            Cantidad=sale.getFloat("valor_venta_total")/sale.getFloat("precio");
                            if(Cantidad<0){
                                Cantidad=Cantidad*-1;
                            }
                            sqlString="insert into dekardex(kardexid,matid,bodid,prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta,preciobase"+
                                        ",precioiva,precioneto,parcvta,preciotasa,parctasa)"+
                                        "values('"+kardexidString+"'"+
                                        ",'"+matidString+"'"+
                                        ",1"+
                                        ",7,0,'D',0,0"+
                                        ",'"+String.valueOf(Cantidad)+"'"+
                                        ",'"+String.valueOf(Cantidad)+"'"+
                                        ",'"+precioBaseFloat+"'"+
                                        ",'"+precioBaseFloat+"'"+
                                        ",'"+precioBaseFloat+"'"+
                                        ",0"+
                                        ",'"+precioBaseFloat+"'"+
                                        ",'"+sale.get("valor_venta_total").toString().replaceAll("-","")+"'"+
                                        ",'"+precioBaseFloat+"'"+
                                        ",'"+sale.get("valor_venta_total").toString().replaceAll("-","")+"'"+
                                        ")";
                                        Tns.actualizar(sqlString);
                        }else{
                            GuardarLog("EL ARTICULO "+sale.getString("producto")+" NO EXISTE, EN LA FACTURA "+sale.getString("consecutivo_factura"));
                        }                         
                    
                                       
                   
                   //System.out.println(i +" de "+obj.getJSONArray("data").length()+1); 
                      
                }
                
                Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
            }
            creditos=ConsultarCredito(fechaString, fechaFinString, establecimiento);
            for (int j = 0; j < creditos.getJSONArray("data").length(); j++) {
                teridString=""; 
                fechaVentaString="";
                vendedorIdString="";
                kardexidString="";
                codcomp="";
                observaciones="";
                formapago="";
                referencia="";
                sale=creditos.getJSONArray("data").getJSONObject(j);  
                System.out.println(sale.getString("consecutivo")); 
                numero=sale.getString("consecutivo");
                prefijo="00";
                codcomp="RS";   
                if(BuscarRemision(codcomp,prefijo,numero).equals(true)){
                   // GuardarLog("la remision "+prefijo+numero+" ya existe");
                    teridString=ConsultarTerceroRemision(sale);
                    sqlString="update kardex set cliente='"+teridString+"',despachar_a='"+teridString+"',formapago='CR',plazodias='1',fecvence=fecha where kardexid='"+ConsultaRemision(codcomp, prefijo, numero)+"'";
                    Tns.actualizar(sqlString);
                }else{
                    teridString=ConsultarTerceroRemision(sale);
                    vendedorIdString=ConsultarTerid(sale, "promotor");
                    observaciones=sale.getString("consecutivo")+" "+sale.getString("vehiculo_placa");
                    fechaVentaString=sale.getString("fecha").split(" ")[0];
                    formapago="CR";  
                    bancoId="1";
                    fechaVentaString=fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);
                    sqlString="insert into kardex(codcomp,codprefijo,numero,nrofactven,observ,fecha,periodo,cenid,areadid,sucid,cliente,vendedor,formapago,bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto,netobase,hora,fecha_terpel)"+
                              "values('"+codcomp+"'"+
                              ",'"+prefijo+"'"+
                              ",'"+numero+"'"+
                              ",'"+referencia+"'"+
                              ",'"+observaciones+"'"+
                              ",'"+fechaVentaString+"'"+
                              ",'"+fechaVentaString.substring(0, 2)+"'"+
                              ",1"+
                              ",1"+
                              ",1"+
                              ",'"+teridString+"'"+
                              ",'"+vendedorIdString+"'"+
                              ",'"+formapago+"'"+
                              ","+bancoId+""+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",'"+sale.get("totalventa").toString().replaceAll("-","")+"'"+
                              ",0"+
                              ",'"+sale.get("totalventa").toString().replaceAll("-","")+"'"+
                              ",'"+sale.get("totalventa").toString().replaceAll("-","")+"'"+
                              ",0"+
                              ",'"+teridString+"'"+
                              ",1"+
                              ",'"+sale.get("totalventa").toString().replaceAll("-","")+"'"+
                              ",'"+sale.get("totalventa").toString().replaceAll("-","")+"'"+
                              ",'"+sale.get("totalventa").toString().replaceAll("-","")+"'"+
                              ",'"+hora+"'"+
                              ",'"+fechaString.substring(5,7)+"/"+fechaString.substring(8,10)+"/"+fechaString.substring(0,4)+"'"+
                              ")";
                              Tns.actualizar(sqlString);
                              kardexidString=ConsultarVenta(codcomp,numero,prefijo);
                              for (int i = 0; i < sale.getJSONArray("producto").length(); i++) {
                                articulo=sale.getJSONArray("producto").getJSONObject(i);
                                if(BuscarMaterialCredito(articulo,"1")){
                                    matidString=ConsultarMaterialCredito(articulo,"1");
                                    Cantidad=(float) 0;
                                    precioBaseFloat=articulo.getFloat("precio");
                                    if (precioBaseFloat<0) {
                                        precioBaseFloat=precioBaseFloat*-1;
                                    }
                                    Cantidad=sale.getFloat("totalventa")/articulo.getFloat("precio");
                                    if(Cantidad<0){
                                        Cantidad=Cantidad*-1;
                                    }
                                    sqlString="insert into dekardex(kardexid,matid,bodid,prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta,preciobase"+
                                                ",precioiva,precioneto,parcvta,preciotasa,parctasa)"+
                                                "values('"+kardexidString+"'"+
                                                ",'"+matidString+"'"+
                                                ",1"+
                                                ",7,0,'D',0,0"+
                                                ",'"+String.valueOf(Cantidad)+"'"+
                                                ",'"+String.valueOf(Cantidad)+"'"+
                                                ",'"+precioBaseFloat+"'"+
                                                ",'"+precioBaseFloat+"'"+
                                                ",'"+precioBaseFloat+"'"+
                                                ",0"+
                                                ",'"+precioBaseFloat+"'"+
                                                ",'"+sale.get("totalventa").toString().replaceAll("-","")+"'"+
                                                ",'"+precioBaseFloat+"'"+
                                                ",'"+sale.get("totalventa").toString().replaceAll("-","")+"'"+
                                                ")";
                                                Tns.actualizar(sqlString);
                                }else{
                                    GuardarLog("EL ARTICULO "+sale.getString("producto")+" NO EXISTE, EN LA FACTURA "+sale.getString("consecutivo_factura"));
                                } 
                              }
                }
                ActualizarCantidad();
            }
        }
    }

    public static String ConsultarMaterialCredito(JSONObject productJsonObject,String sucid) throws ClassNotFoundException, SQLException{
        String matidString="";
        String nombre="";
        if (productJsonObject.getString("descripcion").split(" ")[0].contains("BIO")){
            nombre=productJsonObject.getString("descripcion").split(" ")[0];
        }else{
            nombre=productJsonObject.getString("descripcion").split(" ")[0]+" "+productJsonObject.getString("descripcion").split(" ")[1];
        }
        
        if(sucid.equals("1")){
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>0 and m.descrip like '%"+nombre+"%'";
        }else{
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>0 and m.descrip like '%"+nombre+"%' and m.codigo like 'V%' ";
        }
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("matid");
        }
        return matidString;
    }
    public static Boolean BuscarMaterialCredito(JSONObject productJsonObject,String sucid) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        String nombre="";
        
        if (productJsonObject.getString("descripcion").split(" ")[0].contains("BIO")){
            nombre=productJsonObject.getString("descripcion").split(" ")[0];
        }else{
            nombre=productJsonObject.getString("descripcion").split(" ")[0]+" "+productJsonObject.getString("descripcion").split(" ")[1];
        }
        if(sucid.equals("1")){
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>0 and m.descrip like '%"+nombre+"%'";
        }else{
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>0 and m.descrip like '%"+nombre+"%' and m.codigo like 'V%' ";
        }
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            existe=true;
        }
        return existe;
    }
    public static Boolean BuscarRemision(String codcomp,String codprefijo,String numero) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        
        sqlString="select kardexid from kardex where observ like '"+numero+"%'"; 

        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            existe=true;
        }
        return existe;
    }
    public static void ActualizarCantidad() throws ClassNotFoundException, SQLException{
        sqlString="select contenido from varios where variab ='CANTIDADTERPELSUBIDA'";
        ResultSet rs =Tns.consultar(sqlString);
        while (rs.next()){
            sqlString="UPDATE VARIOS SET CONTENIDO =(select * from TNS_SP_CONSECUTIVO('"+rs.getString("contenido")+"')) WHERE variab LIKE '%CANTIDADTERPELSUBIDA%'";
        }
        Tns.actualizar(sqlString);
    }
    public static String ConsultarTerceroRemision(JSONObject venta) throws ClassNotFoundException, SQLException{
        String teridString="1";
        String nombre=venta.getString("cliente");
        String and="or ";
        String[] partes = nombre.split(" ");
        for (int i = 0; i < partes.length; i++) {
            if (i == 0) {
                and = " (nombre CONTAINING '"+partes[i]+"'";
            }else{
                and = and + " and nombre CONTAINING '"+partes[i]+"'";
            }
        }
        and = and +")";
        sqlString="select terid from terceros where establecimiento ='"+venta.getString("cliente")+"' or nit like '%"+venta.getString("identificacion_cliente")+"%' ";
        System.out.println(sqlString);
        ResultSet rs = Tns.consultar(sqlString);
        while(rs.next()){
            teridString=rs.getString("terid");
        }
        return teridString;
    }
    public static String ConsultaRemision(String codcomp,String codprefijo,String numero) throws ClassNotFoundException, SQLException{
        String kardexid="";
        
        sqlString="select kardexid from kardex where observ like '"+numero+"%'"; 

        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            kardexid=rs.getString("kardexid");
        }
        return kardexid;
    }  
    public static JSONObject ConsultarCredito(String fechaString,String fechaFinString,String establecimiento) throws IOException, InterruptedException{
        JsonObject establecimientoJsonObject = new JsonObject();
        String terceros="";
        establecimientoJsonObject.addProperty("identificadorEstacion",establecimiento);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(establecimientoJsonObject);
        MediaType mediaType = MediaType.parse("application/json");
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        RequestBody body = RequestBody.create(mediaType, json);
        client=builder.build();              
        Request request2 = new Request.Builder().url(url+":7001/api/cliente/select").method("POST", body) 
        .addHeader("Authorization", "Bearer "+token)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json").build();
        Response response2 = client.newCall(request2).execute();
        String respuesta2=response2.body().string();
        obj =new JSONObject(respuesta2);
        for (int i = 0; i < obj.getJSONArray("data").length(); i++) {
            if(terceros.equals("")){
                terceros=obj.getJSONArray("data").getJSONObject(i).get("value").toString();
            }else{
                terceros=terceros+","+obj.getJSONArray("data").getJSONObject(i).get("value").toString();
            }
        }
        establecimientoJsonObject.remove("identificadorEstacion");
        establecimientoJsonObject.remove("identificadorEstacion");
        establecimientoJsonObject.addProperty("fecha_inicial", fechaString);
        establecimientoJsonObject.addProperty("fecha_final", fechaFinString);
        establecimientoJsonObject.addProperty("identificadorEstacion", Integer.parseInt(establecimiento));
        establecimientoJsonObject.addProperty("identificadorCliente", terceros);
        establecimientoJsonObject.addProperty("extra", "AMBAS");    
        gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(establecimientoJsonObject);    
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url+":7008/api/reporteria/clientesFlotas"))
            .header("Accept", "*/*")    
            .header("Authorization", "Bearer "+token)
            .header("Content-Type", "application/json")
            .method("POST", HttpRequest.BodyPublishers.ofString(json))
            .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
           // System.out.println(response.body());
            String respuesta=response.body().toString();
            obj =new JSONObject(respuesta);            
         return obj;
    }
    public static void InsertarFormapago(JSONObject sale,String kardexidString,String terid) throws ClassNotFoundException, SQLException{
        String[] formasPago=sale.getString("metodo_pago").split(",");
        ResultSet rs = null;
        String formapagoId="";
        Float valor=(float) 0;
        for (int i = 0; i < 1; i++) {
            rs=Tns.consultar("select formapagoid from formapago where descrip='"+formasPago[i]+"'");
            while (rs.next()){
                formapagoId=rs.getString("formapagoid");
            }
            valor=sale.getFloat("valor_venta_total");
            sqlString="insert into dekardexfp(kardexid,formapagoid,valor,terid)"+
                      "values('"+kardexidString+"','"+formapagoId+"','"+valor.toString()+"','"+terid+"')";
                      Tns.actualizar(sqlString);
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

    public static String ConsultarVenta(String codcomp,String numero,String prefijo) throws ClassNotFoundException, SQLException{
        String kardexidString="";
        sqlString="select kardexid from kardex where codcomp='"+codcomp+"' and codprefijo='"+prefijo+"' and numero='"+numero+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            kardexidString=rs.getString("kardexid");
        }
        return kardexidString;
    }
    public static Boolean BuscarMaterial(JSONObject productJsonObject,String sucid) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        String nombre="";
        if (productJsonObject.getString("producto").split(" ")[0].contains("BIO")){
            nombre=productJsonObject.getString("producto").split(" ")[0];
        }else{
            nombre=productJsonObject.getString("producto").split(" ")[0]+" "+productJsonObject.getString("producto").split(" ")[1];
        }
        if(sucid.equals("1")){
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.precio1>0 and m.descrip like '%"+nombre+"%'";
        }else{
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.precio1>0 and m.descrip like '%"+nombre+"%' and m.codigo like 'V%' ";
        }
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            existe=true;
        }
        return existe;
    }
    public static String ConsultarMaterial(JSONObject productJsonObject,String sucid) throws ClassNotFoundException, SQLException{
        String matidString="";
        String nombre="";
        if (productJsonObject.getString("producto").split(" ")[0].contains("BIO")){
            nombre=productJsonObject.getString("producto").split(" ")[0];
        }else{
            nombre=productJsonObject.getString("producto").split(" ")[0]+" "+productJsonObject.getString("producto").split(" ")[1];
        }
        
        if(sucid.equals("1")){
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.precio1>0 and m.descrip like '%"+nombre+"%'";
        }else{
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.precio1>0 and m.descrip like '%"+nombre+"%' and m.codigo like 'V%' ";
        }
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("matid");
        }
        return matidString;
    }
    public static Boolean BuscarVenta(JSONObject ventaJsonObject) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        String prefijo="";
        String numero="";  
        if(sale.getString("consecutivo_factura").split("-").length > 1){ 
            prefijo=sale.getString("consecutivo_factura").split("-")[0].trim(); 
            numero=sale.getString("consecutivo_factura").split("-")[1].trim();                       
            if (numero.contains("F")||numero.contains("F")){
                prefijo=sale.getString("consecutivo_factura").split("-")[1].trim();
                numero=sale.getString("consecutivo_factura").split("-")[0].trim();
            }
            prefijo=BuscarPrefijo(prefijo); 
        } else{
            numero=sale.get("consecutivo_factura").toString();
            prefijo="00";
        }
                            
               

        sqlString="select * from kardex where numero = '"+numero+"' and codprefijo='"+prefijo+"'"; 

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
        String nombre="";
        if(tipo.equals("cliente")){
            if (venta.getString("numero_documento_fe").equals("0")){
                sqlString="select terid from terceros where nombre ='%"+venta.getString("nombre_cliente").replaceAll(" ", "%")+"%'";
            }else{
                sqlString="select terid from terceros where (nit like '%"+venta.getString("numero_documento_fe")+"%' or nittri like '%"+venta.getString("numero_documento_fe")+"' or nombre ='"+venta.getString("nombre_cliente")+"%' )";
            }
        }else{
            nombre=ObtenerCampo(venta, "promotor");
            if (nombre.split(" ").length==4){
                nombre=nombre.split(" ")[2]+" "+nombre.split(" ")[3]+" "+nombre.split(" ")[0]+" "+nombre.split(" ")[1];
            }else{
                nombre=nombre.split(" ")[1]+" "+nombre.split(" ")[2]+" "+nombre.split(" ")[0];
            }
            sqlString="select terid from terceros where nombre ='"+nombre+"' ";
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
                    sqlString="select terid from terceros where (nit ='22222222222' or NOMBRE ='CONSUMIDOR FINAL' )";
                        rs =Tns.consultar(sqlString);    
                        if (rs.next()){
                            teridString=rs.getString("terid");
                        } 
                }           

            }else{
                GuardarLog("No se encontro el vendedor "+ObtenerCampo(venta, "promotor"));
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
