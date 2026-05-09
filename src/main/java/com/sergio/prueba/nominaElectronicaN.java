package com.sergio.prueba;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class nominaElectronicaN {
    private static Parametros parametros=null;
    private static ConnectionFirebird Tns=null;
    private static JsonObject object = new JsonObject();
    private static JsonObject novelty = new JsonObject();
    private static JsonObject predecessor = new JsonObject();
    private static JsonObject period = new JsonObject();
    private static JsonObject worker = new JsonObject();
    private static JsonObject payment = new JsonObject();
    private static JsonArray payments_dates = new JsonArray();
    private static JsonObject payment_date = new JsonObject();
    private static JsonArray paid_vacation = new JsonArray();
    private static JsonArray service_bonus = new JsonArray();
    private static JsonArray severance = new JsonArray();

    private static JsonObject j_paid_vacation = new JsonObject();
    private static JsonObject j_service_bonus = new JsonObject();
    private static JsonObject j_severance = new JsonObject();

    private static JsonObject accrued = new JsonObject();
    private static JsonObject deductions = new JsonObject();
    private static JsonArray other_deductions = new JsonArray();
    private static JsonObject other_deduction = new JsonObject();
    private static JSONObject obj = new JSONObject();
    private static String fecini="";
    private static String fecfin="";
    private static String consecutivo="";
    private static String esPrima="";
    private static Float totalD;
    private static Float totalC;

    private static String nombre;
    private static String direccion;
    private static String telefono;
    private static String email;

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        File tempo = new File("c:/tempo/Nomelectronica.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String ruta=data.split(";")[0];
        String ip=data.split(";")[1];
        String personaid=data.split(";")[2];
        fecini=data.split(";")[3];
        fecfin=data.split(";")[4];
        consecutivo=data.split(";")[5];
        esPrima=data.split(";")[6];
        nombre=data.split(";")[7];
        direccion=data.split(";")[8];
        telefono=data.split(";")[9];
        email=data.split(";")[10];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        consultarParametros();
        armar(personaid);
    }
    public static void consultarParametros() throws SQLException, ClassNotFoundException {
        String sql="select * from varios where variab like '%DIAN%'";
        String endpoint="";
        String endpointDv="";
        String endpointC="";
        String token="";
        ResultSet rs = Tns.consultar(sql);

        while (rs.next()){
            System.out.println(rs.getString("contenido"));
            if (rs.getString("variab").equals("ENDPOINTDIANVM")) { endpoint = rs.getString("contenido"); }
            if (rs.getString("variab").equals("ENDPOINTDIANNOMINA")) { endpointDv = rs.getString("contenido"); }           
            if (rs.getString("variab").equals("TOKENDIANVM")) { token = rs.getString("contenido"); }
            if (rs.getString("variab").equals("ENDPOINTCDIANNOMINA")) { endpointC = rs.getString("contenido"); }
            
        }
        parametros = new Parametros(token,endpoint,endpointDv,endpointC,"endpointEmail","cabecera","footer","endpointFc","","","","","","");

    }
    public static void armar(String personaid) throws SQLException, ClassNotFoundException, IOException {
    empleado emp=null;
    Boolean esProvision=false;
        String sql="";

        ResultSet rs=null;
    if (esPrima.equals("1")){
        sql = "SELECT P.CEDULA,P.FECINGRESO,(P.DIASPER+P.DIASINC) AS DIASPER,P.NOMBRE1,p.estadodian,p.cune,P.NOMBRE2,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n" +
                ",D.VALOR AS SALARIO,0 AS TRANSPORTE,0 AS EPS,0 AS PENSION\n" +
                ",(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\n" +
                "WHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID),1 as tipo\n" +
                "FROM PERSONAL P\n" +
                "INNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CANTIDA IS NOT NULL\n" +
                "INNER JOIN BANCO B ON B.BANCOID=P.BANCOID\n" +
                "WHERE P.PERSONALID='" + personaid + "' ";
    }else {
        sql="select c.nomcargo from personal p inner join cargo c on c.cargoid=p.cargoid where p.personalid='"+personaid+"'";
        rs = Tns.consultar(sql);
        rs.next();
        if(rs.getString("nomcargo").toString().contains("APREND")){
            sql = "SELECT P.CEDULA,P.FECINGRESO,(P.DIASPER+P.DIASINC) AS DIASPER,P.NOMBRE1,P.NOMBRE2,p.estadodian,p.cune,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n" +
                    ",D.VALOR AS SALARIO,IIF(D2.VALOR IS NULL,0,D2.VALOR) AS TRANSPORTE,0 AS EPS,0 AS PENSION\n" +
                    ",(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\n" +
                    "WHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID),4 as tipo \n" +
                    "FROM PERSONAL P\n" +
                    "INNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CONCEPTOID=3\n" +
                    "LEFT JOIN DETALLE D2 ON D2.PERSONALID=P.PERSONALID AND D2.CONCEPTOID=4\n" +
                    "INNER JOIN BANCO B ON B.BANCOID=P.BANCOID\n" +
                    "WHERE P.PERSONALID='" + personaid + "' ";
        }else {
            sql = "SELECT P.CEDULA,P.FECINGRESO,(P.DIASPER+P.DIASINC) AS DIASPER,P.NOMBRE1,P.NOMBRE2,p.estadodian,p.cune,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n" +
                    ",D.VALOR AS SALARIO,IIF(D2.VALOR IS NULL,0,D2.VALOR) AS TRANSPORTE,D3.VALOR AS EPS,D4.VALOR AS PENSION\n" +
                    ",(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\n" +
                    "WHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID),1 as tipo \n" +
                    "FROM PERSONAL P\n" +
                    "INNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CANTIDA IS NOT NULL\n" +
                    "LEFT JOIN DETALLE D2 ON D2.PERSONALID=P.PERSONALID AND D2.CONCEPTOID=4\n" +
                    "INNER JOIN DETALLE D3 ON D3.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C ON C.CONCEPTOID=D3.CONCEPTOID AND C.DATOID=6\n" +
                    "INNER JOIN DETALLE D4 ON D4.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C2 ON C2.CONCEPTOID=D4.CONCEPTOID AND C2.DATOID=7\n" +
                    "INNER JOIN BANCO B ON B.BANCOID=P.BANCOID\n" +
                    "WHERE P.PERSONALID='" + personaid + "' ";
        }
    }
         rs = Tns.consultar(sql);



        if (rs.next()){
            emp = new empleado(rs.getString("cedula"),rs.getString("fecingreso")
                    ,rs.getString("diasper"),rs.getString("nombre1"),rs.getString("nombre2")
                    ,rs.getString("apellido1"),rs.getString("apellido2"),rs.getString("direccion")
                    ,rs.getString("basico"),rs.getString("banco"),rs.getString("cuenta")
                    ,rs.getString("salario"),rs.getString("transporte"),rs.getString("eps")
                    ,rs.getString("pension"),rs.getString("descuentos"),rs.getString("tipo"),rs.getString("estadodian"),rs.getString("cune"));
        }else
        {
            sql="SELECT P.CEDULA,P.FECINGRESO,(P.DIASPER+P.DIASINC) AS DIASPER,P.NOMBRE1,P.NOMBRE2,p.estadodian,P.APELLIDO1,p.cune,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n" +
                    ",D.VALOR AS SALARIO,D2.VALOR AS TRANSPORTE,D3.VALOR AS EPS,'0' AS PENSION\n" +
                    ",(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\n" +
                    "WHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID),1 as tipo\n" +
                    "FROM PERSONAL P\n" +
                    "INNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CONCEPTOID=3\n" +
                    "INNER JOIN DETALLE D2 ON D2.PERSONALID=P.PERSONALID AND D2.CONCEPTOID=4\n" +
                    "INNER JOIN DETALLE D3 ON D3.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C ON C.CONCEPTOID=D3.CONCEPTOID AND C.DATOID=6\n" +
                    "INNER JOIN BANCO B ON B.BANCOID=P.BANCOID\n" +
                    "WHERE P.PERSONALID='"+personaid+"' "     ;
            //System.out.println(sql);
             rs = Tns.consultar(sql);
            if (rs.next()) {
                emp = new empleado(rs.getString("cedula"), rs.getString("fecingreso")
                        , rs.getString("diasper"), rs.getString("nombre1"), rs.getString("nombre2")
                        , rs.getString("apellido1"), rs.getString("apellido2"), rs.getString("direccion")
                        , rs.getString("basico"), rs.getString("banco"), rs.getString("cuenta")
                        , rs.getString("salario"), rs.getString("transporte"), rs.getString("eps")
                        , rs.getString("pension"), rs.getString("descuentos"),rs.getString("tipo"),rs.getString("estadodian"),rs.getString("cune"));
            }
        }
        if(emp.getEstadodian().equals("EXITOSA")){
            object.addProperty("type_document_id",10);
            object.addProperty("type_note", 1);
        }else{
            object.addProperty("type_document_id",9);
        }
        
        object.addProperty("establishment_name",nombre);
        object.addProperty("establishment_address",direccion);
        object.addProperty("establishment_phone",telefono);
        object.addProperty("establishment_municipality",780);
        object.addProperty("establishment_email",email);
        object.addProperty("head_note","PRUEBA DE TEXTO LIBRE QUE DEBE POSICIONARSE EN EL ENCABEZADO DE PAGINA DE LA REPRESENTACION GRAFICA DE LA FACTURA ELECTRONICA VALIDACION PREVIA DIAN");
        object.addProperty("foot_note","foot_note");
        novelty.addProperty("novelty",false);
        novelty.addProperty("uuidnov",""); 
        if(!emp.getEstadodian().equals("EXITOSA")){
            object.add("novelty",novelty);
        }else{
            predecessor.addProperty("predecessor_number", consecutivo);
            predecessor.addProperty("predecessor_cune", emp.getCune());
            predecessor.addProperty("predecessor_issue_date",fecfin);
            object.add("predecessor", predecessor);
        }
        period.addProperty("admision_date",emp.getFecingreso().replaceAll(" 00:00:00.0", ""));
        period.addProperty("settlement_start_date",fecini);
        period.addProperty("settlement_end_date",fecfin);
        if(emp.getDiasper().equals("0.00")){
            period.addProperty("worked_time","15");
        }else{
            period.addProperty("worked_time",emp.getDiasper());
        }
        
        period.addProperty("issue_date",fecfin);

        object.add("period",period);
        object.addProperty("worker_code",emp.getCedula());
        object.addProperty("prefix","NI");
        if(emp.getEstadodian().equals("EXITOSA")){
            consecutivo=consultarConsecutivo();
            object.addProperty("consecutive",consecutivo);
        }else{
            object.addProperty("consecutive",consecutivo);
        }
        
        object.addProperty("payroll_period_id",4);
        object.addProperty("notes","ENVIO DE NOMINA ELECTRONICA");

        worker.addProperty("type_worker_id",emp.getTipo());
        worker.addProperty("sub_type_worker_id",1);
        worker.addProperty("payroll_type_document_identification_id",3);
        worker.addProperty("municipality_id",780);
        worker.addProperty("type_contract_id",1);
        worker.addProperty("high_risk_pension",false);
        worker.addProperty("identification_number",emp.getCedula());
        worker.addProperty("surname",emp.getApellido1());
        worker.addProperty("second_surname",emp.getApellido2());
        worker.addProperty("first_name",emp.getNombre1());
        if(emp.getDireccion().equals("")){
            worker.addProperty("address",direccion);
        }else{
            worker.addProperty("address",emp.getDireccion());
        }
        
        worker.addProperty("integral_salarary",false);
        worker.addProperty("salary",emp.getBasico()+"0");

        object.add("worker",worker);

        payment.addProperty("payment_method_id",10);
        payment.addProperty("bank_name",emp.getBanco());
        payment.addProperty("account_type","AHORROS");
        payment.addProperty("account_number",emp.getCuenta());

        object.add("payment",payment);
        payment_date.addProperty("payment_date",fecfin);
        payments_dates.add(payment_date);

        object.add("payment_dates",payments_dates);
        accrued.addProperty("worked_days",Integer.parseInt(emp.getDiasper().trim().replaceAll(".00","")));
        accrued.addProperty("salary",emp.getSalario()+"0");
        if(!emp.getTransporte().equals("0.0")){ accrued.addProperty("transportation_allowance",emp.getTransporte()+"0"); }        
        totalD=Float.parseFloat(emp.getSalario())+Float.parseFloat(emp.getTransporte());
        accrued.addProperty("accrued_total",totalD.toString()+"0");
        rs=Tns.consultar("Select pr.codparafis, pr.Nomparafis\r\n" + //
                        ", pr.porcen, sum(dp.valor) valor, (sum(dp.valor)/(pr.porcen/100)) BASE\r\n" + //
                        "From DetParaf dp\r\n" + //
                        "inner join parafiscales pr on dp.parafisid=pr.parafisid\r\n" + //
                        "inner join personal p on p.personalid=dp.personalid\r\n" + //
                        "Where pr.porcen<>0 and  (Tipoparafis = 'R' )\r\n" + //
                        "and dp.personalid='"+personaid+"' Group by 1,2,3");
                        while (rs.next()){
                            esProvision=true;
                            switch (rs.getString("codparafis")) {
                                case "PR04":
                                    j_paid_vacation.addProperty("quantity", 1);
                                    j_paid_vacation.addProperty("payment", rs.getString("valor"));
                                    paid_vacation.add(j_paid_vacation);
                                    break;
                                case "PR03":
                                    j_service_bonus.addProperty("quantity", 30);
                                    j_service_bonus.addProperty("payment", rs.getString("valor"));
                                    j_service_bonus.addProperty("paymentNS", 0);
                                    service_bonus.add(j_service_bonus);
                                    break;     
                                case "PR02":
                                    j_severance.addProperty("percentage", rs.getString("porcen"));
                                    j_severance.addProperty("interest_payment", rs.getString("valor"));
                                    break;
                                case "PR01":
                                    j_severance.addProperty("payment", rs.getString("valor"));
                                break;                                    
                                default:
                                    break;
                            }
                        }
        if(esProvision.equals(true)){
            severance.add(j_severance);
            accrued.add("paid_vacation", paid_vacation);
            accrued.add("service_bonus", service_bonus);
            accrued.add("severance", severance);
        }
        object.add("accrued",accrued);
        
        deductions.addProperty("eps_type_law_deductions_id",1);
        deductions.addProperty("eps_deduction",emp.getEps()+"0");
        deductions.addProperty("pension_type_law_deductions_id",5);
        deductions.addProperty("pension_deduction",emp.getPension()+"0");
        if(!emp.getDescuentos().equals("0.0")){
            other_deduction.addProperty("other_deduction",emp.getDescuentos()+"0");
            other_deductions.add(other_deduction);
            deductions.add("other_deductions",other_deductions);
        }

        totalC=Float.parseFloat(emp.getEps())+Float.parseFloat(emp.getPension())+Float.parseFloat(emp.getDescuentos());
        deductions.addProperty("deductions_total",totalC.toString()+"0");
        object.add("deductions",deductions);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(object);

        System.out.println(json);

        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(240, TimeUnit.SECONDS).readTimeout(240, TimeUnit.SECONDS).build();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType,json);
        Request request =null;
        if(emp.getEstadodian().equals("EXITOSA")){
            request = new Request.Builder().url(parametros.getEndpointDv()).method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer "+parametros.getToken()).build();
        }else{
            request = new Request.Builder().url(parametros.getEndpoint()).method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer "+parametros.getToken()).build();
        }        
        Response response = client.newCall(request).execute();
        System.out.println(response.code());
        obj = new JSONObject(response.body().string());
        System.out.println(obj);
        if (obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").get("IsValid").equals("true")){
            actualiazrEmpleado(personaid,"",obj.get("cune").toString(),"EXITOSA");
            Tns.actualizar("update personal set consecutivo='"+consecutivo+"' where personalid='"+personaid+"'");            
        }else{

            if (obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").getJSONObject("ErrorMessage").get("string").toString().equals("Regla: 90, Rechazo: Documento procesado anteriormente.")){
                String cune="";
                cune=obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").getString("StatusDescription");
                cune=cune.split("CUNE")[1].trim();
                actualiazrEmpleado(personaid,"",cune.toString(),"EXITOSA");
                Tns.actualizar("update personal set consecutivo='"+consecutivo+"' where personalid='"+personaid+"'");
            }else{
                if(!emp.getEstadodian().equals("EXITOSA")){
                    actualiazrEmpleado(personaid,obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").getJSONObject("ErrorMessage").get("string").toString(),"","NO EXITOSA");
                }                
            }

        }
    }
    public static String consultarConsecutivo() throws IOException{
        String consecutivo="";
        JsonObject bodyJson = new JsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();           
        bodyJson.addProperty("type_document_id", 9);
        bodyJson.addProperty("prefix", "NI");
        String json = gson.toJson(bodyJson);     
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(240, TimeUnit.SECONDS).readTimeout(240, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType,json);
        Request request =null;
        request = new Request.Builder().url(parametros.endpointCt).method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer "+parametros.getToken()).build();
        Response response = client.newCall(request).execute();
        obj = new JSONObject(response.body().string());
        consecutivo=obj.get("number").toString();
        return consecutivo;
    }
    public static void actualiazrEmpleado(String personalid,String mensaje,String cune,String estado) throws SQLException {
        String sql="update personal set mensajefe='"+mensaje+"', cune='"+cune+"',estadodian='"+estado+"' where personalid='"+personalid+"'";
        System.out.println(sql);
        Tns.actualizar(sql);

    }
}
