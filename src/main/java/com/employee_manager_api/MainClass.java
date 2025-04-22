package com.employee_manager_api;

import com.employee_manager_api.config.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;

public class MainClass {

    private static final Logger logger = LogManager.getLogger(MainClass.class);

    public static void main(String[] args) {
        try {
            logger.info("Probando conexion a la base de datos...");

            // Obtener la instancia de DatabaseConnection
            DatabaseConnection dbInstance = DatabaseConnection.getInstance();
            Connection connection = dbInstance.getConnection();

            if (connection != null && !connection.isClosed()) {
                logger.info("Conexion exitosa a la base de datos.");
            } else {
                logger.error("Error: No se pudo conectar a la base de datos.");
            }

            // Cerrar conexión después de la prueba
            dbInstance.closeConnection();
            logger.info("Conexion cerrada correctamente.");
        } catch (Exception e) {
            logger.error("Error en la conexion: {}", e.getMessage(), e);
        }
    }
}
