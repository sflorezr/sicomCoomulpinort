package com.sergio.prueba;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractConexion {
    protected abstract Connection crearConexion() throws ClassNotFoundException, SQLException;
    Logger logger = Logger.getLogger(AbstractConexion.class.getName());
    protected void cerraConexion(Connection connection) {
        try {
            connection.close();
        } catch (Exception ex) {
            
        }
    }    

    public List<String> consultarDatos(String comando, String campo) {
        List<String> resultado = new ArrayList<>();
        ResultSet resultSet = null;
        try (Connection connection = crearConexion();
             PreparedStatement preparedStatement = connection.prepareStatement(comando)) {
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                resultado.add(resultSet.getString(campo));
            }
            resultSet.close();
            cerraConexion(connection);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Querry fallido "+comando);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    logger.info(e.getMessage());
                }
            }
        }

        return resultado;
    }

}
