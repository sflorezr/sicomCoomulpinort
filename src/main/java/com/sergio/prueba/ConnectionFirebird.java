package com.sergio.prueba;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionFirebird {
    private Connection connection;
    private String host;
    private String port;
    private String database;
    private String user;
    private String password;

    public ConnectionFirebird(String host, String ruta, String user, String password,String port) {
        this.host = host;
        this.port = port;
        this.database = ruta;
        this.user = user;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    private String getUrl(){
        return "jdbc:firebirdsql:"+this.host+"/"+this.port+":"+this.database;
    }
    public synchronized Statement getStatement() throws SQLException, ClassNotFoundException {
        if(connection == null || connection.isClosed()){
            getConnection();
        }
        return connection.createStatement();
    }
    public void cerrarConexion() throws SQLException{
        if(this.connection != null && !connection.isClosed()){
            connection.close();
        }
    }

    public Connection getConnection() {
        try {
            Class.forName("org.firebirdsql.jdbc.FBDriver");
            this.connection = DriverManager.getConnection(this.getUrl(), this.user, this.password);
            //System.out.println("Conexion a Base de Datos Firebird "+getUrl()+" . . . . .Ok");
        }catch (SQLException s){
            s.printStackTrace();
        }catch (ClassNotFoundException c){
            System.out.println(c.getMessage());
            c.printStackTrace();
        }
        return this.connection;
    }
    public ResultSet consultar(String sql)throws ClassNotFoundException, SQLException{
        connection = getConnection();
       // System.out.println(sql);
        return connection.prepareStatement(sql).executeQuery();
    }
    public void actualizar(String sql) throws SQLException {
        System.out.println(sql);
        try {
            connection = getConnection();
            connection.prepareStatement(sql).executeUpdate();    
        } catch (SQLException e) {
           System.out.println(sql);
        }
        
    }
    public ResultSet acualizarConId(String sql)throws SQLException{
        ResultSet rs=null;
        PreparedStatement preparedStatement = null;
        try {
            connection =getConnection();
            preparedStatement =connection.prepareStatement(sql); 
            rs=preparedStatement.executeQuery();  
        } catch (SQLException e) {
           System.out.println(sql);
        }
        return rs;
    }
}
