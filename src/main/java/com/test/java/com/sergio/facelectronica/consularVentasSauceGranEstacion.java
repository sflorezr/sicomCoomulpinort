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
import java.util.concurrent.TimeUnit;

import okhttp3.*;
import okhttp3.OkHttpClient.Builder;

public class consularVentasSauceGranEstacion {
    private static ConnectionFirebird Tns=null;
    private static String url="";
    private static String usuario="";
    private static String password="";    
    private static String token="";
    private static JSONObject obj = new JSONObject();
    private static JSONObject sale = new JSONObject();
    private static JSONArray pagos = new JSONArray();
    private static JSONObject pago = new JSONObject();
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
    public static String BuscarPrefijo(String prefijo) throws ClassNotFoundException, SQLException{
        String prefijoString="GS";
        sqlString="select codprefijo from prefijo where preimp='"+prefijo+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            prefijoString=rs.getString("codprefijo");
        }
        if(prefijoString.equals("")){prefijoString=prefijo;}
        return prefijoString;
    }
    public static void ConsultarPedidos(String fechaString,String idString,String insertaString,String sucid) throws IOException, ClassNotFoundException, JSONException, SQLException, InterruptedException{
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
    String cufe,estadodian;
    String formapago="";
    String banco="";
    Float Cantidad=(float) 0;
    Float precioBaseFloat=(float) 0;
    ConsultarToken();    
    OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(600, TimeUnit.SECONDS).readTimeout(600, TimeUnit.SECONDS).build();    
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
   // System.out.println(response.body().string());
    String respuesta=response.body().string();
    ResultSet rsD=null;
    obj =new JSONObject(respuesta);
        if(insertaString.equals("N")){
            if(response.code()==200){
                System.out.println(obj.getJSONArray("Resultado").length());
                sqlString="delete from varios where variab='CANTIDADTERPEL'";
                Tns.actualizar(sqlString);
                sqlString="insert into varios (contenido,variab) values('"+obj.getJSONArray("Resultado").length()+"','CANTIDADTERPEL')";  
                Tns.actualizar(sqlString);
            } else {
                
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
            for (int i = 0; i < obj.getJSONArray("Resultado").length(); i++) {
            //for (int i = 0; i < 5; i++) { 
                teridString=""; 
                fechaVentaString="";
                vendedorIdString="";
                kardexidString="";
                codcomp="";
                prefijo="";
                numero="";
                observaciones="";
                formapago="CO";
                cufe="";
                estadodian="";
                banco="";
                sale=obj.getJSONArray("Resultado").getJSONObject(i);  
                teridString=ConsultarTerid(sale,"cliente");  
                vendedorIdString=ConsultarTerid(sale, "vendedor");
                observaciones=sale.getString("Recibo")+" "+sale.getString("Placa");
                if(sale.get("FacturacionElectronica").equals(null)){
                    if(ObtenerCampo(sale, "Kilometraje").equals("6")){
                        codcomp="FV";
                        formapago="CO";
                        if(sucid.equals("1")){
                            prefijo="FE";
                        }else{
                            prefijo="F1";
                        }
                         
                    }else{
                        codcomp="RS";
                        formapago="CR";
                        prefijo="P1";                    
                    }
                    numero=sale.getString("Recibo").trim();
                    
                   // cufe="null";
                }else {
                    codcomp="FV";
                    prefijo=sale.getJSONArray("FacturacionElectronica").getJSONObject(0).getString("Prefijo");
                    prefijo=BuscarPrefijo(prefijo);
                    numero=sale.getJSONArray("FacturacionElectronica").getJSONObject(0).getString("Numero");
                   // cufe="'"+sale.getJSONArray("FacturacionElectronica").getJSONObject(0).getString("Cufe")+"'";
                    estadodian="EXITOSA";
                }
          
                if (BuscarVenta(codcomp,prefijo,numero,sucid)) {
                    GuardarLog("LA FACTURA "+prefijo+numero+" YA EXISTE");
                    Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
                } else {  
                    String consecutivo;
                    if(codcomp.equals("FV")){
                        sqlString="select CONSECUTIVO from CONSECUTIVO where sucid='"+sucid+"' and codcomp='"+codcomp+"' and codprefijo='"+prefijo+"'";
                            rsD=Tns.consultar(sqlString);
                            
                            if (rsD.next()){
                                consecutivo=rsD.getString("CONSECUTIVO");
                                Integer numEntero = Integer.parseInt(consecutivo);
                                numEntero=numEntero+1;    
                                consecutivo=numEntero.toString();
                            }else{
                                consecutivo="1";
                            }  
                    }else{
                        consecutivo=numero;
                    }
                    banco=ConsultarBanco(ObtenerCampo(sale, "Kilometraje"));
                    fechaVentaString=sale.getString("HoraFin").split("T")[0];
                    hora=sale.getString("HoraFin").split("T")[1].substring(0,5);    
                    String cenid="1";
                    if(sucid.equals("1")){
                        cenid="1";
                    }else{
                        cenid="3";
                    }                
                    //fechaVentaString=fechaString;
                    fechaVentaString=fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);
                    sqlString="insert into kardex(codcomp,codprefijo,numero,estadodian,observ,fecha,periodo,cenid,areadid,sucid,cliente,vendedor,formapago,bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto,netobase,hora)"+
                              "values('"+codcomp+"'"+
                              ",'"+prefijo+"'"+
                              ",'"+consecutivo+"'"+
                              ",'"+estadodian+"'"+
                              ",'"+observaciones+"'"+
                              ",'"+fechaVentaString+"'"+
                              ",'"+fechaVentaString.substring(0, 2)+"'"+
                              ",'"+cenid+"'"+
                              ",1"+
                              ",'"+sucid+"'"+
                              ",'"+teridString+"'"+
                              ",'"+vendedorIdString+"'"+
                              ",'"+formapago+"'"+
                              ",'"+banco+"'"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",'"+sale.get("Valor").toString()+"'"+
                              ",0"+
                              ",'"+sale.get("Valor").toString()+"'"+
                              ",'"+sale.get("Valor").toString()+"'"+
                              ",0"+
                              ",'"+teridString+"'"+
                              ",1"+
                              ",'"+sale.get("Valor").toString()+"'"+
                              ",'"+sale.get("Valor").toString()+"'"+
                              ",'"+sale.get("Valor").toString()+"'"+
                              ",'"+hora+"'"+
                              ")";
                    Tns.actualizar(sqlString);
                    kardexidString=ConsultarVenta(codcomp,consecutivo,prefijo,sucid);
                    
                    if(BuscarMaterial(sale)){
                        matidString=ConsultarMaterial(sale);
                        Cantidad=sale.getFloat("Valor")/sale.getFloat("Precio");
                        precioBaseFloat=sale.getFloat("Precio");
                        sqlString="insert into dekardex(kardexid,matid,bodid,prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta,preciobase"+
                                    ",precioiva,precioneto,parcvta,preciotasa,parctasa)"+
                                    "values('"+kardexidString+"'"+
                                    ",'"+matidString+"'"+
                                    ",2"+
                                    ",7,0,'D',0,0"+
                                    ",'"+String.valueOf(Cantidad)+"'"+
                                    ",'"+String.valueOf(Cantidad)+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",0"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+sale.get("Valor").toString()+"'"+
                                    ",'"+precioBaseFloat+"'"+
                                    ",'"+sale.get("Valor").toString()+"'"+
                                    ")";
                                    Tns.actualizar(sqlString);
                    }else{
                        GuardarLog("EL ARTICULO "+sale.getJSONObject("Producto").getString("Nombre")+" NO EXISTE, EN LA FACTURA "+consecutivo);
                    }   
                    Tns.actualizar("update consecutivo set consecutivo='"+consecutivo+"' where sucid='"+sucid+"' and codcomp='"+codcomp+"' and codprefijo='"+prefijo+"'");                                    
                   Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
                   System.out.println(String.valueOf(i+1) +" de "+obj.getJSONArray("Resultado").length()); 
                }
            }
        }
    }
    public static String ConsultaFormapago(String formaString) throws ClassNotFoundException, SQLException{
        String formaId="1";
        sqlString="select formapagoid from formapago where descrip='"+formaString.toUpperCase()+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            formaId=rs.getString("formapagoid");
        }
        return formaId;
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

    public static String ConsultarVenta(String codcomp,String numero,String prefijo,String sucid) throws ClassNotFoundException, SQLException{
        String kardexidString="";
        sqlString="select kardexid from kardex where sucid='"+sucid+"' and codcomp='"+codcomp+"' and codprefijo='"+prefijo+"' and numero='"+numero+"'";
        System.out.println(sqlString);
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            kardexidString=rs.getString("kardexid");
        }
        return kardexidString;
    }
    public static Boolean BuscarMaterial(JSONObject productJsonObject) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        String nombre="";
        nombre=productJsonObject.getJSONObject("Producto").getString("Nombre");
        sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>10 and (m.descrip like '%"+nombre+"%' or m.codigo='"+productJsonObject.getJSONObject("Producto").getString("CodProducto")+"')";                
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            existe=true;
        }
        return existe;
    }
    public static String ConsultarMaterial(JSONObject productJsonObject) throws ClassNotFoundException, SQLException{
        String matidString="";
        String nombre="";
        nombre=productJsonObject.getJSONObject("Producto").getString("Nombre");
        sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>10 and (m.descrip like '%"+nombre+"%' or m.codigo='"+productJsonObject.getJSONObject("Producto").getString("CodProducto")+"')";                
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("matid");
        }
        return matidString;
    }
    public static String ConsultarBanco(String codigo) throws ClassNotFoundException, SQLException{
        String matidString="1";    
        sqlString="select bcoid from banco where codigo='"+codigo+"'";
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("bcoid");
        }
        return matidString;
    }

    public static Boolean BuscarVenta(String codcomp,String prefijo,String numero,String sucid) throws ClassNotFoundException, SQLException{
        Boolean existe=false;
        sqlString="select kardexid from kardex where observ like '"+numero+"%' and sucid='"+sucid+"'";         
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
        JSONObject tercero;
        if(tipo.equals("cliente")){
            if(venta.get("Cliente").equals(null)){
                if(venta.getString("Placa").equals("")){
                    sqlString="select terid from terceros where (nit ='22222222222' or NOMBRE ='CONSUMIDOR FINAL' )";
                }else{
                    sqlString="select terid from terceros where (observ like  '%"+venta.getString("Placa").split(" ")[0].toUpperCase()+"%' or nit like '%"+venta.getString("Placa").split(" ")[0].toUpperCase()+"%')";
                }
                
            }else{
                sqlString="select terid from terceros where (nit ='"+venta.getJSONObject("Cliente").getString("NumeroDocumento")+"' or nittri ='"+venta.getJSONObject("Cliente").getString("NumeroDocumento")+"' or nombre ='"+venta.getJSONObject("Cliente").getString("Nombre").toUpperCase()+"' )";
            }            
        }else{
            sqlString="select terid from terceros where (nit ='"+venta.getJSONObject("Empleado").getString("Cedula")+"' or nittri ='"+venta.getJSONObject("Empleado").getString("Cedula")+"' or nombre ='"+venta.getJSONObject("Empleado").getString("Nombre").toUpperCase()+"' )";
        }
        
        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            teridString=rs.getString("terid");
        }
        if(teridString.isEmpty()){
            if(tipo.equals("cliente")){
                    if(!venta.getString("Placa").equals("")){
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                        .url("https://caes.eds.com.co/api/thirds/?identification="+venta.getString("Placa")+"&type=CC")
                        .get()
                        .addHeader("Accept", "*/*")
                        .addHeader("User-Agent", "Thunder Client (https://www.thunderclient.com)")
                        .build();
                        try {
                            Response response = client.newCall(request).execute();
                            String respuesta=response.body().string();                            
                            tercero =new JSONObject(respuesta);
                            if(tercero.getInt("count")>0){
                                String nombre=tercero.getJSONArray("results").getJSONObject(0).getString("last_name")+" "+tercero.getJSONArray("results").getJSONObject(0).getString("second_last_name")+" "+tercero.getJSONArray("results").getJSONObject(0).getString("name")+" "+tercero.getJSONArray("results").getJSONObject(0).getString("second_name");
                                sqlString="insert into terceros(nit,nittri,nombre,direcc1,email,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
                                        "values('"+tercero.getJSONArray("results").getJSONObject(0).getString("identification")+"','"+tercero.getJSONArray("results").getJSONObject(0).getString("identification")+"'"+
                                        ",'"+nombre+"','"+tercero.getJSONArray("results").getJSONObject(0).getString("address")+"','"+tercero.getJSONArray("results").getJSONObject(0).getString("email")+"'"+
                                        ",'SIN TELEFONO','','C','1','S','now',1,1)";
                                        Tns.actualizar(sqlString);  
                            }
                            sqlString="select terid from terceros where (observ like  '%"+venta.getString("Placa").split(" ")[0].toUpperCase()+"%' or nit like '%"+venta.getString("Placa").split(" ")[0].toUpperCase()+"%')";
                            rs =Tns.consultar(sqlString);    
                            if (rs.next()){
                                teridString=rs.getString("terid");                    
                            }else{
                                GuardarLog("LA FACTURA "+venta.getString("Recibo")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL ");
                                sqlString="select terid from terceros where (nit ='22222222222' or NOMBRE ='CONSUMIDOR FINAL' )";
                                    rs =Tns.consultar(sqlString);    
                                    if (rs.next()){
                                        teridString=rs.getString("terid");                    
                                    } 
                            } 
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }else{
                        GuardarLog("LA FACTURA "+venta.getString("Recibo")+" SE CARGO CON EL CLIENTE CONSUMIDOR FINAL ");
                        sqlString="select terid from terceros where (nit ='22222222222' or NOMBRE ='CONSUMIDOR FINAL' )";
                            rs =Tns.consultar(sqlString);    
                            if (rs.next()){
                                teridString=rs.getString("terid");                    
                            } 
                    }
          
            }else{
                sqlString="insert into terceros(nit,nittri,nombre,direcc1,telef1,telef2,tipodociden,zona1,empleado,fechcreac,clasificaid,ciudaneid,email)"+
                "values('"+venta.getJSONObject("Empleado").getString("Cedula")+"','"+venta.getJSONObject("Empleado").getString("Cedula")+"'"+
                ",'"+venta.getJSONObject("Empleado").getString("Nombre")+"','"+venta.getJSONObject("Empleado").getString("Direccion")+"'"+
                ",'"+venta.getJSONObject("Empleado").getString("Telefono")+"','','C','1','S','now',1,1,'')";
                Tns.actualizar(sqlString);   
                sqlString="select terid from terceros where (nit ='"+venta.getJSONObject("Empleado").getString("Cedula")+"' or nittri ='"+venta.getJSONObject("Empleado").getString("Cedula")+"' or nombre ='"+venta.getJSONObject("Empleado").getString("Nombre").toUpperCase()+"' )";
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
        token =usuario+":"+password;
        token= DatatypeConverter.printBase64Binary(token.getBytes());
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
        }
        token=usuario+":"+password;
        token=DatatypeConverter.printBase64Binary(token.getBytes());
        System.out.println(usuario+" "+password);
        System.out.println(token);
    }
    
}
