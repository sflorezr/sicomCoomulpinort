package com.test.java.com.sergio.facelectronica;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.sergio.prueba.ConnectionFirebird;

import javax.mail.Multipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.internet.MimeBodyPart;
import javax.activation.FileDataSource;
public class SendEmailOffice365 {
    private static ConnectionFirebird Tns=null;
    private static final Logger LOGGER = Logger.getAnonymousLogger();

    private static String SERVIDOR_SMTP = "smtp.office365.com";
    private static int PORTA_SERVIDOR_SMTP = 587;
    private static String EMIAL = "";
    private static String CLAVE = "";

    private final String from = EMIAL;
    private static String to = "slflorez91@gmail.com";

    private static String subject = "HISTORIA CLINICA";
    private static String messageContent = "PSI";
    private static String usuid="";
    private static String file="";

    public void sendEmail() throws IOException, SQLException {

        final Session session = Session.getInstance(this.getEmailProperties(), new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMIAL, CLAVE);
            }

        });

        try {
            final Message message = new MimeMessage(session);
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setFrom(new InternetAddress(from));
            message.setSubject(subject);
            message.setText(messageContent);
            message.setSentDate(new Date());
            Multipart multipart = new MimeMultipart();
      
            MimeBodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setText(messageContent);
                
            MimeBodyPart attachmentBodyPart= new MimeBodyPart();
            DataSource source = new FileDataSource(file); // ex : "C:\\test.pdf"
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName("File.pdf"); // ex : "test.pdf"
    
            multipart.addBodyPart(textBodyPart);  // add the text part
            multipart.addBodyPart(attachmentBodyPart); // add the attachement part
            message.setContent(multipart);
            Transport.send(message);
            GuardarLog("Se envio el mensaje "+to+" asunto: "+subject+" con exito" );
        } catch (final MessagingException ex) {
            GuardarLog("No se envio el mensaje "+to+" asunto:"+subject);
            LOGGER.log(Level.WARNING, "Error al enviar mensaje: " + ex.getMessage(), ex);
        }
    }
    public static void GuardarLog(String observacionString) throws SQLException{
        Tns.actualizar("INSERT INTO LOGTEMPORAL(FECHA,mensaje)values('now','"+observacionString+"')");
    }
    public Properties getEmailProperties() throws IOException {
        
        final Properties config = new Properties();
        config.put("mail.smtp.auth", "true");
        config.put("mail.smtp.starttls.enable", "true");
        config.put("mail.smtp.host", SERVIDOR_SMTP);
        config.put("mail.smtp.port", PORTA_SERVIDOR_SMTP);
        config.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        config.put("mail.smtp.socketFactory.fallback", "true");     
        config.put("mail.smtp.starttls.enable","true");
        config.put("mail.smtp.socketFactory.port", "587");
       // config.put("mail.smtp.auth.mechanisms", "XOAUTH2");
       config.put("mail.smtp.ssl.trust", "*");
        config.put("mail.smtp.ssl.protocols", "TLSv1.2");
        System.out.println("enviado con exito");
        return config;
    }
    public static void ConsultarDatos() throws SQLException, ClassNotFoundException{
        String sql="select * from varios where variab like '%HC'";

        ResultSet rs = Tns.consultar(sql);
        while (rs.next()){
            System.out.println(rs.getString("contenido"));
            if (rs.getString("variab").equals("EMAILHC")) { EMIAL = rs.getString("contenido"); }
            if (rs.getString("variab").equals("CLAVEHC")) { CLAVE = rs.getString("contenido"); }
            if (rs.getString("variab").equals("HOSTHC")) { SERVIDOR_SMTP = rs.getString("contenido"); }        
            if (rs.getString("variab").equals("PORTHC")) { PORTA_SERVIDOR_SMTP = rs.getInt("contenido"); }   
            if (rs.getString("variab").equals("PER_ASUNTO_HC")) { subject = rs.getString("contenido"); }   
            if (rs.getString("variab").equals("PER_MENSAJE_HC")) { messageContent = rs.getString("contenido"); }   
        }
        sql="select * from usuahosp where usuahosid="+usuid+"";
        rs = Tns.consultar(sql);
        while (rs.next()){
            to=rs.getString("email");
        }
        System.out.println(to);
    }

    public static void main(final String[] args) throws IOException, ClassNotFoundException, SQLException {
        File tempo = new File("c:\\tempo\\uronorte.txt");
        FileReader fr = new FileReader(tempo);
        BufferedReader br = new BufferedReader(fr);
        String data=br.readLine();
        String ruta=data.split("\\|")[0];
        String ip=data.split("\\|")[1];
        file = data.split("\\|")[2];
        usuid = data.split("\\|")[3];
        Tns = new ConnectionFirebird(ip,ruta,"SYSDBA","masterkey","3050");        
        ConsultarDatos();       
        subject =subject+" "+data.split("\\|")[4];
        new SendEmailOffice365().sendEmail();
    }
    
}
