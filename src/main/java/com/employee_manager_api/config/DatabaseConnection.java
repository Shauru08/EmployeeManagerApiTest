package com.employee_manager_api.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.regions.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);

    private static DatabaseConnection instance;
    private Connection connection;

    /**
     * Leo variables desde el entorno donde se encuentre lanzada la aplicacion (
     * Local o AWS Lambda )
     */
    private static final String SECRET_ARN = EnvLoad.get("SECRET_ARN");
    private static final String AWS_REGION = EnvLoad.get("MY_AWS_REGION");
    private static final String AWS_ACCESS_KEY_ID = EnvLoad.get("MY_AWS_ACCESS_KEY_ID");
    private static final String AWS_SECRET_ACCESS_KEY = EnvLoad.get("MY_AWS_SECRET_ACCESS_KEY");

    /**
     * Constructor privado que inicializa la conexion a la base de datos. Se
     * obtiene la configuracion de AWS Secrets Manager para acceder de manera
     * segura a las credenciales de la base de datos.
     */
    private DatabaseConnection() throws Exception {
        logger.info("[Init] Iniciando conexion a la base de datos...");

        // Verifica si las credenciales de AWS estan presentes
        if (AWS_ACCESS_KEY_ID == null || AWS_SECRET_ACCESS_KEY == null) {
            logger.error("Credenciales de AWS no configuradas");
            throw new IllegalStateException("Las credenciales de AWS no estan configuradas.");
        }

        // Configura el cliente de Secrets Manager
        logger.info("Inicializando cliente de AWS Secrets Manager en la region: {}", AWS_REGION);
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
                ))
                .build();

        // Solicita el secreto de la base de datos
        logger.info("Solicitando el secreto con ARN: {}", SECRET_ARN);
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(SECRET_ARN)
                .build();
        GetSecretValueResponse response = client.getSecretValue(request);

        logger.debug("Secreto obtenido exitosamente.");

        // Extrae las credenciales del JSON retornado
        JsonObject secretJson = JsonParser.parseString(response.secretString()).getAsJsonObject();
        String username = secretJson.get("username").getAsString();
        String password = secretJson.get("password").getAsString();
        String host = secretJson.get("host").getAsString();
        String database = secretJson.get("dbInstanceIdentifier").getAsString();

        // Construye la cadena de conexion JDBC
        String url = "jdbc:mysql://" + host + ":3306/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        logger.info("Conectando a la base de datos en la URL: {}", url);

        // Establece la conexion
        this.connection = DriverManager.getConnection(url, username, password);
        logger.info("Conexion a la base de datos establecida correctamente.");
    }

    /**
     * Metodo estatico para obtener la instancia unica del objeto de conexion.
     * Se utiliza patron Singleton y garantiza que no haya multiples instancias
     * activas.
     */
    public static DatabaseConnection getInstance() throws Exception {
        if (instance == null || instance.connection.isClosed()) {
            // Synchronized forma ademas una especie de cola donde, si la conexion esta ocupada, el siguiente hilo esperara su turno antes de acceder.
            synchronized (DatabaseConnection.class) {
                // Verifica nuevamente dentro del bloque sincronizado
                if (instance == null || instance.connection.isClosed()) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Retorna la conexion activa a la base de datos.
     */
    public Connection getConnection() {
        // Devuelve el objeto de conexion que se esta reutilizando
        return connection;
    }

    /**
     * Cierra la conexion a la base de datos si esta abierta.
     */
    public void closeConnection() {
        try {
            // Verifica si la conexion esta activa antes de cerrarla
            if (connection != null && !connection.isClosed()) {
                logger.info("Cerrando conexion a la base de datos.");
                connection.close();
                logger.info("Conexion cerrada exitosamente.");
            }
        } catch (SQLException e) {
            logger.error("Error al cerrar la conexion a la base de datos", e);
        }
    }
}
