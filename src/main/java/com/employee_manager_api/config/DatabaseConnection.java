package com.employee_manager_api.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
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

/**
 * Clase encargada de establecer y mantener una conexión a la base de datos
 * MySQL, recuperando las credenciales de AWS Secrets Manager y la información
 * de host y puerto desde la instancia RDS asociada al identificador
 * proporcionado.
 */
public class DatabaseConnection {

    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);

    // Instancia unica para aplicar singleton
    private static DatabaseConnection instance;

    // Conexion a la base de datos
    private Connection connection;

    // Variables de entorno necesarias para conectar a AWS y obtener los secretos
    private static final String SECRET_ARN = EnvLoad.get("SECRET_ARN");
    private static final String AWS_REGION = EnvLoad.get("MY_AWS_REGION");
    private static final String AWS_ACCESS_KEY_ID = EnvLoad.get("MY_AWS_ACCESS_KEY_ID");
    private static final String AWS_SECRET_ACCESS_KEY = EnvLoad.get("MY_AWS_SECRET_ACCESS_KEY");
    private static final String DB_INSTANCE_IDENTIFIER = EnvLoad.get("DB_INSTANCE_IDENTIFIER");

    // Constructor privado que inicializa la conexion usando datos de Secrets Manager y RDS
    private DatabaseConnection() throws Exception {
        logger.info("[Init] Iniciando conexion a la base de datos...");

        // Verifica si las credenciales fueron cargadas correctamente
        if (AWS_ACCESS_KEY_ID == null || AWS_SECRET_ACCESS_KEY == null) {
            logger.error("Credenciales de AWS no configuradas");
            throw new IllegalStateException("Las credenciales de AWS no estan configuradas.");
        }

        // Crea cliente para comunicarse con AWS
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);

        // Configura Secrets Manager y solicita el secreto
        logger.info("Inicializando Secrets Manager en región {}", AWS_REGION);
        SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

        logger.info("Obteniendo secreto con ARN: {}", SECRET_ARN);
        GetSecretValueResponse response = secretsClient.getSecretValue(
                GetSecretValueRequest.builder().secretId(SECRET_ARN).build()
        );
        logger.info("Secreto recuperado correctamente");

        // Parsea el JSON y obtiene credenciales de acceso
        JsonObject secretJson = JsonParser.parseString(response.secretString()).getAsJsonObject();
        String username = secretJson.get("username").getAsString();
        String password = secretJson.get("password").getAsString();
        String dbIdentifier = secretJson.has("dbInstanceIdentifier") && !secretJson.get("dbInstanceIdentifier").isJsonNull()
                ? secretJson.get("dbInstanceIdentifier").getAsString()
                : DB_INSTANCE_IDENTIFIER;

        logger.info("Identificador de base obtenido: {}", dbIdentifier);

        // Solicita a RDS la metadata de la instancia
        logger.info("Consultando RDS con identificador: {}", dbIdentifier);
        RdsClient rdsClient = RdsClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

        DescribeDbInstancesResponse dbResponse = rdsClient.describeDBInstances(
                DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbIdentifier).build()
        );
        logger.info("Metadatos de instancia RDS obtenidos");

        DBInstance instance = dbResponse.dbInstances().get(0);
        String host = instance.endpoint().address();
        String port = String.valueOf(instance.endpoint().port());

        logger.info("Endpoint de RDS: {}:{}", host, port);

        // Construye la cadena de conexion JDBC
        String url = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, dbIdentifier
        );

        logger.info("Conectando a base de datos con URL JDBC armada");

        // Establece la conexion
        this.connection = DriverManager.getConnection(url, username, password);
        logger.info("Conexion a la base de datos establecida correctamente.");
    }

    // Retorna la unica instancia activa. Si no existe o esta cerrada, se crea una nueva.
    public static DatabaseConnection getInstance() throws Exception {
        if (instance == null || instance.connection.isClosed()) {
            synchronized (DatabaseConnection.class) {
                if (instance == null || instance.connection.isClosed()) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    // Retorna el objeto de conexion reutilizable
    public Connection getConnection() {
        return connection;
    }

    // Cierra la conexion si esta abierta
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
