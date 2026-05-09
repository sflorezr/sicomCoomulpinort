package com.test.java.com.sergio.facelectronica;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;

import com.sergio.prueba.ConnectionFirebird;
import com.sergio.prueba.ConnectionSQL;

public class ConsultaIngreso {
    private static ConnectionFirebird Tns=null;
    private static ConnectionSQL Sql=null;
    private static String host="";
    private static String bdName="";
    private static String usuario="";
    private static String password="";
    private static String port="";
    
    public static void main(String[] args) throws Exception {

        File tempo = new File("c:\\tempo\\cenabastos.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String ruta=data.split("\\|")[0];
        String ip=data.split("\\|")[1];
        String fecha=data.split("\\|")[2];
        String insertar=data.split("\\|")[3];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario();
        ConsultarPedidos(fecha,insertar);
    }
    public static void ConsultarPedidos(String fecha,String insertar) throws Exception {
        String numero="";
        String teridString="";
        String vendedorIdString="";
        String fechaVentaString="";
        String kardexidString="";
        String matidString="";
        String codcomp="";
        String prefijo="";           
        String observaciones="";
        String hora="";
        String sqlString="";
        String tipoDoc,Documento;
        Integer i=0;
        Sql = new ConnectionSQL(host, bdName, usuario, password, port);
        ResultSet rs = null;
        if (insertar.equals("N")){
            rs=Sql.consultar("select count(i.IngrFac) as cuenta "+
                                    " from Ingreso i "+
                                    " inner join Vehiculos v on v.VehPla = i.VehPla "+
                                    " inner join Categorias c on c.CatCod =v.CatCod  "+
                                    " where i.IngrFec ='"+fecha+"' and i.IngrFac <>''") ;
        
            if (rs.next()){
                System.out.println("Se cargaran "+rs.getString("cuenta")+" facturas");
                Tns.actualizar("delete from varios where variab='CANTIDADSQL'");                
                sqlString="insert into varios (contenido,variab) values('"+rs.getString("cuenta")+"','CANTIDADSQL')";  
                Tns.actualizar(sqlString);
            }
        }else{
            sqlString="delete from varios where variab='CANTIDADSUBIDA'";
            Tns.actualizar(sqlString);
            sqlString="insert into varios (contenido,variab) values('0','CANTIDADSUBIDA')";  
            Tns.actualizar(sqlString);    
            rs=Sql.consultar("select   i.IngrFac  ,RIGHT (IngrFac,7) as IngrCod ,i.IngrObsAnu ,i.ubiCod,i.IngrFec ,i.IngrVal ,i.VehPla ,i.IngrEdsTCod ,i.IngrEdsTDes,i.UsuCod,u.UsuId,c.CatCod "+
                            " from Ingreso i "+
                            " inner join Vehiculos v on v.VehPla = i.VehPla "+
                            " inner join Usuarios u on u.UsuCod =i.UsuCod "+
                            " inner join Categorias c on c.CatCod =v.CatCod  "+
                            " where i.IngrFec ='"+fecha+"' and i.IngrFac <>'' ") ;
            i=0;
            while (rs.next()){
                i=i+1;
                numero="";
                teridString ="";
                vendedorIdString ="";
                fechaVentaString = "";
                fechaVentaString = "";
                observaciones = "";
                tipoDoc= "";
                Documento = "";                
                numero=rs.getString("IngrCod");
                if (ConsultarFactura(numero)){
                    GuardarLog("la factura "+numero+" ya existe en Tns");
                }else{
                    teridString = ConsultarTerid(rs.getString("IngrEdsTDes"));
                    vendedorIdString = ConsultarTerid(rs.getString("UsuId"));
                    fechaVentaString = rs.getString("IngrFec");
                    fechaVentaString = fechaVentaString.substring(5, 7)+'/'+fechaVentaString.substring(8, 10)+'/'+fechaVentaString.substring(0, 4);
                    observaciones = rs.getString("IngrObsAnu");
                    tipoDoc= rs.getString("UbiCod");
                    Documento = rs.getString("VehPla");                    
                    sqlString="insert into kardex(codcomp,codprefijo,numero,observ,fecha,periodo,cenid,areadid,sucid,cliente,vendedor,formapago"+
                    ",bcoid,ajustebase,ajusteiva,ajusteneto,vrbase,vriva,total,fpcontado,fpcredito,despachar_a,factorconv,vrtotal,neto,netobase,hora,tipodoc,documento)"+
                    "values('FV'"+
                    ",'PE'"+
                    ",'"+numero.trim()+"'"+
                    ",'"+observaciones+"'"+
                    ",'"+fechaVentaString+"'"+
                    ",'"+fechaVentaString.substring(0, 2)+"'"+
                    ",1"+
                    ",1"+
                    ",1"+
                    ",'"+teridString+"'"+
                    ",'"+vendedorIdString+"'"+
                    ",'CO'"+
                    ",1"+
                    ",0"+
                    ",0"+
                    ",0"+
                    ",'"+rs.getString("IngrVal")+"'"+
                    ",0"+
                    ",'"+rs.getString("IngrVal")+"'"+
                    ",'"+rs.getString("IngrVal")+"'"+
                    ",0"+
                    ",'"+teridString+"'"+
                    ",1"+
                    ",'"+rs.getString("IngrVal")+"'"+
                    ",'"+rs.getString("IngrVal")+"'"+
                    ",'"+rs.getString("IngrVal")+"'"+
                    ",null"+
                    ",'"+rs.getString("ubiCod")+"'"+
                    ",'"+rs.getString("VehPla")+"'"+
                    ")";                    
                    Tns.actualizar(sqlString);                    
                    kardexidString = ConsultarFacturaKardexid(numero);
                    matidString=ConsultarMatid(rs.getString("CatCod"));
                    sqlString="INSERT INTO DEKARDEX(kardexid,Bodid,matid,Prioridad,remtotfac,tipund,porciva,descuento,canlista,canmat,preciolista,preciovta," + 
                              "  preciobase,precioiva,precioneto,parcvta, preciotasa ) "+
                              "values ("+
                               " "+kardexidString+", "+
                               "1,"+
                               " "+matidString+", "+
                               "7,0,'D',19,0,1,1"+
                               ",'"+rs.getString("IngrVal")+"'"+
                               ",'"+rs.getString("IngrVal")+"'"+
                               ",0,0"+
                               ",'"+rs.getString("IngrVal")+"'"+
                               ",'"+rs.getString("IngrVal")+"'"+
                               ",'"+rs.getString("IngrVal")+"'"+
                              ")";
                              Tns.actualizar(sqlString);                                                            
                }
                Tns.actualizar("update varios set contenido='"+Integer.toString(i)+"' where variab='CANTIDADSUBIDA'");                                
            }
        }
    }
    public static String ConsultarMatid(String codigo) throws ClassNotFoundException, SQLException{
        String matid="1";
        String sqlString = "select matid from material where codigo='"+codigo+"'";
        ResultSet rs =Tns.consultar(sqlString);
        if(rs.next()){
            matid=rs.getString("matid");
        }
        return matid;
    }
    public static String ConsultarTerid(String documento) throws ClassNotFoundException, SQLException{
        String teridString="";

       String sqlString = "select terid from terceros where (nit ='"+documento+"' or nittri ='"+documento+"')";

        ResultSet rs =Tns.consultar(sqlString);
        while(rs.next()){
            teridString=rs.getString("terid");
        }
        if (teridString.isEmpty()){
            sqlString="select terid from terceros where (nit ='22222222222' or NOMBRE ='CONSUMIDOR FINAL' )";
            rs =Tns.consultar(sqlString);    
            if (rs.next()){
                teridString=rs.getString("terid");
            } 
        }
        return teridString;
    }    
    public static void GuardarLog(String observacionString) throws SQLException{
        Tns.actualizar("INSERT INTO LOGTEMPORAL(FECHA,OBSERVACIONES)values('now','"+observacionString+"')");
    }
    public static String ConsultarFacturaKardexid(String numero) throws ClassNotFoundException, SQLException{
        String result="";
        String sql="select kardexid from kardex where numero='"+numero.trim()+"'";
        ResultSet rs = Tns.consultar(sql);
        if (rs.next()){
            result=rs.getString("kardexid");
        }
        return result;
    }    
    public static Boolean ConsultarFactura(String numero) throws ClassNotFoundException, SQLException{
        Boolean result=false;
        String sql="select kardexid from kardex where numero='"+numero.trim()+"'";
        ResultSet rs = Tns.consultar(sql);
        if (rs.next()){
            result=true;
        }
        return result;
    }
    public static void ConsultarDatosUsuario() throws Exception {
        String sql="select * from varios where variab like '%SQL%'";

        ResultSet rs = Tns.consultar(sql);
        while (rs.next()){
           // System.out.println(rs.getString("contenido"));
            if (rs.getString("variab").equals("HOSTSQL")) { host = rs.getString("contenido"); }
            if (rs.getString("variab").equals("PORTSQL")) { port = rs.getString("contenido"); }
            if (rs.getString("variab").equals("BDNAMESQL")) { bdName = rs.getString("contenido"); }
            if (rs.getString("variab").equals("USUARIOSQL")) { usuario = rs.getString("contenido"); }
            if (rs.getString("variab").equals("CLAVESQL")) { password = rs.getString("contenido"); }        
        }
    }
    
}
