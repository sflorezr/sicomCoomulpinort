package com.sergio.prueba;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

public class ConnectionSQL {
    private Connection connection;
    private String host;
    private String port;
    private String database;
    private String user;
    private String password;

    public ConnectionSQL(String host, String ruta, String user, String password,String port) {
        this.host = host;
        this.port = port;
        this.database = ruta;
        this.user = user;
        this.password = password;
    }
    private String getUrl(){
        return "jdbc:sqlserver://;serverName="+this.host+"/"+this.port+":"+this.database;
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
    SQLServerDataSource ds = new SQLServerDataSource();
    ds.setServerName(this.host); // Replace with your server name
    ds.setDatabaseName(this.database); // Replace with your database
    ds.setUser(this.user); // Replace with your user name
    ds.setPassword(this.password); // Replace with your password
    ds.setEncrypt(true);
    ds.setTrustServerCertificate(true);
    ds.setHostNameInCertificate("*.database.windows.net");
    ds.setLoginTimeout(30);
    ds.setAuthentication("SqlPassword");
    Connection connection = null;
    try{
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        connection = ds.getConnection();
    }catch (Exception ex){
        ex.getMessage();
    }
    return connection;
    }    

    public ResultSet consultar(String sql)throws ClassNotFoundException, SQLException{
        connection = getConnection();
        System.out.println(sql);
        return connection.prepareStatement(sql).executeQuery();
    }    
}
