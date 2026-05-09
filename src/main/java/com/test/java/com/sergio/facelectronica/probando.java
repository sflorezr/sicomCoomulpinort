package com.test.java.com.sergio.facelectronica;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import com.sergio.prueba.AbstractConexion;
import com.sergio.prueba.ConnectionSQL;

public class probando {
    private static ConnectionSQL SQLServer=null;
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        String token ="adminlacuatro"+":"+"Admin2018+";
        token= DatatypeConverter.printBase64Binary(token.getBytes());
        System.out.println(token);
        String numero="asdf-1232132";
        System.out.println(numero.split("-")[0]);
        System.out.println(numero.split("-")[1]);
        System.out.println(numero.substring(2,numero.length()));
        String nombre="EDILEXI NAYARITH ARDILA";
        System.out.println(nombre.replaceAll(" ", "%"));
        System.out.println(nombre.split(" ").length);

    }
    
}
