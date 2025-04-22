package com.employee_manager_api.config;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvLoad {

    // Determina si la aplicacion se esta ejecutando en AWS Lambda
    private static final boolean isRunningOnLambda = System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null;

    // Si no estamos en Lambda, cargamos las variables desde el archivo .env
    private static final Dotenv dotenv = isRunningOnLambda ? null : Dotenv.load();

    // Devuelve el valor de una variable de entorno
    // Prioriza valores del sistema, y si no esta en Lambda, busca en el archivo .env
    public static String get(String key) {
        String value = System.getenv(key);

        if (value == null && !isRunningOnLambda) {
            value = dotenv.get(key);
        }

        return value;
    }
}
