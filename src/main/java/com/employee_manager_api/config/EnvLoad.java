package com.employee_manager_api.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Clase para cargar variables de entorno, ya sea desde el sistema o un archivo
 * .env en local.
 */
public class EnvLoad {

    private static final boolean isRunningOnLambda = System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null;
    private static final Dotenv dotenv = isRunningOnLambda ? null : Dotenv.load();

    public static String get(String key) {
        String value = System.getenv(key); // Primero intentamos obtener desde el entorno del sistema

        if (value == null && !isRunningOnLambda) {
            value = dotenv.get(key); // Si no es Lambda, intentamos obtenerlo del archivo .env
        }

        return value;
    }
}
