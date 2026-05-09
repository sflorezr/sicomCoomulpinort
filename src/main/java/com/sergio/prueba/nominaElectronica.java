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

public class nominaElectronica {
    private static Parametros parametros=null;
    private static ConnectionFirebird Tns=null;
    private static JsonObject object = new JsonObject();
    private static JsonObject novelty = new JsonObject();
    private static JsonObject period = new JsonObject();
    private static JsonObject worker = new JsonObject();
    private static JsonObject payment = new JsonObject();
    private static JsonArray payments_dates = new JsonArray();
    private static JsonObject payment_date = new JsonObject();
    private static JsonObject accrued = new JsonObject();
    private static JsonObject deductions = new JsonObject();
    private static JsonArray other_deductions = new JsonArray();
    private static JsonObject other_deduction = new JsonObject();
    private static JSONObject obj = new JSONObject();
    private static String fecini="";
    private static String fecfin="";
    private static String consecutivo="";
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
        nombre=data.split(";")[6];
        direccion=data.split(";")[7];
        telefono=data.split(";")[8];
        email=data.split(";")[9];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        consultarParametros();
        armar(personaid);
    }
    public static void consultarParametros() throws SQLException, ClassNotFoundException {
        String sql="select * from varios where variab like '%DIANVM'";
        String endpoint="";
        String token="";
        ResultSet rs = Tns.consultar(sql);

        while (rs.next()){
            System.out.println(rs.getString("contenido"));
            if (rs.getString("variab").equals("ENDPOINTDIANVM")) { endpoint = rs.getString("contenido"); }
            if (rs.getString("variab").equals("TOKENDIANVM")) { token = rs.getString("contenido"); }
        }
        parametros = new Parametros(token,endpoint,"endpointDv","endpointCt","endpointEmail","cabecera","footer","endpointFc","","","","","","");

    }
    public static void armar(String personaid) throws SQLException, ClassNotFoundException, IOException {
    empleado emp=null;
    String sql="SELECT P.CEDULA,P.FECINGRESO,P.DIASPER,P.NOMBRE1,p.estadodian,P.NOMBRE2,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n" +
            ",D.VALOR AS SALARIO,D2.VALOR AS TRANSPORTE,D3.VALOR AS EPS,D4.VALOR AS PENSION\n" +
            ",(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\n" +
            "WHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID)\n" +
            "FROM PERSONAL P\n" +
            "INNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CONCEPTOID=3\n" +
            "INNER JOIN DETALLE D2 ON D2.PERSONALID=P.PERSONALID AND D2.CONCEPTOID=4\n" +
            "INNER JOIN DETALLE D3 ON D3.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C ON C.CONCEPTOID=D3.CONCEPTOID AND C.DATOID=6\n" +
            "INNER JOIN DETALLE D4 ON D4.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C2 ON C2.CONCEPTOID=D4.CONCEPTOID AND C2.DATOID=7\n" +
            "INNER JOIN BANCO B ON B.BANCOID=P.BANCOID\n" +
            "WHERE P.PERSONALID='"+personaid+"' "     ;
        ResultSet rs = Tns.consultar(sql);



        if (rs.next()){
            emp = new empleado(rs.getString("cedula"),rs.getString("fecingreso")
                    ,rs.getString("diasper"),rs.getString("nombre1"),rs.getString("nombre2")
                    ,rs.getString("apellido1"),rs.getString("apellido2"),rs.getString("direccion")
                    ,rs.getString("basico"),rs.getString("banco"),rs.getString("cuenta")
                    ,rs.getString("salario"),rs.getString("transporte"),rs.getString("eps")
                    ,rs.getString("pension"),rs.getString("descuentos"),rs.getString("tipo"),rs.getString("estadodian"),"");
        }else
        {
            sql="SELECT P.CEDULA,P.FECINGRESO,P.DIASPER,P.NOMBRE1,P.NOMBRE2,p.estadodian,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n" +
                    ",D.VALOR AS SALARIO,D2.VALOR AS TRANSPORTE,D3.VALOR AS EPS,'0' AS PENSION\n" +
                    ",(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\n" +
                    "WHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID)\n" +
                    "FROM PERSONAL P\n" +
                    "INNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CONCEPTOID=3\n" +
                    "INNER JOIN DETALLE D2 ON D2.PERSONALID=P.PERSONALID AND D2.CONCEPTOID=4\n" +
                    "INNER JOIN DETALLE D3 ON D3.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C ON C.CONCEPTOID=D3.CONCEPTOID AND C.DATOID=6\n" +
                    "INNER JOIN BANCO B ON B.BANCOID=P.BANCOID\n" +
                    "WHERE P.PERSONALID='"+personaid+"' "     ;
            System.out.println(sql);
             rs = Tns.consultar(sql);
            if (rs.next()) {
                emp = new empleado(rs.getString("cedula"), rs.getString("fecingreso")
                        , rs.getString("diasper"), rs.getString("nombre1"), rs.getString("nombre2")
                        , rs.getString("apellido1"), rs.getString("apellido2"), rs.getString("direccion")
                        , rs.getString("basico"), rs.getString("banco"), rs.getString("cuenta")
                        , rs.getString("salario"), rs.getString("transporte"), rs.getString("eps")
                        , rs.getString("pension"), rs.getString("descuentos"),rs.getString("tipo"),rs.getString("estadodian"),"");
            }
        }
        object.addProperty("type_document_id",9);
        object.addProperty("establishment_name",nombre);
        object.addProperty("establishment_address",direccion);
        object.addProperty("establishment_phone",telefono);
        object.addProperty("establishment_municipality",780);
        object.addProperty("establishment_email",email);
        object.addProperty("head_note","PRUEBA DE TEXTO LIBRE QUE DEBE POSICIONARSE EN EL ENCABEZADO DE PAGINA DE LA REPRESENTACION GRAFICA DE LA FACTURA ELECTRONICA VALIDACION PREVIA DIAN");
        object.addProperty("foot_note","foot_note");
        novelty.addProperty("novelty",false);
        novelty.addProperty("uuidnov","");
        object.add("novelty",novelty);
        period.addProperty("admision_date",emp.getFecingreso().replaceAll(" 00:00:00.0", ""));
        period.addProperty("settlement_start_date",fecini);
        period.addProperty("settlement_end_date",fecfin);
        period.addProperty("worked_time",emp.getDiasper());
        period.addProperty("issue_date",fecfin);

        object.add("period",period);
        object.addProperty("worker_code",emp.getCedula());
        object.addProperty("prefix","NI");
        object.addProperty("consecutive",consecutivo);
        object.addProperty("payroll_period_id",4);
        object.addProperty("notes","PRUEBA DE ENVIO DE NOMINA ELECTRONICA");

        worker.addProperty("type_worker_id",1);
        worker.addProperty("sub_type_worker_id",1);
        worker.addProperty("payroll_type_document_identification_id",3);
        worker.addProperty("municipality_id",780);
        worker.addProperty("type_contract_id",1);
        worker.addProperty("high_risk_pension",false);
        worker.addProperty("identification_number",emp.getCedula());
        worker.addProperty("surname",emp.getApellido1());
        worker.addProperty("second_surname",emp.getApellido2());
        worker.addProperty("first_name",emp.getNombre1());
        worker.addProperty("address",emp.getDireccion());
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
        accrued.addProperty("transportation_allowance",emp.getTransporte()+"0");
        totalD=Float.parseFloat(emp.getSalario())+Float.parseFloat(emp.getTransporte());
        accrued.addProperty("accrued_total",totalD.toString()+"0");
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
        request = new Request.Builder().url(parametros.getEndpoint()).method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer "+parametros.getToken()).build();
        Response response = client.newCall(request).execute();
        System.out.println(response.code());
        obj = new JSONObject(response.body().string());
        System.out.println(obj);
        if (obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").get("IsValid").equals("true")){
            actualiazrEmpleado(personaid,"",obj.get("cune").toString(),"EXITOSA");
        }else{

            if (obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").getJSONObject("ErrorMessage").get("string").toString().equals("Regla: 90, Rechazo: Documento procesado anteriormente.")){
                actualiazrEmpleado(personaid,"",obj.get("cune").toString(),"EXITOSA");
            }else{
                actualiazrEmpleado(personaid,obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").getJSONObject("ErrorMessage").get("string").toString(),"","NO EXITOSA");
            }

        }
    }
    public static void actualiazrEmpleado(String personalid,String mensaje,String cune,String estado) throws SQLException {
        String sql="update personal set mensajefe='"+mensaje+"', cune='"+cune+"',estadodian='"+estado+"' where personalid='"+personalid+"'";
        System.out.println(sql);
        Tns.actualizar(sql);

    }
}
