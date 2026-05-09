package com.sergio.prueba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.firebirdsql.jdbc.FBSQLException;
import org.json.JSONObject;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

public class NominaMasiva {
    private static ConnectionFirebird Maestro = null;
    private static ConnectionFirebird Periodo = null;

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
    private static String fecini = "";
    private static String fecfin = "";
    private static String consecutivo = "";
    private static String esPrima = "";
    private static Float totalD;
    private static Float totalC;
    private static String cune="";    
    private static String endpoint = "";
    private static String token = "";
    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        File tempo = new File("c:/tempo/NominaMasiva.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data = "";
        String ruta = "";
        String ip = "";
        String sql = "";
        ResultSet rs = null;
        ResultSet rsP = null;
        ResultSet rsPx = null;
        Integer consecivo=0;
        esPrima="0";


        for(int i = 0; i < br.read(); ++i) {
            data = br.readLine();
            ruta = data.split("-")[0];
            ip = data.split("-")[1];
            Maestro =null;
            Maestro = new ConnectionFirebird(ip, ruta, "SYSDBA", "masterkey", "3050");
            sql="select contenido from varios where variab='CONSECUTIVONE'";
            rs= Maestro.consultar(sql);
            if (rs.next()){
               consecivo=Integer.parseInt(rs.getString("contenido"));
            }
            
            sql ="select * from periodos where pendiente is null ";
            rs = Maestro.consultar(sql);
            while (rs.next()){
                fecini=rs.getString("perdesde").split(" ")[0];
                fecfin=rs.getString("perhasta").split(" ")[0];
                System.out.println(ruta.replaceAll("MAESTRO.GDB","")+rs.getString("PERARCHIVO"));
                Periodo=null;
                Periodo = new ConnectionFirebird(ip, ruta.replaceAll("MAESTRO.GDB","")+rs.getString("PERARCHIVO"), "SYSDBA", "masterkey", "3050");
                sql="alter table personal add reintento varchar(2)";
                try {
                  Periodo.actualizar(sql);
                } catch (FBSQLException e) {
                  // TODO: handle exception
                }
                sql="select * from personal where inactivo is NULL and (estadodian<>'EXITOSA' OR ESTADODIAN IS null)";
                rsP = null;
                rsP = Periodo.consultar(sql);
                while (rsP.next()){
                    rsPx=null;
                    sql = "select * from varios where variab like '%DIANVM'";
                    rsPx = Periodo.consultar(sql);
                    while (rsPx.next()) {
                       switch (rsPx.getString("variab")) {
                        case "ENDPOINTDIANVM":endpoint=rsPx.getString("contenido");
                           break;
                        case "TOKENDIANVM":token=rsPx.getString("contenido");                            
                            break;                       
                        default:
                            break;
                       } 
                    }
                    System.out.println(rs.getString("PERARCHIVO")+":"+rsP.getString("nombre")+" "+rsP.getString("cune")+" "+rsP.getString("consecutivo"));
                    if(rsP.getString("consecutivo") == null){
                     armar(rsP.getInt("personalid"),Periodo,consecivo);
                     consecivo=consecivo+1;
                     Maestro.actualizar("update varios set contenido="+consecivo+" where variab='CONSECUTIVONE'");
                    }else{
                     armar(rsP.getInt("personalid"),Periodo,Integer.parseInt(rsP.getString("consecutivo")));
                    }

                }

            }
        }
    }
    public static void armar(Integer personaid,ConnectionFirebird Tns,Integer consecivo) throws SQLException, ClassNotFoundException, IOException {
        empleado emp = null;
        String sql = "";
        ResultSet rs = null;
        sql="update personal set consecutivo='"+String.valueOf(consecivo)+"' where personalid ='"+personaid+"'";
        Tns.actualizar(sql);
        if (esPrima.equals("1")) {
           sql = "SELECT P.CEDULA,P.FECINGRESO,P.DIASPER AS DIASPER,P.NOMBRE1,P.NOMBRE2,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n,D.VALOR AS SALARIO,0 AS TRANSPORTE,0 AS EPS,0 AS PENSION\n,(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\nWHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID),1 as tipo\nFROM PERSONAL P\nINNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CANTIDA IS NOT NULL\nINNER JOIN BANCO B ON B.BANCOID=P.BANCOID\nWHERE P.PERSONALID='" + personaid + "' ";
        } else {
           sql = "select c.nomcargo from personal p inner join cargo c on c.cargoid=p.cargoid where p.personalid='" + personaid + "'";
           rs = Tns.consultar(sql);
           rs.next();
           if (rs.getString("nomcargo").toString().contains("APREND")) {
              sql = "SELECT P.CEDULA,P.FECINGRESO,P.DIASPER,P.NOMBRE1,P.NOMBRE2,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n,D.VALOR AS SALARIO,IIF(D2.VALOR IS NULL,0,D2.VALOR) AS TRANSPORTE,0 AS EPS,0 AS PENSION\n,(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\nWHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID),4 as tipo \nFROM PERSONAL P\nINNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CONCEPTOID=3\nLEFT JOIN DETALLE D2 ON D2.PERSONALID=P.PERSONALID AND D2.CONCEPTOID=4\nINNER JOIN BANCO B ON B.BANCOID=P.BANCOID\nWHERE P.PERSONALID='" + personaid + "' ";
           } else {
              sql = "SELECT P.CEDULA,P.FECINGRESO,P.DIASPER,P.NOMBRE1,P.NOMBRE2,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n,D.VALOR AS SALARIO,IIF(D2.VALOR IS NULL,0,D2.VALOR) AS TRANSPORTE,D3.VALOR AS EPS,D4.VALOR AS PENSION\n,(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\nWHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID),1 as tipo \nFROM PERSONAL P\nINNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CANTIDA IS NOT NULL\nLEFT JOIN DETALLE D2 ON D2.PERSONALID=P.PERSONALID AND D2.CONCEPTOID=4\nINNER JOIN DETALLE D3 ON D3.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C ON C.CONCEPTOID=D3.CONCEPTOID AND C.DATOID=6\nINNER JOIN DETALLE D4 ON D4.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C2 ON C2.CONCEPTOID=D4.CONCEPTOID AND C2.DATOID=7\nINNER JOIN BANCO B ON B.BANCOID=P.BANCOID\nWHERE P.PERSONALID='" + personaid + "' ";
           }
        }
  
        rs = Tns.consultar(sql);
        if (rs.next()) {
           emp = new empleado(rs.getString("cedula"), rs.getString("fecingreso"), rs.getString("diasper"), rs.getString("nombre1"), rs.getString("nombre2"), rs.getString("apellido1"), rs.getString("apellido2"), rs.getString("direccion"), rs.getString("basico"), rs.getString("banco"), rs.getString("cuenta"), rs.getString("salario"), rs.getString("transporte"), rs.getString("eps"), rs.getString("pension"), rs.getString("descuentos"), rs.getString("tipo"),"","");
        } else {
           sql = "SELECT P.CEDULA,P.FECINGRESO,P.DIASPER,P.NOMBRE1,P.NOMBRE2,P.APELLIDO1,P.APELLIDO2,P.DIRECCION,P.BASICO,B.NOMBANCO AS BANCO,P.CTATRAB AS CUENTA\n,D.VALOR AS SALARIO,D2.VALOR AS TRANSPORTE,D3.VALOR AS EPS,'0' AS PENSION\n,(SELECT IIF(SUM(DD.VALOR) IS NULL,0,SUM(DD.VALOR)) AS DESCUENTOS FROM DETALLE DD INNER JOIN CONCEPTO CC ON CC.CONCEPTOID=DD.CONCEPTOID\nWHERE CC.DATOID NOT IN (6,7) AND CC.TIPODC='C' AND DD.PERSONALID=P.PERSONALID),1 as tipo\nFROM PERSONAL P\nINNER JOIN DETALLE D ON D.PERSONALID=P.PERSONALID AND D.CONCEPTOID=3\nINNER JOIN DETALLE D2 ON D2.PERSONALID=P.PERSONALID AND D2.CONCEPTOID=4\nINNER JOIN DETALLE D3 ON D3.PERSONALID=P.PERSONALID INNER JOIN CONCEPTO C ON C.CONCEPTOID=D3.CONCEPTOID AND C.DATOID=6\nINNER JOIN BANCO B ON B.BANCOID=P.BANCOID\nWHERE P.PERSONALID='" + personaid + "' ";
           rs = Tns.consultar(sql);
           if (rs.next()) {
              emp = new empleado(rs.getString("cedula"), rs.getString("fecingreso"), rs.getString("diasper"), rs.getString("nombre1"), rs.getString("nombre2"), rs.getString("apellido1"), rs.getString("apellido2"), rs.getString("direccion"), rs.getString("basico"), rs.getString("banco"), rs.getString("cuenta"), rs.getString("salario"), rs.getString("transporte"), rs.getString("eps"), rs.getString("pension"), rs.getString("descuentos"), rs.getString("tipo"),"","");
           }
        }
  
        object.addProperty("type_document_id", 9);
        object.addProperty("establishment_name", "DEPOSITO HABITARE CUCUTA SAS");
        object.addProperty("establishment_address", "AV 1 7-02 CHAPINERO");
        object.addProperty("establishment_phone", "3143302524");
        object.addProperty("establishment_municipality", 780);
        object.addProperty("establishment_email", "contadorpalustre@gmail.com");
        object.addProperty("head_note", "PRUEBA DE TEXTO LIBRE QUE DEBE POSICIONARSE EN EL ENCABEZADO DE PAGINA DE LA REPRESENTACION GRAFICA DE LA FACTURA ELECTRONICA VALIDACION PREVIA DIAN");
        object.addProperty("foot_note", "foot_note");
        novelty.addProperty("novelty", false);
        novelty.addProperty("uuidnov", "");
        object.add("novelty", novelty);
        period.addProperty("admision_date", emp.getFecingreso().replaceAll(" 00:00:00.0", ""));
        period.addProperty("settlement_start_date", fecini);
        period.addProperty("settlement_end_date", fecfin);
        period.addProperty("worked_time", emp.getDiasper());
        period.addProperty("issue_date", fecfin);
        object.add("period", period);
        object.addProperty("worker_code", emp.getCedula());
        object.addProperty("prefix", "NI");
        object.addProperty("consecutive", String.valueOf(consecivo));
        object.addProperty("payroll_period_id", 4);
        object.addProperty("notes", "PRUEBA DE ENVIO DE NOMINA ELECTRONICA");
        worker.addProperty("type_worker_id", emp.getTipo());
        worker.addProperty("sub_type_worker_id", 1);
        worker.addProperty("payroll_type_document_identification_id", 3);
        worker.addProperty("municipality_id", 780);
        worker.addProperty("type_contract_id", 1);
        worker.addProperty("high_risk_pension", false);
        worker.addProperty("identification_number", emp.getCedula());
        worker.addProperty("surname", emp.getApellido1());
        worker.addProperty("second_surname", emp.getApellido2());
        worker.addProperty("first_name", emp.getNombre1());
        if (emp.getDireccion().equals("")){
         worker.addProperty("address", "SIN DIRECCION");
        }else {
         worker.addProperty("address", emp.getDireccion());
        }
        worker.addProperty("integral_salarary", false);
        worker.addProperty("salary", emp.getBasico() + "0");
        object.add("worker", worker);
        payment.addProperty("payment_method_id", 10);
        payment.addProperty("bank_name", emp.getBanco());
        payment.addProperty("account_type", "AHORROS");
        payment.addProperty("account_number", emp.getCuenta());
        object.add("payment", payment);
        payment_date=new JsonObject();
        payment_date.addProperty("payment_date", fecfin);
        payments_dates.add(payment_date);
        object.add("payment_dates", payments_dates);
        accrued.addProperty("worked_days", Integer.parseInt(emp.getDiasper().trim().replaceAll(".00", "")));
        accrued.addProperty("salary", emp.getSalario() + "0");
        accrued.addProperty("transportation_allowance", emp.getTransporte() + "0");
        totalD = Float.parseFloat(emp.getSalario()) + Float.parseFloat(emp.getTransporte());
        accrued.addProperty("accrued_total", totalD.toString() + "0");
        object.add("accrued", accrued);
        deductions.addProperty("eps_type_law_deductions_id", 1);
        deductions.addProperty("eps_deduction", emp.getEps() + "0");
        deductions.addProperty("pension_type_law_deductions_id", 5);
        deductions.addProperty("pension_deduction", emp.getPension() + "0");
        if (!emp.getDescuentos().equals("0.0")) {
         other_deduction=new JsonObject();
           other_deduction.addProperty("other_deduction", emp.getDescuentos() + "0");
           other_deductions.add(other_deduction);
           deductions.add("other_deductions", other_deductions);
        }
  
        totalC = Float.parseFloat(emp.getEps()) + Float.parseFloat(emp.getPension()) + Float.parseFloat(emp.getDescuentos());
        deductions.addProperty("deductions_total", totalC.toString() + "0");
        object.add("deductions", deductions);
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
        String json = gson.toJson(object);
        System.out.println(json);
        OkHttpClient client = (new OkHttpClient()).newBuilder().connectTimeout(240L, TimeUnit.SECONDS).readTimeout(240L, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);
        Request request = null;
        request = (new Request.Builder()).url(endpoint).method("POST", body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer " + token).build();
        Response response = client.newCall(request).execute();
        System.out.println(response.code());
        obj = new JSONObject(response.body().string());
        //System.out.println(obj);
        if (obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").get("IsValid").equals("true")) {
           actualiazrEmpleado(personaid, "", obj.get("cune").toString(), "EXITOSA","NO",Tns);
        } else if (obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").getJSONObject("ErrorMessage").get("string").toString().equals("Regla: 90, Rechazo: Documento procesado anteriormente.")) {
           cune=obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").getString("StatusDescription");
           cune=cune.split(" ")[5];
           actualiazrEmpleado(personaid, "", cune, "EXITOSA","SI",Tns);
        } else {                       
           actualiazrEmpleado(personaid, obj.getJSONObject("ResponseDian").getJSONObject("Envelope").getJSONObject("Body").getJSONObject("SendNominaSyncResponse").getJSONObject("SendNominaSyncResult").getJSONObject("ErrorMessage").get("string").toString(), "", "NO EXITOSA","",Tns);
        }
  
     }
  
     public static void actualiazrEmpleado(Integer personalid, String mensaje, String cune, String estado,String reintento,ConnectionFirebird Tns) throws SQLException {
        String sql = "update personal set mensajefe='" + mensaje + "',reintento='"+reintento+"', cune='" + cune + "',estadodian='" + estado + "' where personalid='" + personalid + "'";
        System.out.println(sql);
        Tns.actualizar(sql);
     }    
}
