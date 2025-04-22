package com.employee_manager_api.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.employee_manager_api.domain.entity.Employee;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class S3EmployeeReader {

    private static final Logger logger = LogManager.getLogger(S3EmployeeReader.class);

    private final String bucketName;
    private final S3Client s3Client;
    private final Gson gson = new Gson();

    // Constructor que configura el cliente S3 para acceder al bucket especificado
    public S3EmployeeReader(String bucketName, String region) {
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    // Retorna una lista de nombres de archivos .json encontrados en el bucket configurado
    public List<String> listJsonFiles() {
        List<String> keys = new ArrayList<>();

        try {
            // Construye la solicitud para listar objetos en el bucket
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            // Ejecuta la solicitud y obtiene la respuesta
            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            // Itera sobre los archivos encontrados y filtra los que terminan en ".json"
            for (S3Object object : response.contents()) {
                String key = object.key();
                if (key.endsWith(".json")) {
                    keys.add(key);
                    logger.info("Archivo detectado: {}", key);
                }
            }

        } catch (Exception e) {
            logger.error("Error al listar archivos en el bucket", e);
        }

        return keys;
    }

    // Lee un archivo .json desde S3 y lo transforma en una lista de objetos Employee
    public List<Employee> readEmployees(String key) {
        List<Employee> employees = new ArrayList<>();
        try {
            // Construye la solicitud para obtener un objeto específico del bucket
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Realiza la lectura del archivo
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);

            // Parsea el contenido JSON en una lista de empleados
            employees = gson.fromJson(new InputStreamReader(response), new TypeToken<List<Employee>>() {
            }.getType());

            logger.info("Archivo {} leído correctamente. Empleados: {}", key, employees.size());

        } catch (Exception e) {
            logger.error("Error al leer archivo {} desde S3", key, e);
        }

        return employees;
    }
}
