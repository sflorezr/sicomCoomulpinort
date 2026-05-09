package com.test.java.com.sergio.facelectronica;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConsultaRemisiones {
    public static void main(String[] args) throws IOException {
        ConsultarRemisiones();

    }

    public static void ConsultarRemisiones() throws IOException{
        OkHttpClient client2 =new OkHttpClient();
        OkHttpClient.Builder builder2 = new OkHttpClient.Builder();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        @SuppressWarnings("deprecation")
        RequestBody body2 = RequestBody.create(mediaType, "nit=9014823555&login=audcontable&passwd=Dibalcont123$&submit=Login&origen=new");
        builder2 = configureToIgnoreCertificate(builder2);
        client2=builder2.build();

        Request request2 = new Request.Builder().url("https://www.sicoe.com.co/index1.php")
        .post(body2)
        .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .build();
        Response response2 = client2.newCall(request2).execute();
        System.out.println(response2.header("Set-Cookie").split(";")[0]);

        OkHttpClient client =new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = configureToIgnoreCertificate(builder);
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        client=builder.build();
        Request request = new Request.Builder().url("https://www.sicoe.com.co/sam/ajax/ajax_informe_detallado_x_factura.php?id_asesor=&fecha_ini=2024-04-16&fecha_fin=2024-04-16&id_producto=&id_cliente=&estado=T&id_tipo_documento=R&id_proveedor=&tipoproducto=N&categoria=&sublinea=&planilla=&bodega=&zona=&supervisor=").get()
        //.addHeader("Content-Type", "application/json")
        .addHeader("Accept", "*/*")    
        .addHeader("Cookie", response2.header("Set-Cookie").split(";")[0])
            .build();
            Response response = client.newCall(request).execute();
            System.out.println(response.code());
            Document document = Jsoup.parse(response.body().string());
            //System.out.println(document);
            Element table = document.select("table").first();
            String arrayName = table.select("td").first().text();
            JSONObject jsonObj = new JSONObject();
            JSONArray jsonArr = new JSONArray();
            Elements cellLabel = table.getElementsByClass("cellimpar");
            Elements cellpar = table.getElementsByClass("cellpar");
            for (int i = 0; i < 50; i++) {
                System.out.println(cellLabel.get(i).text());
              //  System.out.println(cellpar.get(i).text());
            }
           /// Elements nfos = table.getElementsByClass("nfo");
           // response.body().string()
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
