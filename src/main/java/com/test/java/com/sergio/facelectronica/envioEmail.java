package com.test.java.com.sergio.facelectronica;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.sergio.prueba.ConnectionFirebird;

public class envioEmail {
    
    private static String correo;
	private static String password;
    private static ConnectionFirebird Tns=null;
    public static void main(String[] args) throws MessagingException, IOException, ClassNotFoundException, SQLException {
    /*     File tempo = new File("c:\\tempo\\envioEmail.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String base=data.split("\\|")[0];
        String ip=data.split("\\|")[1];
        String usuario=data.split("\\|")[2];
        br.close();
        Tns = new ConnectionFirebird(ip,base,"SYSDBA","masterkey","3050");
        ConsultarDatosUsuario(usuario);
*/
      String remitente = "slflorez91@gmail.com";
      //La clave de aplicación obtenida según se explica en este artículo:
      String claveemail = "nkws rbow kdvu qsvn";
   
      Properties props = System.getProperties();
      props.put("mail.smtp.host", "smtp.gmail.com");  //El servidor SMTP de Google
      props.put("mail.smtp.user", remitente);
      props.put("mail.smtp.clave", claveemail);    //La clave de la cuenta
      props.put("mail.smtp.auth", "true");    //Usar autenticación mediante usuario y clave
      props.put("mail.smtp.starttls.enable", "true"); //Para conectar de manera segura al servidor SMTP
      props.put("mail.smtp.port", "587"); //El puerto SMTP seguro de Google
      props.put("mail.smtp.ssl.protocols", "TLSv1.2");
      props.put("mail.smtp.socketFactory.port", "587");
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
   
      Session session = Session.getDefaultInstance(props);
      MimeMessage message = new MimeMessage(session);

      try {
          message.setFrom(new InternetAddress(remitente));
          message.addRecipient(Message.RecipientType.TO, new InternetAddress("slflorez91@gmail.com"));   //Se podrían añadir varios de la misma manera
          message.setSubject("asunto");
          message.setText("cuerpo");
          message.setFileName("C:\\tempo\\AceptarPedido.jar");
          Transport transport = session.getTransport("smtp");
          transport.connect("smtp.gmail.com", remitente, claveemail);
          transport.sendMessage(message, message.getAllRecipients());
          transport.close();
      }
      catch (MessagingException me) {
          me.printStackTrace();   //Si se produce un error
      }
    }

    public static void ConsultarDatosUsuario(String usuarioString)throws SQLException, ClassNotFoundException{
        String sql="SELECT * FROM varios WHERE VARIAB  LIKE '%MAIL"+usuarioString+"%'";

        ResultSet rs = Tns.consultar(sql);
        while(rs.next()){
            if(rs.getString("VARIAB").equals("GPASSWORDMAIL"+usuarioString)){
                password=rs.getString("CONTENIDO");
            }
            if(rs.getString("VARIAB").equals("GMAIL"+usuarioString)){
                correo=rs.getString("CONTENIDO");
            }           
        }
        System.out.println(correo+"-"+password);
    }
}
