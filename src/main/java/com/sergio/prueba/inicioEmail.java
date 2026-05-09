package com.sergio.prueba;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class inicioEmail {
    private static ConnectionFirebird Tns=null;
    private static JsonObject object = new JsonObject();
    private static JsonObject lodeadentro = new JsonObject();
    private static JsonObject payment_form = new JsonObject();
    private static JsonObject legal_monetary_totals = new JsonObject();
    private static JsonObject consulta = new JsonObject();
    private static JSONObject obj = new JSONObject();
    private static String json =null;
    private static Parametros parametros=null;
    private static String Nit="";

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        File tempo = new File("c:/tempo/facelectronica.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String ruta=data.split("-")[0];
        String ip=data.split("-")[1];
        String numero=data.split("-")[2];
        String Prefijo=data.split("-")[3];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        consultarParametros();

        object.addProperty("company_idnumber",Nit.split("-")[0]);
        object.addProperty("prefix",Prefijo);
        object.addProperty("number",numero);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(object);

        System.out.println(json);
        System.out.println("Endpoint: "+parametros.getEndpointEmail().replaceAll("/ubl2.1", ""));
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(240, TimeUnit.SECONDS).readTimeout(240, TimeUnit.SECONDS).build();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType,json);
        Request request =null;
        request = new Request.Builder().url(parametros.getEndpointEmail().replaceAll("/ubl2.1", "")).method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").build();
        Response response = client.newCall(request).execute();
        System.out.println(response.code());
        Document doc = Jsoup.parse(response.body().string());
        obj = new JSONObject(response.body().string());
        System.out.println(obj);

        //String cufe="";

        //consultarKardexid(kardexid);
    }
    public static void consultarParametros() throws SQLException, ClassNotFoundException {
        String sql="select * from varios where (variab like '%DIAN%' or variab like '%GNITEMP%')";
        String token="";
        String endpoint="";
        String endpointDv="";
        String endpointCt="";
        String endpointEmail="";
        String cabecera="";
        String footer="";
        String sendmail="";
        String cantidad="";
        String atraso="";
        ResultSet rs = Tns.consultar(sql);

        while (rs.next()){
            System.out.println(rs.getString("contenido"));
            if (rs.getString("variab").equals("TOKENDIANVM")) { token = rs.getString("contenido"); }
            if (rs.getString("variab").equals("ENDPOINTDIANVM")) { endpoint = rs.getString("contenido"); }
            if (rs.getString("variab").equals("ENDPOINTCTDIANVM")) { endpointCt = rs.getString("contenido"); }
            if (rs.getString("variab").equals("ENDPOINTDVDIANVM")) { endpointDv = rs.getString("contenido"); }
            if (rs.getString("variab").equals("ENDPOINTEMDIANVM")) { endpointEmail = rs.getString("contenido"); }
            if (rs.getString("variab").equals("CABECERADIANVM")) { cabecera = rs.getString("contenido"); }
            if (rs.getString("variab").equals("FOOTERDIANVM")) { footer = rs.getString("contenido"); }
            if (rs.getString("variab").equals("DIANSENDMAIL")) { sendmail = rs.getString("contenido"); }
            if (rs.getString("variab").equals("DIANCANTIDAD")) { cantidad = rs.getString("contenido"); }
            if (rs.getString("variab").equals("DIANATRASO")) { atraso = rs.getString("contenido"); }
            if (rs.getString("variab").equals("GNITEMP")) { Nit = rs.getString("contenido"); }
        }
        endpointEmail=endpoint+"api/send-email-customer/NO";
        parametros = new Parametros(token,endpoint,endpointDv,endpointCt,endpointEmail,cabecera,footer,"","","",sendmail,cantidad,atraso,"");

    }
    public static void consultarKardexid(String kardexid) throws SQLException, ClassNotFoundException, IOException {
        kardex factura = null;
        String sql = "select distinct k.kardexid,iif(P.Contingencia='S',3,(iif(k.codcomp='FV',1,4))) tipoDocumento,k.numero,k.fecha,k.hora"
                + ",k.formapago,k.plazodias,k.formapago,k.fecvence,k.vrbase,k.vriva,iif((k.fpcontado+k.fpcredito+k.vrrcree)=k.neto,k.neto,(k.fpcontado+k.fpcredito)) total  "
                + ",t.nittri as nit,t.nombre,rpad(substring(t.telef1 from 1  for 10),7,'0') as telef1"
                + ",iif(t.direcc1 is null,'AVENIDA 1 NUMERO 7-02 CHAPINERO',t.direcc1) as direcc1,iif(t.emailemp is null"
                + ",t.email,t.emailemp) as"
                + " email,case t.tipodociden "
                + "when 'T' then '2' "
                + "when 'C' then '3' "
                + "when 'N' then '6' "
                + "when 'U' then '10' "
                + "else '3' end as tipodociden,"
                + "case t.natjuridica "
                + "when 'J' then '1' "
                + "when 'N' then '2' "
                + "else '1' end as tipoOrganizacion , '780' as municipio,p.resolucion,p.contingencia,p.prefe "
                +" ,k.NUMEROFACTANT numerodev,k.CUFEFACTANT cufedev,k.FECHAFACTANT fechadev,iif(m.codfactelect is null,2,m.codfactelect) motivo,k.observ as notes,k.exportacion,k.factorconv "
                +",c.nombre as municipio,c.departamento,ps.country_id "
                + "from kardex k "
                +" inner join prefijo p on p.codprefijo=k.codprefijo "
                +"inner join terceros t on t.terid=k.cliente "
                +"inner join ciudane c on c.ciudaneid=t.ciudaneid "
                +"left join pais ps on ps.paisid=c.paisid "
                +"left join motivodev m on m.motivodevid=k.motivodevid  "
                +"left join kardexself ks on ks.kardexid=k.kardexid "
                + "left join kardex k2 on k2.codprefijo||k2.numero=ks.nrofactdev "
                + " where t.nit<>'ANULA' and k.kardexid='"+kardexid+"'   and k.fecasentad is not null and k.codcomp in ('FV')";
        //+ " where t.nit<>'ANULA' and k.kardexid='"+kardexid+"'   and k.fecasentad is not null and k.codcomp in ('FV','DV','FC','DC')";
        System.out.println(sql);
        lodeadentro= new JsonObject();
        lodeadentro.remove("dv");
        ResultSet rs = Tns.consultar(sql);
        while(rs.next()){
            factura = new kardex(rs.getString("kardexid"),rs.getString("tipoDocumento"),rs.getString("numero"),rs.getString("fecha"),rs.getString("hora"),rs.getString("formapago"),rs.getString("plazodias"),rs.getString("fecvence"),rs.getBigDecimal("vrbase").setScale(2, RoundingMode.HALF_UP).toString(),rs.getString("vriva"),rs.getBigDecimal("total").setScale(2, RoundingMode.HALF_UP).toString(),rs.getString("nit"),rs.getString("telef1"),rs.getString("direcc1"),rs.getString("email"),rs.getString("tipodociden"),rs.getString("tipoOrganizacion"),rs.getString("municipio"),rs.getString("tipoOrganizacion"),rs.getString("nombre"),rs.getString("resolucion"),rs.getString("contingencia"),rs.getString("prefe"),rs.getString("numerodev"),rs.getString("cufedev"),rs.getString("fechadev"),rs.getString("motivo"),rs.getString("notes"),rs.getString("dv"),rs.getString("exportacion"),rs.getString("factorconv"),rs.getString("ciudad"),rs.getString("departamento"),rs.getString("country_id"));
        }
        switch (factura.getTipoDocument()){
            case "1":
                object.addProperty("number", Integer.parseInt(factura.getNumero().trim()));
                object.addProperty("type_document_id", Integer.parseInt(factura.getTipoDocument().trim()));
                object.addProperty("resolution_number", factura.getResolucion());
                object.addProperty("sendmail", true);
                object.addProperty("notes", factura.getNotes());
                break;
            case "3":
                object.addProperty("number", Integer.parseInt(factura.getNumero().trim()));
                object.addProperty("type_document_id", Integer.parseInt(factura.getTipoDocument().trim()));
                object.addProperty("AdditionalDocumentReferenceID",factura.getPrefe()+factura.getNumero());
                object.addProperty("AdditionalDocumentReferenceDate",factura.getFecha().replaceAll(" 00:00:00.0", ""));
                object.addProperty("AdditionalDocumentReferenceTypeDocument","01");
                object.addProperty("notes", factura.getNotes());
                break;
            case "4":
                JsonObject billing_reference = new JsonObject();
                billing_reference.addProperty("number",factura.getNumerodev());
                // billing_reference.addProperty("number","");
                billing_reference.addProperty("uuid",factura.getCufedev());
                // billing_reference.addProperty("uuid","");
                billing_reference.addProperty("issue_date",factura.getFechadev().replaceAll(" 00:00:00.0", ""));
                // billing_reference.addProperty("issue_date","");
                object.add("billing_reference", billing_reference);
                object.addProperty("discrepancyresponsecode", factura.getMotivo());

                object.addProperty("discrepancyresponsedescription", "DEVOLUCION DE VENTA");
                object.addProperty("number", Integer.parseInt(factura.getNumero().trim()));
                object.addProperty("type_document_id", Integer.parseInt(factura.getTipoDocument().trim()));
                object.addProperty("note", factura.getNotes());
                break;
            default:;
        }
        object.addProperty("date", factura.getFecha().replaceAll(" 00:00:00.0", ""));
        object.addProperty("time", factura.getHora()+":00");
        object.addProperty("establishment_email", "fehabitare@gmail.com");
        object.addProperty("sendmailtome", true);
        if (factura.getDocumento().split("-").length>1){
            lodeadentro.addProperty("identification_number", Integer.parseInt(factura.getDocumento().split("-")[0].trim()));
            lodeadentro.addProperty("dv", Integer.parseInt(factura.getDocumento().split("-")[1].trim()));
        }else{
            lodeadentro.addProperty("identification_number", Integer.parseInt(factura.getDocumento().trim()));
        }
        lodeadentro.addProperty("name", factura.getNombre());
        if (factura.getTelefono()==null){
            lodeadentro.addProperty("phone",5555555);
        }else{
            try{
                lodeadentro.addProperty("phone", Integer.parseInt(factura.getTelefono().trim()));
            }catch(Exception e){
                lodeadentro.addProperty("phone",5555555);
            }
        }
        lodeadentro.addProperty("address", factura.getDireccion());
        if (factura.getEmail()==null){
            lodeadentro.addProperty("email", "contadorpalustre@gmail.com");
        }else{
            if(factura.getEmail().equals("")){
                lodeadentro.addProperty("email", "contadorpalustre@gmail.com");
            }else{
                lodeadentro.addProperty("email", factura.getEmail());
            }
        }
        lodeadentro.addProperty("merchant_registration", "0000000-00");
        lodeadentro.addProperty("type_document_identification_id", Integer.parseInt(factura.getTipoIdentificacion().trim()));
        lodeadentro.addProperty("type_organization_id", Integer.parseInt(factura.getTipoOrganizacion().trim()));
        lodeadentro.addProperty("municipality_id", Integer.parseInt(factura.getMunicipio().trim()));
        lodeadentro.addProperty("type_regime_id", Integer.parseInt(factura.getTipoRegimen().trim()));
        object.add("customer", lodeadentro);
        if(!factura.getTipoDocument().equals("4")){

            if (factura.getFormapago().equals("CO")){
                payment_form.addProperty("payment_form_id", 1);
                payment_form.addProperty("payment_method_id", 10);
                payment_form.addProperty("payment_due_date", factura.getFecha().replaceAll(" 00:00:00.0", ""));
                // payment_form.addProperty("payment_due_date", "2020-09-06");
                payment_form.addProperty("duration_measure", 0);
            }else if (factura.getFormapago().equals("MU")){
                payment_form.addProperty("payment_form_id", 1);
                payment_form.addProperty("payment_method_id", 10);
                payment_form.addProperty("payment_due_date", factura.getFecha().replaceAll(" 00:00:00.0", ""));
                payment_form.addProperty("duration_measure", 0);

            }else {
                payment_form.addProperty("payment_form_id", 2);
                payment_form.addProperty("payment_method_id", 30);
                payment_form.addProperty("payment_due_date", factura.getFecvence().replaceAll(" 00:00:00.0", ""));
                // payment_form.addProperty("payment_due_date", "2020-09-06");
                payment_form.addProperty("duration_measure", factura.getPlazodias());

            }
            object.add("payment_form",payment_form);
        }
        if(factura.getTipoDocument().equals("1")) {
            object.add("with_holding_tax_total",holdins(factura.getKardexid()));
        }
        JsonArray allowance_charges = new JsonArray();
        object.add("allowance_charges",allowance_charges);
        legal_monetary_totals.addProperty("line_extension_amount", factura.getBase());
        legal_monetary_totals.addProperty("tax_exclusive_amount", factura.getBase());
        legal_monetary_totals.addProperty("tax_inclusive_amount", factura.getTotal());
        legal_monetary_totals.addProperty("allowance_total_amount", "0.00");
        legal_monetary_totals.addProperty("charge_total_amount", "0.00");
        legal_monetary_totals.addProperty("payable_amount", factura.getTotal());
        object.add("legal_monetary_totals",legal_monetary_totals);

        object.add("tax_totals",obtenerImpuestos(factura.getKardexid()));
         List<Articulo> items = articulos(factura.getKardexid());
        //List<Articulo> items = this.articulosGrande(factura.getKardexid());
        JsonArray invoice_lines = new JsonArray();
        for (int j=0;j<items.size();j++){
            JsonObject line= new JsonObject();
            Articulo articulo=null;
            articulo = items.get(j);
            line.addProperty("unit_measure_id", 70);
            line.addProperty("invoiced_quantity", articulo.getCantidad());

            if(articulo.getParcvta().equals("0.0")){
                line.addProperty("line_extension_amount", "1.0");
                line.addProperty("free_of_charge_indicator", true);
            }else{
                line.addProperty("line_extension_amount", articulo.getParcvta());
                line.addProperty("free_of_charge_indicator", false);
            }

            JsonArray allowance_charges_line = new JsonArray();
            JsonObject allowance_line= new JsonObject();
            allowance_line.addProperty("charge_indicator", false);
            allowance_line.addProperty("allowance_charge_reason", "DESCUENTO GENERAL");
            if (articulo.getPorcdescuento()=="0"){
                allowance_line.addProperty("amount", "0.00");
            }else{
                allowance_line.addProperty("amount", "0.00");
            }
            allowance_line.addProperty("base_amount", articulo.getParcvta());
            allowance_charges_line.add(allowance_line);
            line.add("allowance_charges",allowance_charges_line);
            JsonArray tax_totals_line = new JsonArray();
            JsonObject tax_line = new JsonObject();
            tax_line.addProperty("tax_id", 1);
            tax_line.addProperty("tax_amount", articulo.getVriva());
            tax_line.addProperty("taxable_amount", articulo.getParcvta());
            tax_line.addProperty("percent", articulo.getPorciva());
            tax_totals_line.add(tax_line);
            line.add("tax_totals", tax_totals_line);
            line.addProperty("description", articulo.getDescripcion());
            line.addProperty("code", articulo.getCodigo());
            line.addProperty("type_item_identification_id", 4);
            // Double precio=Double.parseDouble(articulo.getPreciobase())/Double.parseDouble(articulo.getCantidad());
            if (articulo.getPreciobase().equals("0.0")) {
                line.addProperty("price_amount", "1.0");
            }else{
                line.addProperty("price_amount", articulo.getPreciobase());
            }

            line.addProperty("base_quantity", 1);
            line.addProperty("reference_price_id", 1);
            //  line.addProperty("free_of_charge_indicator", true);
            invoice_lines.add(line);
        }
        if(factura.getTipoDocument().equals("4")){
            object.add("credit_note_lines", invoice_lines);
        }else{
            object.add("invoice_lines", invoice_lines);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        json = gson.toJson(object);

        System.out.println(json);
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(240, TimeUnit.SECONDS).readTimeout(240, TimeUnit.SECONDS).build();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType,json);
        Request request =null;
        if(!factura.getTipoDocument().equals("1")){
            // System.out.println("Pasando por el 1");
            request = new Request.Builder().url(parametros.getEndpointDv()).method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer "+parametros.getToken()).build();
        }else{
            // System.out.println("Pasando por el 2");
            request = new Request.Builder().url(parametros.getEndpoint()).method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer "+parametros.getToken()).build();
        }
        Response response = client.newCall(request).execute();
        // System.out.println(response.toString());
        System.out.println(response.code());
        // System.out.println(response.body());

        System.out.println(factura.getTipoDocument());
        obj = new JSONObject(response.body().string());
        System.out.println(obj);
        String cufe="";
        if(!factura.getTipoDocument().equals("1")){
            System.out.print(factura.getKardexid()+"cufe :"+obj.get("cude").toString());
            cufe=obj.get("cude").toString();
        }else{
            System.out.print(factura.getKardexid()+"cufe :"+obj.get("cufe").toString());
            cufe=obj.get("cufe").toString();
        }
        if (obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendBillSyncResponse").getJSONObject("SendBillSyncResult").get("IsValid")=="true"){
            System.out.println("true");
            actualizarFactura(factura.getKardexid(),"",cufe,"EXITOSA");
        }else{
            System.out.println("false");
            actualizarFactura(factura.getKardexid(),obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendBillSyncResponse").getJSONObject("SendBillSyncResult").get("StatusMessage").toString(),"","NO EXITOSA");
        }
    }
    public static void actualizarFactura(String kardexid,String mensaje,String cufe,String estado) throws SQLException {
        String sql="update kardex set mensajefe='"+mensaje+", cufe='"+cufe+",estadodian='"+estado+"' where kardexid='"+kardexid+"'";
        Tns.actualizar(sql);

    }
    public static List<Articulo> articulos(String kardexid) throws ClassNotFoundException, SQLException{
        List<Articulo> items = new ArrayList<Articulo>();
        String sql ="select c.codigo unidad,dk.canlista,round((dk.preciobase*dk.canlista),2) parcvta "
                + ",dk.descuento,dk.porciva,round(dk.precioiva*dk.canlista,2) precioiva,round(dk.preciovta,2) vrbase"
                + ",m.descrip,m.codigo "
                + "from material m "
                + "inner join dekardex dk on dk.matid=m.matid "
                + "left join codigosunidades c on c.codunidadid=m.codunidadid "
                + "where dk.kardexid='"+kardexid+"' and dk.parcvta>0 ";
        // System.out.println(sql);
        ResultSet rs = Tns.consultar(sql);
        Articulo item = null;
        while(rs.next()){
            item = new Articulo(rs.getString("unidad"),rs.getString("canlista"),rs.getBigDecimal("parcvta").setScale(2, RoundingMode.HALF_UP).toString(),rs.getString("descuento"),rs.getString("porciva"),rs.getString("precioiva"),rs.getBigDecimal("vrbase").setScale(2, RoundingMode.HALF_UP).toString(),rs.getString("descrip"),rs.getString("codigo"));
            items.add(item);
        }
        return items;
    }

    public static JsonArray holdins(String kardexid) throws ClassNotFoundException, SQLException{
        JsonArray with_holding_tax_total= new JsonArray();

        ArrayList<holding> impuestos = new ArrayList<holding>();
        String sql ="select * from (SELECT case c.rettipo\n" +
                "                    when 'C' then '7'\n" +
                "                    when 'R' then '6'\n" +
                "                    when 'I' then '5'\n" +
                "                    else '5' end as tax_id,dk.valordto,dk.baseretd,iif(c.rettipo='R',(dk.porcretd),(dk.porcretd/10)) porc\n" +
                "                    from dekardexdto dk\n" +
                "                    inner join concepto c on c.concid=dk.concid\n" +
                "                    where dk.kardexid='"+kardexid+"'\n" +
                "union all\n" +
                "select '6' as tax_id,k.vrrcree as valordto ,k.vrbase as baseretd,k.retcree as porc\n" +
                "from kardex k\n" +
                "where k.kardexid='"+kardexid+"' and vrrcree>0)";
        ResultSet rs = Tns.consultar(sql);
        while(rs.next()){
            JsonObject tax_line = new JsonObject();
            tax_line.addProperty("tax_id", rs.getString("tax_id"));
            tax_line.addProperty("tax_amount", rs.getString("valordto"));
            tax_line.addProperty("percent", rs.getString("porc"));
            tax_line.addProperty("taxable_amount", rs.getString("baseretd"));
            with_holding_tax_total.add(tax_line);
        }
        return with_holding_tax_total;
    }
    public static JsonArray obtenerImpuestos(String kardexid) throws ClassNotFoundException, SQLException{
        JsonArray tax_totals_line= new JsonArray();

        String sql=" select dk.porciva,round(sum(dk.preciobase*dk.canlista),2) base,round(sum(dk.precioiva*dk.canlista),2) as iva " +
                "from kardex k " +
                "inner join dekardex dk on k.kardexid=dk.kardexid " +
                "where k.kardexid='"+kardexid+"' " +
                "group by 1 ";
        ResultSet rs = Tns.consultar(sql);
        while(rs.next()){
            JsonObject tax_line = new JsonObject();
            tax_line.addProperty("tax_id", 1);
            if(rs.getString("iva").equals("0")){
                tax_line.addProperty("tax_amount", "0.00");
                tax_line.addProperty("percent", "0.00");
            }else{
                tax_line.addProperty("tax_amount", rs.getString("iva"));
                tax_line.addProperty("percent", rs.getString("porciva"));
            }
            tax_line.addProperty("taxable_amount", rs.getBigDecimal("base").setScale(2, RoundingMode.HALF_UP).toString());
            tax_totals_line.add(tax_line);
        }
        return tax_totals_line;
    }
}
