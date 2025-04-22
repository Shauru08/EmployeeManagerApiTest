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

    public S3EmployeeReader(String bucketName, String region) {
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    /**
     * Lista los archivos .json disponibles en el bucket S3 configurado.
     *
     * @return Lista de nombres de archivos JSON.
     */
    public List<String> listJsonFiles() {
        List<String> keys = new ArrayList<>();

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

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

    /**
     * Lee un archivo JSON desde S3 y lo convierte en una lista de empleados.
     *
     * @param key Ruta del archivo en el bucket.
     * @return Lista de empleados leída desde el archivo.
     */
    public List<Employee> readEmployees(String key) {
        List<Employee> employees = new ArrayList<>();
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            employees = gson.fromJson(new InputStreamReader(response), new TypeToken<List<Employee>>() {
            }.getType());

            logger.info("Archivo {} leído correctamente. Empleados: {}", key, employees.size());

        } catch (Exception e) {
            logger.error("Error al leer archivo {} desde S3", key, e);
        }

        return employees;
    }
}
