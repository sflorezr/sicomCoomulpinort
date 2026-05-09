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

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sergio.prueba.ConnectionFirebird;
import com.sergio.prueba.kardex;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class pruebaSicoe {
        private static ConnectionFirebird Tns = null;
        private static String nit = null;
        private static String usuario = null;
        private static String clave = null;

        public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
                File tempo = new File("c:\\tempo\\sicoe.txt");
                FileReader fr = new FileReader(tempo);
                BufferedReader br = new BufferedReader(fr);
                String data = br.readLine();
                String ruta = data.split("\\|")[0];
                String ip = data.split("\\|")[1];
                String fecha = data.split("\\|")[2];
                String fechaFin = data.split("\\|")[3];
                String insertar = data.split("\\|")[4];
                Tns = new ConnectionFirebird(ip, ruta, "SYSDBA", "masterkey", "3050");
                String token = consultarToken();                
                // String fecha = "2025-01-24";
                // Compras(token);
                Ventas(token, fecha, fechaFin,insertar);
               // Compras(token, fecha, fechaFin,insertar);

                // System.out.println(table);
        }

        public static void Ventas(String token, String fecha, String fechaFin,String insertar) throws IOException, ClassNotFoundException, SQLException {
                OkHttpClient client = new OkHttpClient().newBuilder().build();
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                builder = configureToIgnoreCertificate(builder);
                String sqlString="",cantidad,cantidadDet,valor;
                builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5,TimeUnit.MINUTES);
                client=builder.build();
                String terid, kardexid,matid;
                Double valorBase=0.0,valorD=0.0;
                MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");               
                 String       url="https://www.sicoe.com.co/sam/ajax/ajax_informe_detallado_x_factura.php?id_asesor=&fecha_ini="+ fecha + "&fecha_fin=" + fechaFin+ "&id_producto=&id_cliente=&estado=T&id_tipo_documento=R&id_proveedor=&tipoproducto=N&categoria=&sublinea=&planilla=&bodega=&zona=&supervisor=&marca=";
                Request request = new Request.Builder()                
                                .url(url)
                                .get()
                                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                                .addHeader("X-Requested-With", "XMLHttpRequest")
                                .addHeader("User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                                .addHeader("Accept", "text/html, */*; q=0.01")
                                .addHeader("sec-ch-ua",
                                                "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                                .addHeader("sec-ch-ua-mobile", "?0")
                                .addHeader("Sec-Fetch-Site", "same-origin")
                                .addHeader("Sec-Fetch-Mode", "cors")
                                .addHeader("Sec-Fetch-Dest", "empty")
                                .addHeader("host", "www.sicoe.com.co")
                                .addHeader("Cookie", "PHPSESSID=" + token)
                                .build();
                Response response = client.newCall(request).execute();
                Document doc = Jsoup.parse(response.body().string());
                Elements table = doc.select("table");
                Elements rows = table.select("tr");
                if(insertar.equals("N")){
                        sqlString="delete from varios where variab='CANTIDADSICOE'";
                        Tns.actualizar(sqlString);               
                        sqlString="insert into varios (contenido,variab) values('"+String.valueOf(rows.size()-2)+"','CANTIDADSICOE')";  
                        Tns.actualizar(sqlString);
                }else{
                        sqlString="delete from varios where variab='CANTIDADSICOE'";
                        Tns.actualizar(sqlString);
                        sqlString="insert into varios (contenido,variab) values('"+String.valueOf(rows.size()-2)+"','CANTIDADSICOE')";  
                        Tns.actualizar(sqlString);
                        sqlString="delete from varios where variab='CANTIDADSICOESUBIDA'";
                        Tns.actualizar(sqlString);
                        sqlString="insert into varios (contenido,variab) values('0','CANTIDADSICOESUBIDA')";  
                        Tns.actualizar(sqlString);                           
                        for (int i = 2; i < rows.size()-2; i++) { 
                // for (int i = 2; i < 6; i++) {
                                Element row = rows.get(i);
                                Elements cells = row.select("td");
                                terid = "";
                                kardexid="";
                                matid="";
                                cantidad="";
                                cantidadDet="";
                                valor="";
                                //valoriva="";
                                valorD=0.0;
                                System.out.println(cells.get(0).select("td").get(0).text());
                                terid = ConsultarTercero(cells.get(1).select("td").get(0).text().split("-")[0],cells.get(1).select("td").get(0).text().split("-")[1]);
                                kardexid = BuscarKardexid(terid,cells.get(0).select("td").get(0).text(),cells.get(16).select("td").get(0).text(),"FV");
                                matid = BuscarMatid(cells.get(3).select("td").get(0).text());
                                cantidadDet=cells.get(5).select("td").get(0).text().replaceAll("\\.00", "");
                                cantidad=cells.get(7).select("td").get(0).text().replaceAll("\\.00", "");
                                cantidad=cantidad.replaceAll(",", "");   
                                valor=cells.get(9).select("td").get(0).text().replaceAll("\\.00", "");
                                valor=valor.replaceAll("\\$", "");
                                valor=valor.replaceAll(",", "");   
                                cantidadDet=cantidadDet.replaceAll(",", "");
                                valorBase=Double.parseDouble(cantidadDet)*Double.parseDouble(valor);
                                valorD=valorBase/Double.parseDouble(cantidad);
                                sqlString="insert into dekardex(kardexid,matid,bodid,prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta,preciobase"+
                                                ",precioiva,precioneto,parcvta,preciotasa,parctasa)"+
                                                "values('"+kardexid+"'"+
                                                ",'"+matid+"'"+
                                                ",1"+
                                                ",7,0,'M',0,0"+
                                                ",'"+cantidad+"'"+
                                                ",'"+cantidad+"'"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",0"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",'"+String.valueOf(valorBase)+"'"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",'"+String.valueOf(valorBase)+"'"+
                                                ")";
                                                Tns.actualizar(sqlString);  
                                Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADSICOESUBIDA'");                      
                        }
                }
        }
                        
        public static String BuscarMatid(String nombre) throws ClassNotFoundException, SQLException{
                String matid="1";
                String codigo,nombreArticulo;
                codigo=nombre.split("-")[0];
                nombreArticulo=nombre.split("-")[1];
                ResultSet rs=null;
                rs= Tns.consultar("select matid from material where (codigo like '%"+codigo+"%' or descrip like '%"+nombreArticulo+"%')");
                if(rs.next()){
                        matid=rs.getString("matid");                
                }
                return matid;
        }
                        
        public static String ConsultarTercero(String nit, String nombre) throws ClassNotFoundException, SQLException {
                String terid = "1";
                String sqlString="";
                ResultSet rs = Tns.consultar("select terid from terceros where ( nit='" + nit + "' or nombre ='" + nombre + "' or establecimiento = '"+nombre+"')");
                if (rs.next()) {
                   //     System.out.println("tercero encontrado " + rs.getString("terid") + " " + nombre + "-" + nit);
                        terid = rs.getString("terid");
                }else{
                        sqlString="insert into terceros(nit,nittri,nombre,direcc1,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
                        "values('"+nit+"','"+nit+"'"+
                        ",'"+nombre+"','SIN DIRECCION'"+
                        ",'SIN TELEFONO','','C','1','S','now',1,1)";
                        Tns.actualizar(sqlString);
                        rs = Tns.consultar("select terid from terceros where ( nit='" + nit + "' or nombre ='" + nombre + "' or establecimiento = '"+nombre+"')");
                        if (rs.next()) {
                        //     System.out.println("tercero encontrado " + rs.getString("terid") + " " + nombre + "-" + nit);
                                terid = rs.getString("terid");
                        }
                }
                return terid;
        }

        public static String BuscarKardexid(String terid, String numero, String fecha,String tipo) throws ClassNotFoundException, SQLException {
                
                if (numero.length() > 8) {
                numero = numero.substring(numero.length() - 8);
                }
                String kardexid="";
                String sqlString="";
                String fechaFactura=fecha.substring(5, 7)+'/'+fecha.substring(8, 10)+'/'+fecha.substring(0, 4);
                ResultSet rs=null;
                rs=Tns.consultar("select kardexid from kardex where numero='"+numero+"' and fecha='"+fechaFactura+"'");
                if (rs.next()){
                        kardexid=rs.getString("kardexid");
                }else{
                sqlString="insert into kardex(codcomp,codprefijo,numero,observ,fecha,FECVENCE,periodo,plazodias,cenid,areadid,sucid,cliente,vendedor"+
                    ",formapago,bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto"+
                    ",netobase,hora)"+
                              "values('"+tipo+"'"+
                              ",'00'"+
                              ",'"+numero+"'"+
                              ",'PRUEBA CON SICOE SERGIO FLOREZ'"+
                              ",'"+fechaFactura+"'"+
                              ",'"+fechaFactura+"'"+
                              ",'"+fecha.substring(5, 7)+"'"+
                              ",1"+
                              ",1"+
                              ",1"+
                              ",1"+
                              ",'"+terid+"'"+
                              ",1"+
                              ",'CR'"+
                              ",1"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",'"+terid+"'"+
                              ",1"+
                              ",0"+
                              ",0"+
                              ",0"+
                              ",'00:00'"+                                                                                       
                              ")  ";
                        Tns.actualizar(sqlString);
                        rs=Tns.consultar("select kardexid from kardex where numero='"+numero+"' and fecha='"+fechaFactura+"' and cliente='"+terid+"'");
                        if (rs.next()){
                                kardexid=rs.getString("kardexid");
                        }
                }
                return kardexid;
        }

        public static void Compras(String token, String fecha, String fechaFin,String insertar) throws IOException, SQLException, ClassNotFoundException {
                System.out.println("compras...");
                OkHttpClient client = new OkHttpClient().newBuilder().build();
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                builder = configureToIgnoreCertificate(builder);
                String sqlString="",cantidad,cantidadDet,valor;
                builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5,TimeUnit.MINUTES);
                client=builder.build();
                String terid, kardexid,matid;
                Double valorBase=0.0,valorD=0.0;
                MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
                String url="https://www.sicoe.com.co/sam/ajax/ajax_informe_detalladocompras.php?fechaini="+fecha+"&fechafin="+fechaFin+"&proveedor=&concepto=1&bodega=";
                     //  url="https://www.sicoe.com.co/sam/ajax/ajax_informe_detallado_x_factura.php?id_asesor=&fecha_ini="+ fecha + "&fecha_fin=" + fechaFin+ "&id_producto=&id_cliente=&estado=T&id_tipo_documento=R&id_proveedor=&tipoproducto=N&categoria=&sublinea=&planilla=&bodega=&zona=&supervisor=&marca=";
                Request request = new Request.Builder()                
                                .url(url)
                                .get()
                                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                                .addHeader("X-Requested-With", "XMLHttpRequest")
                                .addHeader("User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                                .addHeader("Accept", "text/html, */*; q=0.01")
                                .addHeader("sec-ch-ua",
                                                "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                                .addHeader("sec-ch-ua-mobile", "?0")
                                .addHeader("Sec-Fetch-Site", "same-origin")
                                .addHeader("Sec-Fetch-Mode", "cors")
                                .addHeader("Sec-Fetch-Dest", "empty")
                                .addHeader("host", "www.sicoe.com.co")
                                .addHeader("Cookie", "PHPSESSID=" + token)
                                .build();
                Response response = client.newCall(request).execute();
                Document doc = Jsoup.parse(response.body().string());
                Elements table = doc.select("table");
                Elements rows = table.select("tr");
                if(insertar.equals("N")){
                        sqlString="delete from varios where variab='CANTIDADSICOE'";
                        Tns.actualizar(sqlString);               
                        sqlString="insert into varios (contenido,variab) values('"+String.valueOf(rows.size()-2)+"','CANTIDADSICOE')";  
                        Tns.actualizar(sqlString);
                }else{
                        sqlString="delete from varios where variab='CANTIDADSICOE'";
                        Tns.actualizar(sqlString);
                        sqlString="insert into varios (contenido,variab) values('"+String.valueOf(rows.size()-2)+"','CANTIDADSICOE')";  
                        Tns.actualizar(sqlString);
                        sqlString="delete from varios where variab='CANTIDADSICOESUBIDA'";
                        Tns.actualizar(sqlString);
                        sqlString="insert into varios (contenido,variab) values('0','CANTIDADSICOESUBIDA')";  
                        Tns.actualizar(sqlString);                                                  
                        for (int i = 2; i <= rows.size()-2; i++) { 
                // for (int i = 2; i < 6; i++) {
                                Element row = rows.get(i);
                                Elements cells = row.select("td");
                                terid = "";
                                kardexid="";
                                matid="";
                                cantidad="";
                                cantidadDet="";
                                valor="";
                                //valoriva="";
                                valorD=0.0;
                                System.out.println(cells.get(0).select("td").get(0).text());
                                terid = ConsultarTercero(cells.get(6).select("td").get(0).text().split("-")[0],cells.get(6).select("td").get(0).text().split("-")[1]);
                                kardexid = BuscarKardexid(terid,cells.get(2).select("td").get(0).text(),cells.get(3).select("td").get(0).text(),"FC");
                                matid = BuscarMatid(cells.get(4).select("td").get(0).text()+"-"+cells.get(5).select("td").get(0).text());
                                cantidadDet=cells.get(7).select("td").get(0).text().replaceAll("\\.00", "");
                                cantidad=cells.get(7).select("td").get(0).text().replaceAll("\\.00", "");
                                cantidad=cantidad.replaceAll(",", "");     
                                valor=cells.get(11).select("td").get(0).text().replaceAll("\\.00", "");
                                valor=valor.replaceAll("\\$", "");
                                valor=valor.replaceAll(",", "");                        
                                valorBase=Double.parseDouble(valor);
                                valorD=valorBase/Double.parseDouble(cantidad);
                                sqlString="insert into dekardex(kardexid,matid,bodid,prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta,preciobase"+
                                                ",precioiva,precioneto,parcvta,preciotasa,parctasa)"+
                                                "values('"+kardexid+"'"+
                                                ",'"+matid+"'"+
                                                ",1"+
                                                ",7,0,'M',0,0"+
                                                ",'"+cantidad+"'"+
                                                ",'"+cantidad+"'"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",0"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",'"+String.valueOf(valorBase)+"'"+
                                                ",'"+String.valueOf(valorD)+"'"+
                                                ",'"+String.valueOf(valorBase)+"'"+
                                                ")";
                                                Tns.actualizar(sqlString);  
                                Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADSICOESUBIDA'");                      
                        }
                }
        }

        public static String consultarToken() throws IOException, ClassNotFoundException, SQLException {
                String token = "";
                ResultSet rs = Tns.consultar("select * from varios where variab like '%SICOE%'");
                while (rs.next()){
                        switch (rs.getString("variab")) {
                                case "NITSICOE": nit = rs.getString("contenido");break;
                                case "USERSICOE": usuario = rs.getString("contenido");break;
                                case "PASSSICOE": clave = rs.getString("contenido");break;
                        
                                default:
                                        break;
                        }
                }

                OkHttpClient client = new OkHttpClient();
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                builder = configureToIgnoreCertificate(builder);
                builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5,
                                TimeUnit.MINUTES);
                client=builder.build();
                Request getRequest = new Request.Builder().url("https://www.sicoe.com.co/index1.php").build();
                Response getResponse = client.newCall(getRequest).execute();
                String phpsessid = null;

                // Obtener la cookie PHPSESSID
                for (String header : getResponse.headers("Set-Cookie")) {
                        if (header.startsWith("PHPSESSID")) {
                                phpsessid = header.split(";")[0].split("=")[1];
                                break;
                        }
                }
                System.out.println(phpsessid);
                Connection.Response response = Jsoup.connect("https://www.sicoe.com.co/index1.php")
                                .timeout(200000)
                                .header("Accept",
                                                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                                .header("Accept-Language",
                                                "es,es-ES;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,es-CO;q=0.5")
                                .header("Cache-Control", "max-age=0")
                                .header("Connection", "keep-alive")
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .cookie("PHPSESSID", phpsessid)
                                .header("Origin", "https://sicoe.com.co")
                                .header("Referer", "https://sicoe.com.co/")
                                .header("Sec-Fetch-Dest", "document")
                                .header("Sec-Fetch-Mode", "navigate")
                                .header("Sec-Fetch-Site", "same-site")
                                .header("Sec-Fetch-User", "?1")
                                .header("Upgrade-Insecure-Requests", "1")
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                                .header("sec-ch-ua",
                                                "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                                .header("sec-ch-ua-mobile", "?0")
                                .header("sec-ch-ua-platform", "\"Windows\"")
                                //.requestBody("nit=9014823555&login=audcontable&passwd=Soporte123%24&submit=Login&origen=new")
                                .requestBody("nit="+nit+"&login="+usuario+"&passwd="+clave+"&submit=Login&origen=new")
                                .method(org.jsoup.Connection.Method.POST)
                                .ignoreContentType(true)
                                .execute();
                // System.out.println(response.parse());
                token = phpsessid;
                return token;

        }

        private static OkHttpClient.Builder configureToIgnoreCertificate(OkHttpClient.Builder builder) {
                try {

                        // Create a trust manager that does not validate certificate chains
                        final TrustManager[] trustAllCerts = new TrustManager[] {
                                        new X509TrustManager() {
                                                @Override
                                                public void checkClientTrusted(
                                                                java.security.cert.X509Certificate[] chain,
                                                                String authType)
                                                                throws CertificateException {
                                                }

                                                @Override
                                                public void checkServerTrusted(
                                                                java.security.cert.X509Certificate[] chain,
                                                                String authType)
                                                                throws CertificateException {
                                                }

                                                @Override
                                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                                        return new java.security.cert.X509Certificate[] {};
                                                }
                                        }
                        };

                        // Install the all-trusting trust manager
                        final SSLContext sslContext = SSLContext.getInstance("SSL");
                        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                        // Create an ssl socket factory with our all-trusting manager
                        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
                        builder.hostnameVerifier((HostnameVerifier) new HostnameVerifier() {
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
