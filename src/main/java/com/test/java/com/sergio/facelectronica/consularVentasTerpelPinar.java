package com.test.java.com.sergio.facelectronica;
import java.io.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

public class consularVentasTerpelPinar {
    private static ConnectionFirebird Tns=null;
    private static String url="";
    private static String usuario="";
    private static String password="";    
    private static String token="";
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
    Float Cantidad=(float) 0;
    String referencia="";
    String bodega="";

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
            int total=obj.getJSONArray("data").length();
            int insertadas=0;
            int existentes=0;
            int conError=0;
            GuardarLog("INICIO CARGUE "+fechaString+" A "+fechaFinString+": "+total+" VENTAS RECIBIDAS DE TERPEL");
            for (int i = 0; i < total; i++) {
            //for (int i = 0; i < 5; i++) {
              String facturaLog="";
              try {
                teridString="";
                fechaVentaString="";
                vendedorIdString="";
                kardexidString="";
                codcomp="";
                observaciones="";
                formapago="";
                referencia="";
                bodega="";
                sale=obj.getJSONArray("data").getJSONObject(i);
                facturaLog=sale.get("consecutivo_factura").toString();
                observaciones=Sql(sale.getString("consecutivo_factura")+" "+sale.getString("placa"));
                System.out.println(sale.getString("consecutivo_factura"));

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
                numero=UltimosOchoDigitos(numero);
                if(sale.getString("tipo_factura").equals("NC")){
                    codcomp="DV";
                    referencia=sale.getString("referencia").trim();
                    referencia=referencia.replaceAll(" ", "");
                    referencia=referencia.replaceAll("-", "");
                    referencia=referencia.substring(2);
                }else{
                    if(sale.getString("tipo_factura").equals("FE")){
                        codcomp="FV";
                    }else{
                        codcomp="RS";
                    }

                }

                if (!ConsultarVenta(codcomp,numero,prefijo).isEmpty()) {
                    existentes++;
                    GuardarLog("LA FACTURA "+facturaLog+" YA EXISTE ("+codcomp+" "+prefijo+" "+numero+")");
                } else {
                    teridString=ConsultarTerid(sale,"cliente");
                    vendedorIdString=ConsultarTerid(sale, "vendedor");

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
                    if (sucid.equals("1")){
                        bodega="1";
                        if(sale.getString("metodo_pago").contains("EFECTIVO")){
                            bancoId="23";
                        }else{
                            bancoId="138";
                        }
                    }else{
                        bodega="6";
                        bancoId="125";
                    }

                    if (sale.getString("medio_autorizacion").contains("Ibutton")){
                        if(sale.getString("medio_autorizacion").contains("Rumbo")){
                            teridString = ConsultaTercero("830095213");
                        }
                        else{
                            if (sale.getString("nombre_cliente").equals("SUMINISTRO COMBUSTIBLE")){
                                teridString = "1";
                            }
                        }
                        observaciones = observaciones + " "+Sql(sale.getString("medio_autorizacion") + " " +sale.getString("nombre_cliente"));
                    }

                    
                    // Firebird recibe las fechas como MM/dd/yyyy; sin lenient un formato equivocado falla en vez de correr la fecha
                    SimpleDateFormat sdfSalida  = new SimpleDateFormat("MM/dd/yyyy");
                    sdfSalida.setLenient(false);
                    Date fecha = null;
                    fechaVentaString=fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);

                    String plazoString = "0";
                    String fecVence = "0";
                    if(sale.getString("metodo_pago").equals("CREDITO CON EDS")){
                        formapago="CR";
                        plazoString="3";
                                            try {
                        fecha = sdfSalida.parse(fechaVentaString);
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(fecha);
                        cal.add(Calendar.DAY_OF_MONTH, 3);
                        fecVence = sdfSalida.format(cal.getTime());
                    }else{
                        formapago="CO";
                        fecVence = fechaVentaString;
                    }
                                        
                /*      if(sale.getString("metodo_pago").contains(",")){
                        formapago="MU";
                    }
                */
                    
                    sqlString="insert into kardex(codcomp,codprefijo,numero,nrofactven,observ,fecha,fecvence,plazodias,periodo,cenid,areadid,sucid,cliente,vendedor,formapago,bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto,netobase,hora,fecha_terpel)"+
                              "values('"+codcomp+"'"+
                              ",'"+prefijo+"'"+
                              ",'"+numero+"'"+
                              ",'"+referencia+"'"+
                              ",'"+observaciones+"'"+
                              ",'"+fechaVentaString+"'"+
                              ",'"+fecVence+"'"+
                              ",'"+plazoString+"'"+
                              ",'"+fechaVentaString.substring(0, 2)+"'"+
                              ","+sucid+
                              ",1"+
                              ",'1'"+
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
                        if(!Ejecutar(sqlString,"AL INSERTAR EL KARDEX DE LA FACTURA "+facturaLog)){
                            conError++;
                            continue;
                        }
                        kardexidString=ConsultarVenta(codcomp,numero,prefijo);
                        if(kardexidString.isEmpty()){
                            conError++;
                            GuardarLog("LA FACTURA "+facturaLog+" NO QUEDO EN KARDEX ("+codcomp+" "+prefijo+" "+numero+") AUNQUE EL INSERT NO REPORTO ERROR");
                            continue;
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
                                        ",'"+bodega+"'"+
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
                            if(!Ejecutar(sqlString,"AL INSERTAR EL DETALLE DE LA FACTURA "+facturaLog)){
                                // se borra el encabezado para que la factura se vuelva a intentar en el siguiente cargue
                                Ejecutar("delete from kardex where kardexid="+kardexidString,"AL BORRAR EL KARDEX SIN DETALLE DE LA FACTURA "+facturaLog);
                                conError++;
                                continue;
                            }
                        }else{
                            GuardarLog("EL ARTICULO "+sale.getString("producto")+" NO EXISTE, EN LA FACTURA "+sale.getString("consecutivo_factura"));
                        }
                    insertadas++;


                   //System.out.println(i +" de "+obj.getJSONArray("data").length()+1);
                }
              } catch (Exception e) {
                conError++;
                GuardarLog("ERROR PROCESANDO LA FACTURA "+facturaLog+": "+e);
                e.printStackTrace();
              } finally {
                Tns.actualizar("update varios set contenido='"+Integer.toString(i+1)+"' where variab='CANTIDADTERPELSUBIDA'");
              }
            }
            GuardarLog("FIN CARGUE: "+total+" RECIBIDAS, "+insertadas+" INSERTADAS, "+existentes+" YA EXISTIAN, "+conError+" CON ERROR");
        }
    }
    // kardex.numero solo admite 8 caracteres y Terpel POS a veces envia consecutivos mas largos
    public static String UltimosOchoDigitos(String numero){
        numero=numero.trim();
        return numero.length()>8 ? numero.substring(numero.length()-8) : numero;
    }
    // escapa comillas simples para que un nombre como D'ALEMAN no rompa el SQL
    public static String Sql(String valor){
        return valor==null ? "" : valor.replace("'", "''");
    }
    // a diferencia de Tns.actualizar, informa en LOGTERPEL cuando el SQL falla
    public static boolean Ejecutar(String sql,String contexto){
        System.out.println(sql);
        try (Statement st=Tns.getStatement()) {
            st.executeUpdate(sql);
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            GuardarLog("ERROR "+contexto+": "+e.getMessage());
            GuardarLogArchivo("SQL FALLIDO: "+sql);
            return false;
        }
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
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>0 and m.descrip like '%"+nombre+"%' and m.codigo not like '%V%'";
        }else{
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>0 and m.descrip like '%"+nombre+"%' and m.codigo like 'V%' ";
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
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>0 and m.descrip like '%"+nombre+"%' and m.codigo not like '%V%'";
        }else{
            sqlString="select m.matid from material m inner join materialsuc ms on ms.matid=m.matid where ms.existenc>0 and m.descrip like '%"+nombre+"%' and m.codigo like 'V%' ";
        }
        ResultSet rs = Tns.consultar(sqlString);
        while (rs.next()){
            matidString=rs.getString("matid");
        }
        return matidString;
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
                String nombreCliente = Sql(venta.getString("nombre_cliente").trim().replaceAll("\\s+", "%"));
                sqlString = "SELECT terid FROM terceros WHERE TRIM(nombre) LIKE '%" + nombreCliente + "%'";
            }else{
                sqlString="select terid from terceros where (nit like '%"+venta.getString("numero_documento_fe")+"%' or nittri like '%"+venta.getString("numero_documento_fe")+"' or nombre ='"+Sql(venta.getString("nombre_cliente"))+"%' )";
            }
        }else{
            nombre=ObtenerCampo(venta, "promotor").trim();
            String[] partes=nombre.split("\\s+");
            // Terpel envia NOMBRES APELLIDOS y en TNS esta APELLIDOS NOMBRES
            switch (partes.length) {
                case 4: nombre=partes[2]+" "+partes[3]+" "+partes[0]+" "+partes[1]; break;
                case 3: nombre=partes[1]+" "+partes[2]+" "+partes[0]; break;
                case 2: nombre=partes[1]+" "+partes[0]; break;
                default: break;
            }
            sqlString="select terid from terceros where nombre ='"+Sql(nombre)+"' ";
        }

        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            teridString=rs.getString("terid");
        }
        // el nombre en TNS puede estar en otro orden o incompleto
        if(teridString.isEmpty()){
            if(tipo.equals("cliente")){
                if (venta.getString("numero_documento_fe").equals("0")){
                    teridString=BuscarTerceroPorNombre(venta.getString("nombre_cliente"));
                }
            }else{
                teridString=BuscarTerceroPorNombre(ObtenerCampo(venta, "promotor"));
            }
        }
        if(teridString.isEmpty()){
            if(tipo.equals("cliente")){
                if (!venta.getString("numero_documento_fe").equals("0")){
                    sqlString="insert into terceros(nit,nittri,nombre,direcc1,telef1,telef2,tipodociden,zona1,cliente,fechcreac,clasificaid,ciudaneid)"+
                    "values('"+venta.getString("numero_documento_fe")+"','"+venta.getString("numero_documento_fe")+"'"+
                    ",'"+Sql(venta.getString("nombre_cliente"))+"','SIN DIRECCION'"+
                    ",'SIN TELEFONO','','C','1','S','now',1,1)";
                    Ejecutar(sqlString,"AL CREAR EL TERCERO "+venta.getString("numero_documento_fe")+" DE LA FACTURA "+venta.get("consecutivo_factura"));
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

    public static String ConsultaTercero(String documento)throws ClassNotFoundException, SQLException{
        String terid="";
        ResultSet rs = Tns.consultar("select terid from terceros where nit='"+documento+"'");
        while(rs.next()){
            terid=rs.getString("terid");
        }
        return terid;
    }
    private static final Set<String> PALABRAS_IGNORADAS = new HashSet<>(Arrays.asList(
        "S","A","SA","SAS","LTDA","LTD","EU","E","U","CIA","Y","DE","DEL","LA","LAS","LOS","EL","EN","CI","SC","ZOMAC"));

    // "Transportes Gomez  S.A.S." -> [TRANSPORTES, GOMEZ] (sin tildes ni signos)
    private static List<String> PalabrasNombre(String nombre){
        String limpio=Normalizer.normalize(nombre==null?"":nombre, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase()
            .replaceAll("[^A-Z0-9 ]", " ")
            .trim();
        List<String> palabras=new ArrayList<>();
        for (String palabra : limpio.split("\\s+")) {
            if(!palabra.isEmpty() && !PALABRAS_IGNORADAS.contains(palabra) && !palabras.contains(palabra)){
                palabras.add(palabra);
            }
        }
        return palabras;
    }

    private static int Levenshtein(String a,String b){
        int[] previo=new int[b.length()+1];
        int[] actual=new int[b.length()+1];
        for (int j = 0; j <= b.length(); j++) { previo[j]=j; }
        for (int i = 1; i <= a.length(); i++) {
            actual[0]=i;
            for (int j = 1; j <= b.length(); j++) {
                int costo=a.charAt(i-1)==b.charAt(j-1)?0:1;
                actual[j]=Math.min(Math.min(actual[j-1]+1, previo[j]+1), previo[j-1]+costo);
            }
            int[] tmp=previo; previo=actual; actual=tmp;
        }
        return previo[b.length()];
    }

    // dos palabras se consideran iguales si son identicas o, cuando son largas, difieren en una letra (RODRIGUES/RODRIGUEZ)
    private static boolean PalabrasParecidas(String a,String b){
        if(a.equals(b)){ return true; }
        return a.length()>=5 && b.length()>=5 && Levenshtein(a,b)<=1;
    }

    private static int PalabrasEnComun(List<String> a,List<String> b){
        int comunes=0;
        for (String palabra : a) {
            for (String otra : b) {
                if(PalabrasParecidas(palabra, otra)){ comunes++; break; }
            }
        }
        return comunes;
    }

    /**
     * Busca el tercero por nombre comparando por palabras (sin tildes, mayusculas,
     * sin S.A.S/LTDA, en cualquier orden). Acepta el candidato si:
     *  - contiene todas las palabras del nombre de Terpel, o
     *  - todas sus palabras estan en el nombre de Terpel y tiene al menos 2
     *    (nombre incompleto en TNS: "PEREZ GOMEZ JUAN" vs "JUAN CARLOS PEREZ GOMEZ"), o
     *  - coinciden al menos el 80% de las palabras.
     * Si hay empate entre varios terceros no elige ninguno para no cargar la venta a otro.
     */
    public static String BuscarTerceroPorNombre(String nombreBuscado) throws ClassNotFoundException, SQLException{
        List<String> palabrasBuscadas=PalabrasNombre(nombreBuscado);
        if(palabrasBuscadas.isEmpty()){ return ""; }

        StringBuilder filtro=new StringBuilder();
        for (String palabra : palabrasBuscadas) {
            if(palabra.length()<3){ continue; }
            if(filtro.length()>0){ filtro.append(" or "); }
            filtro.append("upper(nombre) like '%").append(palabra.substring(0, Math.min(palabra.length(), 4))).append("%'");
        }
        if(filtro.length()==0){ return ""; }

        String mejorTerid="";
        String mejorNombre="";
        double mejorPuntaje=0;
        boolean empate=false;
        ResultSet rs=Tns.consultar("select first 500 terid,nombre from terceros where "+filtro);
        while(rs.next()){
            List<String> palabrasTercero=PalabrasNombre(rs.getString("nombre"));
            if(palabrasTercero.isEmpty()){ continue; }
            int comunes=PalabrasEnComun(palabrasBuscadas, palabrasTercero);
            double dice=2.0*comunes/(palabrasBuscadas.size()+palabrasTercero.size());
            boolean contieneTodas=comunes==palabrasBuscadas.size();
            boolean terceroIncompleto=palabrasTercero.size()>=2
                && PalabrasEnComun(palabrasTercero, palabrasBuscadas)==palabrasTercero.size();
            if(!contieneTodas && !terceroIncompleto && dice<0.8){ continue; }
            if(dice>mejorPuntaje+0.0001){
                mejorPuntaje=dice;
                mejorTerid=rs.getString("terid");
                mejorNombre=rs.getString("nombre");
                empate=false;
            }else if(Math.abs(dice-mejorPuntaje)<=0.0001 && !rs.getString("terid").equals(mejorTerid)){
                empate=true;
            }
        }
        if(mejorTerid.isEmpty()){
            GuardarLog("NO SE ENCONTRO EL TERCERO POR NOMBRE: "+nombreBuscado);
            return "";
        }
        if(empate){
            GuardarLog("VARIOS TERCEROS PARECIDOS A "+nombreBuscado+", NO SE ASIGNO NINGUNO");
            return "";
        }
        if(!PalabrasNombre(mejorNombre).equals(palabrasBuscadas)){
            GuardarLog("TERCERO "+nombreBuscado+" ASIGNADO POR PARECIDO A "+mejorNombre);
        }
        return mejorTerid;
    }

    // LOGTERPEL no debe perder mensajes: se escapan comillas, se recorta el texto y,
    // si aun asi falla el insert, el mensaje queda en c:\tempo\terpel_log.txt
    public static void GuardarLog(String observacionString){
        String texto=observacionString==null?"":observacionString;
        GuardarLogArchivo(texto);
        if(texto.length()>250){ texto=texto.substring(0, 250); }
        try (Statement st=Tns.getStatement()) {
            st.executeUpdate("INSERT INTO LOGTERPEL(FECHA,OBSERVACIONES)values('now','"+Sql(texto)+"')");
        } catch (SQLException | ClassNotFoundException e) {
            GuardarLogArchivo("NO SE PUDO GUARDAR EN LOGTERPEL: "+e.getMessage());
        }
    }
    public static void GuardarLogArchivo(String texto){
        System.out.println(texto);
        String fechaHora=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        try (PrintWriter pw=new PrintWriter(new FileWriter("c:\\tempo\\terpel_log.txt", true))) {
            pw.println(fechaHora+" "+texto);
        } catch (IOException e) {
            System.out.println("NO SE PUDO ESCRIBIR EL LOG EN ARCHIVO: "+e.getMessage());
        }
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
