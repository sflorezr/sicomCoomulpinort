package com.test.java.com.sergio.facelectronica;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sergio.prueba.ConnectionFirebird;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class rechazarPedido {
    private static ConnectionFirebird Tns=null;
    private static String usuario="";
    private static String password="";
    private static String token="";
    private static String OP_Autorizacion_Codigo="";
    private static String Seguridad_Contrasena="";
    private static String Rechazo_Motivo_Descripcion="";
    private static String RowVersion="";
    private static JsonObject object = new JsonObject(); 
    private static JSONObject objResponse =null; 
    private static String json =null; 

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        File tempo = new File("c:\\tempo\\sicomRechazar.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String base=data.split("\\|")[0];
        String ip=data.split("\\|")[1];
        String usuario=data.split("\\|")[2];
        String id=data.split("\\|")[3];
        String endPoint=data.split("\\|")[4];
        br.close();
        Tns = new ConnectionFirebird(ip,base,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario(usuario);
        AceptarPedido(id, endPoint);
    }
    public static void AceptarPedido(String idString,String endPointString) throws ClassNotFoundException, SQLException, IOException{
        ConsultarDatosPedido(idString);
        object.addProperty("Id", idString);
        object.addProperty("OP_Autorizacion_Codigo", OP_Autorizacion_Codigo);
        object.addProperty("Rechazo_Motivo_Codigo", "17");
        object.addProperty("Rechazo_Motivo_Descripcion", Rechazo_Motivo_Descripcion);
        object.addProperty("Seguridad_Contrasena", Seguridad_Contrasena);
        object.addProperty("RowVersion", RowVersion);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(object);
        System.out.println(json);
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(240, TimeUnit.SECONDS).readTimeout(240, TimeUnit.SECONDS).build();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        client=builder.build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);
        Request request = null;
        request = new Request.Builder().url(endPointString+"/sicomdata/PedidoSimpleRechazadosBasico").method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Basic "+token).build();
        Response response = client.newCall(request).execute();
        // System.out.println(response.toString());
        
        System.out.println(response.code());  
        String vsql="";
        if(response.code()==200||response.code()==400){
            vsql="update pedidosicom set status_rechazar='200', mensaje_rechazar='EXITOSA' where id='"+idString+"'";
        } else{
            objResponse = new JSONObject(response.body().string());
            System.out.println(objResponse.getJSONObject("odata.error").getJSONObject("message").getString("value"));
            vsql="update pedidosicom set status_rechazar='"+Integer.toString(response.code())+"', mensaje_rechazar='"+objResponse.getJSONObject("odata.error").getJSONObject("message").getString("value")+"' where id='"+idString+"'";
        } 
        Tns.actualizar(vsql);
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
    }
    public static void ConsultarDatosPedido(String idString) throws ClassNotFoundException, SQLException{
        String sql="select * from pedidosicom where id='"+idString+"'";
        ResultSet rs = Tns.consultar(sql);
        while(rs.next()){
          OP_Autorizacion_Codigo=rs.getString("AUTORIZACION");
          Seguridad_Contrasena=password;
          RowVersion=rs.getString("ROWVERSION");
          Rechazo_Motivo_Descripcion=rs.getString("MOTIVO_RECHAZO");
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
}
