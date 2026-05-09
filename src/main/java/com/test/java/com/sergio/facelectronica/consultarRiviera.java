package com.test.java.com.sergio.facelectronica;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

public class consultarRiviera {
 private static ConnectionFirebird Tns=null;
    private static String url="";
    private static String usuario="";
    private static String password="";    
    private static String token="";
    private static String tokenG="";
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
        File tempo = new File("c:\\tempo\\terpelRiviera.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String ruta=data.split("\\|")[0];
        String ip=data.split("\\|")[1];
        String fecha=data.split("\\|")[2];
        String fechaFin=data.split("\\|")[3];
        String insertar=data.split("\\|")[4];
        String reloj="N";
          try {
            reloj=data.split("\\|")[5];
          } catch (Exception e) {
            // TODO: handle exception
          }
        System.out.println("reloj activo: "+reloj);
        File tempo2 = new File("c:\\visual tns\\target\\token.txt");
        FileReader fr2 = new FileReader(tempo2);
        BufferedReader br2 = new BufferedReader(fr2);
        String data2 = br2.readLine();
        String tokenF=data2.split("\\|")[0];
        String laravelSession=data2.split("\\|")[1];

        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario();
        ConsultarPedidos(fecha,fechaFin,insertar,reloj,tokenF,laravelSession);
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
    public static String ConsultarFecha() throws ClassNotFoundException, SQLException{
        String fecha="";
        sqlString="select contenido from varios where variab='UTLFECHASIN'";
        ResultSet rs = Tns.consultar(sqlString);
        if (rs.next()){
            fecha=rs.getString("contenido");
        }
        return fecha;
    }
    public static void ConsultarPedidos(String fechaString,String fechaFinString,String insertaString,String reloj,String tokenF,String laravelSession) throws IOException, ClassNotFoundException, JSONException, SQLException{
    String teridString="";
    String vendedorIdString="";
    String fechaVentaString="";
    String kardexidString="";
    String matidString="";
    String codcomp="";
    String prefijo="";
    String numero="";    
    String observaciones="";
    String consecutivo="";
    String hora="";
    String banco="";
    Float precioBaseFloat=(float) 0;
    Float cantidad=(float) 0;
    String fechaConsulta=new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
    String fechaInicial=ConsultarFecha();
    //fechaInicial="2025-02-10";
    String fechaUltConsulta=new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
    System.out.println(fechaUltConsulta);
    Tns.actualizar("update varios set contenido='"+fechaUltConsulta+"' where variab='UTLFECHASIN'");
    ConsultarToken();    
    OkHttpClient client = new OkHttpClient().newBuilder()
    .build();
    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
    @SuppressWarnings("deprecation")
  // RequestBody body = RequestBody.create(mediaType, "data[agreements]=&data[bill_receipt_number]=&data[employees]=&data[final_date]="+fechaString+" 23:59:59&data[integrations]=&data[is_shop]=0&data[payments_type]=&data[pccs]=&data[people]=&data[person]=&data[plate]=&data[print]=0&data[products]=&data[shift_id]=&data[start_date]="+fechaString+" 00:00:00&data[total]=&data[wildcard]=");
    RequestBody body = RequestBody.create(mediaType, "data[agreements]=&data[bill_receipt_number]=&data[employees]=&data[final_date]="+fechaConsulta+" 23:59:59&data[integrations]=&data[is_shop]=0&data[payments_type]=&data[pccs]=&data[people]=&data[person]=&data[plate]=&data[print]=0&data[products]=&data[shift_id]=&data[start_date]="+fechaConsulta+" 00:00:00&data[total]=&data[wildcard]=");
    Request request = new Request.Builder()
    .url("https://dominus.iapropiada.com/administration/operation/shifts/datasales")
    .method("POST", body)
    .addHeader("sec-ch-ua-platform", "\"Windows\"")
    .addHeader("Authorization", "_token=\""+tokenF+"\"")
    .addHeader("sec-ch-ua", "\"Not A(Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Google Chrome\";v=\"132\"")
    .addHeader("sec-ch-ua-mobile", "?0")
    .addHeader("X-Requested-With", "XMLHttpRequest")
    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36")
    .addHeader("Accept", "*/*")
    .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    .addHeader("Sec-Fetch-Site", "same-origin")
    .addHeader("Sec-Fetch-Mode", "cors")
    .addHeader("Sec-Fetch-Dest", "empty")
    .addHeader("host", "dominus.iapropiada.com")
    .addHeader("Cookie", "laravel_session="+laravelSession)   .build();
    Response response = client.newCall(request).execute();
   // System.out.println(response.code());
   // System.out.println(response.body().string());
   String respuesta=response.body().string();
   ResultSet rsD=null;
    obj =new JSONObject(respuesta);
         if(insertaString.equals("N")){
            if(response.code()==200){
               // System.out.println(obj.getJSONArray("data").length());
                sqlString="delete from varios where variab='CANTIDADTERPEL'";
                Tns.actualizar(sqlString);
                System.out.println("CANTIDAD A SUBIR "+obj.getJSONObject("data").getJSONArray("sales").length());
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
                codcomp="";
                observaciones="";
                sale=obj.getJSONObject("data").getJSONArray("sales").getJSONObject(i);  
                teridString=ConsultarTerid(sale,"cliente");  
                vendedorIdString=ConsultarTerid(sale, "vendedor");
                observaciones=sale.get("receipt").toString()+" "+sale.getString("plate");
              
                if (BuscarVenta(sale)) {
                    GuardarLog("LA FACTURA "+sale.get("receipt").toString()+" YA EXISTE");
                    Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
                } else {
                    if (sale.get("acctagreement").toString().equals("CONTADO")) {
                        prefijo="FE";
                        codcomp="FV";
                    }else{
                        prefijo="00";
                        codcomp="RS";
                    }
                    if (reloj.equals("S")){
                        prefijo="XX";
                        codcomp="FV";
                    }

                    if(!sale.getString("client").contains("CONSUMIDOR")){
                        prefijo="FE";
                        codcomp="FV";                        
                    }                    
                    if(sale.getString("wildcard").length()>6){
                        prefijo="FE";
                        codcomp="FV";   
                    }
                    
                    banco=ConsultarBanco(sale.get("odometer").toString());  
                    if(!banco.equals("1")){
                        prefijo="FE";
                        codcomp="FV";      
                    }
                    if (sale.get("acctagreement").toString().equals("CREDITO")) {
                        codcomp="RS";
                        prefijo="00";
                    }
                    sqlString="select CONSECUTIVO from CONSECUTIVO where codcomp='"+codcomp+"' and codprefijo='"+prefijo+"'";
                    rsD=Tns.consultar(sqlString);
                    if (rsD.next()){
                        consecutivo=rsD.getString("CONSECUTIVO");
                        Integer numEntero = Integer.parseInt(consecutivo);
                        numEntero=numEntero+1;    
                        consecutivo=numEntero.toString();
                    }else{
                        consecutivo="1";
                    }                               
                    fechaVentaString=sale.getString("date_sale").split(" ")[0]; 
                    hora=sale.getString("date_sale").split(" ")[1];
                    hora=hora.substring(1,5);        
                    //fechaVentaString=fechaString;
                    fechaVentaString=fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);
                    System.out.println(prefijo+"-"+numero);
                    sqlString="insert into kardex(codcomp,codprefijo,numero,observ,fecha,periodo,cenid,areadid,sucid,cliente,vendedor,formapago,bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto,netobase,hora)"+
                              "values('"+codcomp+"'"+
                              ",'"+prefijo+"'"+
                              ",'"+consecutivo+"'"+
                              ",'"+observaciones+"'"+
                              ",'"+fechaVentaString+"'"+
                              ",'"+fechaVentaString.substring(0, 2)+"'"+
                              ",1"+
                              ",1"+
                              ",1"+
                              ",'"+teridString+"'"+
                              ",'"+vendedorIdString+"'"+
                              ",'CO'"+
                              ",'"+banco+"'"+
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
                              ",'"+hora+"'"+
                              ")";
                    Tns.actualizar(sqlString);
                    kardexidString=ConsultarVenta(codcomp,consecutivo,prefijo);
                    if(BuscarMaterial(sale,obj)){
                        matidString=ConsultarMaterial(sale);
                        
                        precioBaseFloat=obj.getJSONObject("data").getJSONObject("productrefs_sale").getJSONArray(sale.get("id").toString()).getJSONObject(0).getFloat("price");
                        cantidad=obj.getJSONObject("data").getJSONObject("productrefs_sale").getJSONArray(sale.get("id").toString()).getJSONObject(0).getFloat("total")/obj.getJSONObject("data").getJSONObject("productrefs_sale").getJSONArray(sale.get("id").toString()).getJSONObject(0).getFloat("price");
                        
                        sqlString="insert into dekardex(kardexid,matid,bodid,prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta,preciobase"+
                                    ",precioiva,precioneto,parcvta,preciotasa,parctasa)"+
                                    "values('"+kardexidString+"'"+
                                    ",'"+matidString+"'"+
                                    ",1"+
                                    ",7,0,'D',0,0"+
                                    ",'"+String.valueOf(cantidad)+"'"+
                                    ",'"+String.valueOf(cantidad)+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",0"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+sale.get("total").toString()+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+sale.get("total").toString()+"'"+
                                    ")";
                                    Tns.actualizar(sqlString);
                    }else{
                        GuardarLog("EL ARTICULO "+obj.getJSONObject("data").getJSONObject("productrefs_sale").getJSONArray(sale.get("id").toString()).getJSONObject(0).getString("product")+" NO EXISTE, EN LA FACTURA "+sale.get("receipt").toString());
                    }     
                    Tns.actualizar("update consecutivo set consecutivo='"+consecutivo+"' where codcomp='"+codcomp+"' and codprefijo='"+prefijo+"'");                                  
                   Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
                   //System.out.println(i +" de "+obj.getJSONArray("data").length()+1); 
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

    public static String ConsultarBanco(String codigo) throws ClassNotFoundException, SQLException{
        String banco="1";
        sqlString="select bcoid from banco where codigo='"+codigo+"' ";
        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            banco=rs.getString("bcoid");
        }
        return banco;
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
    public static Boolean BuscarMaterial(JSONObject productJsonObject,JSONObject obj) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        String nombre="";
        nombre=obj.getJSONObject("data").getJSONObject("productrefs_sale").getJSONArray(productJsonObject.get("id").toString()).getJSONObject(0).getString("product");

        sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>1 and m.descrip like '"+nombre+"%' ";

        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            existe=true;
        }
        return existe;
    }
    public static String ConsultarMaterial(JSONObject productJsonObject) throws ClassNotFoundException, SQLException{
        String matidString="";
        String nombre="";
        nombre=obj.getJSONObject("data").getJSONObject("productrefs_sale").getJSONArray(productJsonObject.get("id").toString()).getJSONObject(0).getString("product");
        sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>1 and m.descrip like '"+nombre+"%'";        
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("matid");
        }
        return matidString;
    }
    public static Boolean BuscarVenta(JSONObject ventaJsonObject) throws ClassNotFoundException, SQLException{
        String fechaVentaString=sale.getString("date_sale").split(" ")[0];
        fechaVentaString=fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);
        Boolean existe=false;
        sqlString="select kardexid from kardex where fecha='"+fechaVentaString+"' and observ like '%"+ventaJsonObject.get("receipt").toString()+"%'"; 
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
        JSONObject tercero = new JSONObject();
        if(tipo.equals("cliente")){
            System.out.println(venta.get("receipt").toString()+" - "+venta.getString("wildcard"));
            if (venta.getString("wildcard").equals("")||venta.getString("wildcard").length()<6){
                sqlString="select terid from terceros where (nit like '%"+venta.getString("nit").substring(0, venta.getString("nit").length()-1)+"%' or nittri like '%"+venta.getString("nit").substring(0, venta.getString("nit").length()-1)+"%' or nombre ='"+venta.getString("client")+"' )";
            }else{
                sqlString="select terid from terceros where (nit like '%"+venta.getString("wildcard")+"%' or nittri like '%"+venta.getString("wildcard")+"%')";
            }            
        }else{
            sqlString="select terid from terceros where (nombre like '%"+ObtenerCampo(venta, "employee")+"%' or nit like '%"+venta.getString("employee_document")+"%') ";
        }
        
        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            teridString=rs.getString("terid");
        }
        if(teridString.isEmpty()){
            if(tipo.equals("cliente")){
                if (!venta.getString("nit").equals("0")){
                    if(venta.getString("wildcard").equals("")||venta.getString("wildcard").length()<6){
                        sqlString="insert into terceros(nit,nittri,nombre,direcc1,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
                        "values('"+venta.getString("nit").substring(0, venta.getString("nit").length()-1)+"','"+venta.getString("nit").substring(0, venta.getString("nit").length()-1)+"'"+
                        ",'"+venta.getString("client")+"','SIN DIRECCION'"+
                        ",'SIN TELEFONO','','C','1','S','now',1,1)";
                        Tns.actualizar(sqlString);  
                    }else{
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                        .url("https://eds.com.co/api/thirds/?identification="+venta.getString("wildcard")+"&type=CC")
                        .get()
                        .addHeader("Accept", "*/*")
                        .addHeader("User-Agent", "Thunder Client (https://www.thunderclient.com)")
                        .build();
                        try {
                            Response response = client.newCall(request).execute();
                            String respuesta=response.body().string();                            
                            tercero =new JSONObject(respuesta);
                            if(tercero.getInt("count")>0){
                                String nombre="";
                                if(tercero.getJSONArray("results").getJSONObject(0).getString("type").equals("NIT")){
                                    nombre=tercero.getJSONArray("results").getJSONObject(0).getString("name");
                                }else{
                                    nombre=    tercero.getJSONArray("results").getJSONObject(0).getString("last_name")+" "+ObtenerCampo(tercero.getJSONArray("results").getJSONObject(0), "second_last_name")+" "+tercero.getJSONArray("results").getJSONObject(0).getString("name")+" "+ObtenerCampo(tercero.getJSONArray("results").getJSONObject(0), "second_name");                             
                                }
                                sqlString="insert into terceros(nit,nittri,nombre,direcc1,email,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
                                        "values('"+tercero.getJSONArray("results").getJSONObject(0).getString("identification")+"','"+tercero.getJSONArray("results").getJSONObject(0).getString("identification")+"'"+
                                        ",'"+nombre+"','"+tercero.getJSONArray("results").getJSONObject(0).getString("address")+"','"+tercero.getJSONArray("results").getJSONObject(0).getString("email")+"'"+
                                        ",'SIN TELEFONO','','C','1','S','now',1,1)";
                                        Tns.actualizar(sqlString);  
                            }
                            
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    //System.out.println(venta.getString("nit").substring(0, venta.getString("nit").length()-1));
                     
                    if (venta.getString("wildcard").equals("")||venta.getString("wildcard").length()<6){
                        sqlString="select terid from terceros where (nit like '%"+venta.getString("nit").substring(0, venta.getString("nit").length()-1)+"%' or nittri like '%"+venta.getString("nit").substring(0, venta.getString("nit").length()-1)+"%' or nombre ='"+venta.getString("client")+"' )";
                    }else{
                        sqlString="select terid from terceros where (nit like '%"+venta.getString("wildcard")+"%' or nittri like '%"+venta.getString("wildcard")+"%')";
                    }       
                    rs =Tns.consultar(sqlString);
                    while(rs.next()){
                        teridString=rs.getString("terid");
                    }    
                    if(teridString.equals("")){
                        sqlString="select terid from terceros where (nit ='2222222222227' or NOMBRE ='CONSUMIDOR FINAL' )";
                        rs =Tns.consultar(sqlString);    
                        if (rs.next()){
                            teridString=rs.getString("terid");
                        } 
                    }                
                }else{
                    GuardarLog("LA FACTURA "+venta.getString("consecutivo_factura")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL ");
                    sqlString="select terid from terceros where (nit ='2222222222227' or NOMBRE ='CONSUMIDOR FINAL' )";
                        rs =Tns.consultar(sqlString);    
                        if (rs.next()){
                            teridString=rs.getString("terid");
                        } 
                }           

            }else{
                    sqlString="insert into terceros(nit,nittri,nombre,direcc1,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
                    "values('"+venta.getString("employee_document")+"','"+venta.getString("employee_document")+"'"+
                    ",'"+venta.getString("employee")+"','SIN DIRECCION'"+
                    ",'SIN TELEFONO','','C','1','S','now',1,1)";
                    Tns.actualizar(sqlString);   
                    sqlString="select terid from terceros where (nit ='"+venta.getString("employee_document")+"' or nittri ='"+venta.getString("employee_document")+"' )"; 
                    rs =Tns.consultar(sqlString);
                    while(rs.next()){
                        teridString=rs.getString("terid");
                    }                   
                
            }       
        }

        return teridString;
    }
    public static void GuardarLog(String observacionString) throws SQLException{
        Tns.actualizar("INSERT INTO LOGTERPEL(FECHA,OBSERVACIONES)values('now','"+observacionString+"')");
    } 
    public static void ConsultarToken()throws IOException, ClassNotFoundException, JSONException, SQLException{
        OkHttpClient client = new OkHttpClient().newBuilder()
        .build();
      MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
      RequestBody body = RequestBody.create(mediaType, "_token=slxZRqvIVbw3AO5qIVs0jRojmFLJqMigOqyYu3ma&cpt-email={\"iv\":\"04abc336699074195951bae84eb927df\",\"s\":\"6e583ee303922f95\",\"ct\":\"rVnchNmSucJxcOaVooKjUA==\"}&cpt-password={\"iv\":\"8c2561152563399a5b6e1ac47cd177cb\",\"s\":\"ec5e3303eb965e08\",\"ct\":\"uPYxu3ESQCzHVYB/6yK8yQ==\"}&email=&password=&recaptcha=");
      Request request = new Request.Builder()
        .url("https://dominus.iapropiada.com/users/postlogin")
        .method("POST", body)
        .addHeader("sec-ch-ua", "\"Not A(Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Microsoft Edge\";v=\"132\"")
        .addHeader("sec-ch-ua-mobile", "?0")
        .addHeader("sec-ch-ua-platform", "\"Windows\"")
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .addHeader("Upgrade-Insecure-Requests", "1")
        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
        .addHeader("Sec-Fetch-Site", "same-origin")
        .addHeader("Sec-Fetch-Mode", "navigate")
        .addHeader("Sec-Fetch-User", "?1")
        .addHeader("Sec-Fetch-Dest", "document")
        .addHeader("host", "dominus.iapropiada.com")
        .addHeader("Cookie", "laravel_session=eyJpdiI6IkN1RnpxZVJBNjBOajdGWXdwZDhXWUE9PSIsInZhbHVlIjoiXC8yUUgrZFlKakVBOVRJRkRmb1pFWlRqWWlxVVoxWmhPc0M2NjAyS1BKYmFQWVFMdVplV3VJSjhBODlcL1hLTWx3ejcyTThybk40S1wvNzk4dTZ3SzdCOUE9PSIsIm1hYyI6IjNjODk2MjNhZDI4YmFmYjQzZmViNDZlY2JjZTg4MDE2Nzk0MjcxZWNiM2I2MDZhZmM3ZmY0NjM4OTU0YzA1YzQifQ%3D%3D")
        .build();
    Response response = client.newCall(request).execute();
      ResponseBody responseBody = response.body();
      String htmlResponse = responseBody.string();

      // Imprimir el HTML de la respuesta
    //  System.out.println("HTML de respuesta:\n" + htmlResponse);
      String regex = "<meta name=\"_token\" content=\"([^\"]+)\">";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(htmlResponse);

        if (matcher.find()) {
            tokenG = matcher.group(1);
            System.out.println("Token extraído: " + tokenG);
        } else {
            System.out.println("No se encontró el token en el HTML.");
        }
        token= response.header("Set-Cookie");
     //   String respuesta=response.body().string();
        
       // System.out.println(token);
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
            //System.out.println(rs.getString("contenido"));
            if (rs.getString("variab").equals("URLTERPEL")) { url = rs.getString("contenido"); }
            if (rs.getString("variab").equals("USERTERPEL")) { usuario = rs.getString("contenido"); }
            if (rs.getString("variab").equals("SECRETOTERPEL")) { password = rs.getString("contenido"); }        
        }
        token=usuario+":"+password;
        token=DatatypeConverter.printBase64Binary(token.getBytes());
       // System.out.println(usuario+" "+password);
       // System.out.println(token);
    }
    
    
}
