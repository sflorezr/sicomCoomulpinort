package com.test.java.com.sergio.facelectronica;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import javax.mail.internet.MimeMultipart;
import javax.mail.Multipart;

public class prueba {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, ParseException {
        final String username = "slflorez91@hotmail.com";
        final String password = "Nakia*300";
        
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.office365.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");
     //   props.put("mail.smtp.socketFactory.port", "587");
     //   props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    //    props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
              protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
              }
          });
        
          Message msg = new MimeMessage(session);
          try {
              msg.setFrom(new InternetAddress(username));
              msg.setRecipient(Message.RecipientType.TO, new InternetAddress("slflorez91@gmail.com"));
              msg.setSubject("probando con un adjunto");
      
              Multipart multipart = new MimeMultipart();
      
              MimeBodyPart textBodyPart = new MimeBodyPart();
              textBodyPart.setText("your text");
      
              MimeBodyPart attachmentBodyPart= new MimeBodyPart();
              DataSource source = new FileDataSource("c:\\tempo\\CV.pdf"); // ex : "C:\\test.pdf"
              attachmentBodyPart.setDataHandler(new DataHandler(source));
              attachmentBodyPart.setFileName("CV.pdf"); // ex : "test.pdf"
      
              multipart.addBodyPart(textBodyPart);  // add the text part
              multipart.addBodyPart(attachmentBodyPart); // add the attachement part
      
              msg.setContent(multipart);
      
      
              Transport.send(msg);
              System.out.println("si se envio");
        } catch (Exception e) {
            System.out.println("no se envio");
            System.out.println(e.getMessage());
        }

    }
}
 