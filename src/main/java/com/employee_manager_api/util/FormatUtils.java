package com.employee_manager_api.util;

public class FormatUtils {

    // Metodo estatico para construir un mensaje JSON plano con clave y valor.
    // Escapa comillas dobles del valor para evitar errores de formato.
    public static String jsonMessage(String key, String value) {
        return String.format("{\"%s\":\"%s\"}", key, value.replace("\"", "\\\""));
    }

}
