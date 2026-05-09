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
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sergio.prueba.ConnectionFirebird;

import okhttp3.*;

public class consultarPedidos {
    private static ConnectionFirebird Tns=null;
    private static String usuario="";
    private static String password="";
    private static String token="";
    private static JSONObject obj = new JSONObject();
    private static JSONObject respuestaObject = new JSONObject();
    private static JSONObject respuestaObject2 = new JSONObject();
    private static JSONArray rows = new JSONArray();
    private static JSONObject row = new JSONObject();
    private static JSONArray values = new JSONArray();
    private static String claveSicom="";

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException,JSONException{
        File tempo = new File("c:\\tempo\\sicom.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String ruta=data.split("-")[0];
        String ip=data.split("-")[1];
        String usuarioTNS=data.split("-")[2];
        String urlString=data.split("-")[3];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario(usuarioTNS);
        ConsultarPedidos(urlString);
      //  ActualizarCupos(urlString);
        Tns.cerrarConexion();
        br.close();
        System.out.println("termine");
    }
    public static void ConsultarPedidos(String uString) throws IOException, ClassNotFoundException, JSONException, SQLException{
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        client=builder.build();
        Request request = new Request.Builder().url(uString+"/sicomdata/ProveedoresBasico(330023)/PedidoSimpleSolicitudes").get()
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")    
        .addHeader("Authorization", "Basic "+token)
            .build();
            Response response = client.newCall(request).execute();
            System.out.println(response.code());
            if(response.code()!=500){
                Tns.actualizar("delete from varios where variab='ESTADOSICOM'");
                Tns.actualizar("insert into varios(variab,contenido)values('ESTADOSICOM','El servicio esta caido')");
            }
            //response.close();
            String respuesta=response.body().string();
            obj =new JSONObject(respuesta);
            values= obj.getJSONArray("value");
            System.out.println("Inicie");
            for (int i = 0; i < values.length(); i++) {
              System.out.println(values.getJSONObject(i).getString("Id"));
              if(!ConsultarPedido(values.getJSONObject(i).getString("Id"))){
                  InsertaPedido(values.getJSONObject(i),uString);
              }else{
                 ActualizaPedido(values.getJSONObject(i));
              }

            }
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
    public static void ActualizarCupos(String urlString) throws IOException, JSONException, SQLException{
        String caseString="";
        String workitem="";
        String task="";
        String mensajeRespuesta="";
        String vDisponible="",vReservado="",vMensual="";
        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        @SuppressWarnings("deprecation")
        RequestBody body = RequestBody.create(mediaType, "user=330023000&password="+claveSicom+"&domain=DOMAIN&loginOption=saveAccountPassword&type=&oAuth2InternalState=");
        builder = configureToIgnoreCertificate(builder);
        client=builder.build();
        Request request = new Request.Builder().url(urlString.replace("/WS","")+"/UAT_TEST/Api/Authentication/User")
        .post(body)
        .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .addHeader("Origin", "https://uatbpm.sicom.gov.co")
        .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
        .addHeader("Accept-Language", "es,es-ES;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,es-CO;q=0.5")
        .addHeader("Cookie", "syncTimerTimeout=1710951424399; loginOptions=loginOption=saveAccountPassword&userName=330023000&domain=domain&expires=4/19/2024%204:17:07%20PM; ai_user=t9qDN|2024-03-20T17:08:27.772Z; ASP.NET_SessionId=5n5esw1kgqltpnd1f30j0fpd")
        .addHeader("Sec", "")
        .build();
        Response response = client.newCall(request).execute();

        Request request3 = new Request.Builder().url(urlString.replace("/WS","")+"/UAT_TEST/Api/Authentication/BizagiConfig")
        .get()
        .addHeader("Accept", "*/*")
        .addHeader("sec-ch-ua-platform", "'Windows'")
        .addHeader("sec-ch-ua-mobile", "?0")
        .addHeader("Cookie", response.headers("set-cookie").get(0))
        .addHeader("Cookie", response.headers("set-cookie").get(1))
        .addHeader("Cookie", response.headers("set-cookie").get(2))
        .build();
        Response response3 = client.newCall(request3).execute();
        String respuesta=response3.body().string();
        respuestaObject =new JSONObject(respuesta);
        Request request2 = new Request.Builder().url(urlString.replace("/WS","")+"/UAT_TEST/Rest/Processes/CustomizedColumnsData?smartInboxFilter=W10%3D&pageSize=100&page=1&orderFieldName=&orderType=0&order=&taskState=all&idWorkflow=16&_=171095382971").get()
        .addHeader("Accept", "*/*")
        .addHeader("x-bzxsrf-token", respuestaObject.getString("XSRFToken"))
        .addHeader("sec-ch-ua-platform", "Windows")
        .addHeader("sec-ch-ua-mobile", "?0")
        .addHeader("Cookie", response.headers("set-cookie").get(0))
        .addHeader("Cookie", response.headers("set-cookie").get(1))
        .addHeader("Cookie", response.headers("set-cookie").get(2))
        .build();
        Response response2 = client.newCall(request2).execute();
        obj = new JSONObject(response2.body().string());
        rows = obj.getJSONObject("cases").getJSONArray("rows");
        System.out.println(response.code());
        for (int i = 0; i < rows.length(); i++) {
            mensajeRespuesta="";
            row = rows.getJSONObject(i);
            caseString="";
            workitem="";
            task="";
            caseString = row.get("id").toString();
            workitem=row.getJSONArray("fields").getJSONObject(3).getJSONArray("workitems").getJSONObject(0).get("idWorkItem").toString();
            task=row.getJSONArray("fields").getJSONObject(3).getJSONArray("workitems").getJSONObject(0).get("idTask").toString();
            //System.out.println(row.getJSONArray("fields").get(0)+" "+caseString+" "+workitem+" "+task);
            OkHttpClient client1 =new OkHttpClient();
            OkHttpClient.Builder builder1 = new OkHttpClient.Builder();
            @SuppressWarnings("deprecation")
            RequestBody body1 = RequestBody.create(mediaType, "h_action=LOADFORM&h_devicetype=0&h_devicecode=1920x1080&h_idCase="+caseString+"&h_idWorkitem="+workitem+"&h_idTask="+task);
            builder1 = configureToIgnoreCertificate(builder1);
            client1=builder1.build();
            Request request1 = new Request.Builder().url(urlString.replace("/WS","")+"/UAT_TEST/Rest/Handlers/Render")
            .post(body1)
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("x-bzxsrf-token", respuestaObject.getString("XSRFToken"))
            .addHeader("sec-ch-ua-platform", "Windows")
            .addHeader("sec-ch-ua-mobile", "?0")
            .addHeader("Cookie", response.headers("set-cookie").get(0))
            .addHeader("Cookie", response.headers("set-cookie").get(1))
            .addHeader("Cookie", response.headers("set-cookie").get(2))
            .build();
            Response response4 = client1.newCall(request1).execute();
            respuestaObject2 = new JSONObject(response4.body().string());
            mensajeRespuesta=respuestaObject2.getJSONObject("form").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONObject("properties").getString("displayName");
            //System.out.println(mensajeRespuesta);
            if (mensajeRespuesta.contains("Aceptaci")){
                vDisponible=respuestaObject2.getJSONObject("form").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(2).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(1).getJSONObject("render").getJSONObject("properties").get("value").toString();
                vMensual=respuestaObject2.getJSONObject("form").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(2).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(1).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("render").getJSONObject("properties").get("value").toString();
                vReservado=respuestaObject2.getJSONObject("form").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(2).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(0).getJSONObject("container").getJSONArray("elements").getJSONObject(1).getJSONObject("container").getJSONArray("elements").getJSONObject(1).getJSONObject("render").getJSONObject("properties").get("value").toString();                
                Tns.actualizar("update pedidosicom set vldisponible='"+vDisponible+"',vlmensual='"+vMensual+"',vlReservado='"+vReservado+"' where nro_caso='"+rows.getJSONObject(i).getJSONArray("fields").get(0)+"'");
            }
        }

    }
    public static void InsertaPedido(JSONObject pedido,String urlString) throws SQLException, IOException{
        
        String sql="insert into pedidosicom(id,proveedor,proveedornit,autorizacion,codigo_comprador,despacho_codigo,recibo_planta,tipo_transporte,placa,placa_remolque"+
        ",conductor,cedula_conductor,fecha_entrega,fecha,sobrecupo,observaciones,productos,clave,rowversion,Nro_Caso,vldisponible,vlmensual,vlReservado,vlmismo,vldiferente,resgaso,resacpm)"+
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
        "'"+pedido.getString("Nro_Caso")+"',"+
        "'"+pedido.getString("Volumen_maximo_asignado_disponible")+"',"+
        "'"+pedido.getString("Volumen_maximo_asignado_total_mensual")+"',"+
        "'"+pedido.getString("Volumen_maximo_asignado_total_reservado")+"',"+
        "'"+pedido.getString("Volumen_cesion_eds_mismo")+"',"+
        "'"+pedido.getString("Volumen_cesion_eds_diferentes")+"',"+
        "'"+pedido.getString("Reserva_gasolina")+"',"+
        "'"+pedido.getString("Reserva_acpm")+"'"+        
        ")";                
        Tns.actualizar(sql);
    } 
    public static void ActualizaPedido(JSONObject pedido) throws SQLException{
        String sql="update pedidosicom set observaciones='"+pedido.get("Observaciones").toString()+"' where id='"+pedido.getString("Id")+"'";
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
    public static void ConsultarDatosUsuario(String usuarioString)throws SQLException, ClassNotFoundException{
        String sql="select * from usuarios where nombre='"+usuarioString+"'";

        ResultSet rs = Tns.consultar(sql);
        while(rs.next()){
            usuario=rs.getString("USUARIOSICOM");
            password=rs.getString("PASSWORDSICOM");
        }
        token=usuario+":"+password;
        token=DatatypeConverter.printBase64Binary(token.getBytes());
        System.out.println(usuario+" "+password);
        System.out.println(token);
        sql="select contenido from varios where variab='CLAVESICOM'";
        rs.close();
        rs = Tns.consultar(sql);
        while (rs.next()) {
            claveSicom=rs.getString("CONTENIDO");
        }
        System.out.println(claveSicom);
    }
    
}
