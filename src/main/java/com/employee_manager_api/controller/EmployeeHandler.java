package com.employee_manager_api.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.employee_manager_api.domain.entity.Employee;
import com.employee_manager_api.service.EmployeeService;
import com.employee_manager_api.util.FormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final Logger logger = LogManager.getLogger(EmployeeHandler.class);
    private final EmployeeService employeeService = new EmployeeService();
    private final Gson gson = new Gson();

    // Metodo principal que actua como punto de entrada para AWS Lambda
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        // Objeto response que es serializado a un JSON como respuesta HTTP
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Input entrante: {}", input);

            // Obtengo la ruta y el metodo HTTP desde la request recibida por API Gateway  ("/employees",/employees/{id} ,"/employees/salary/top")
            String path = (String) input.get("path");
            String httpMethod = (String) input.get("httpMethod");

            logger.info("[Init] Request recibida - Ruta: {} - Metodo: {}", path, httpMethod);

            // Extraigo el valor de path "proxy", puede ser "employees", "employees/1", etc.
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            logger.debug("PathParams entrante: {}", input);
            final String proxyPath = (pathParams != null) ? pathParams.get("proxy") : null;

            logger.info("Proxy path recibido: {}", proxyPath);

            //Map "padre" que contendra posteriormente los subMapas correspondientes a las distintas rutas bajo /employee
            Map<String, Map<String, Runnable>> routeHandlers = new HashMap<>();

            // Submapa de handlers para rutas base "/employees"
            Map<String, Runnable> employeesHandlers = new HashMap<>();
            // Submapa de handlers para rutas tipo "/employees/{id}"
            Map<String, Runnable> employeeIdHandlers = new HashMap<>();
            // Submapa de handlers para ruta "/employees/salary/top"
            Map<String, Runnable> topSalaryHandlers = new HashMap<>();

            // /employees GET
            employeesHandlers.put("GET", () -> {
                //Devuelve una lista con todos los empleados, en caso exitoso, devuelve un codigo de estado HTTP 200, Ok.
                try {
                    logger.info("Obteniendo todos los empleados");
                    List<Employee> employees = employeeService.getAllEmployees();
                    response.put("statusCode", 200);
                    response.put("body", gson.toJson(employees));
                } catch (Exception e) {
                    logger.error("Error al obtener empleados", e);
                    response.put("statusCode", 500);
                    response.put("body", FormatUtils.jsonMessage("error", "Error al obtener empleados: " + e.getMessage()));
                }
            });

            // /employees POST
            employeesHandlers.put("POST", () -> {
                //Crea un nuevo empleado, en caso exitoso, devuelve un codigo de estado HTTP 201, Created.
                try {
                    logger.info("Creando un nuevo empleado");
                    String body = (String) input.get("body");
                    Employee newEmployee = gson.fromJson(body, Employee.class);
                    employeeService.createEmployee(newEmployee);
                    response.put("statusCode", 201);
                    response.put("body", FormatUtils.jsonMessage("message", "Empleado creado correctamente."));
                } catch (Exception e) {
                    logger.error("Error al crear empleado", e);
                    response.put("statusCode", 500);
                    response.put("body", FormatUtils.jsonMessage("error", "Error al crear empleado: " + e.getMessage()));
                }
            });

            // /employees/{id} GET
            employeeIdHandlers.put("GET", () -> {
                //Devuelve un json conteniendo la informacion de un unico empleado, filtrando por ID
                try {
                    Integer id = extractIdFromProxy(proxyPath, response);
                    if (id == null) {
                        return;
                    }
                    logger.info("Obteniendo empleado con ID: {}", id);
                    Employee emp = employeeService.getEmployeeById(id);
                    response.put("statusCode", 200);
                    if (emp == null) {
                        response.put("body", FormatUtils.jsonMessage("error", "No se encontro ningun usuario con el id: " + id));
                    } else {
                        response.put("body", gson.toJson(emp));
                    }
                } catch (Exception e) {
                    logger.error("Error al obtener empleado por ID", e);
                    response.put("statusCode", 500);
                    response.put("body", FormatUtils.jsonMessage("error", "Error al obtener empleado por ID: " + e.getMessage()));
                }
            });

            // /employees/{id} PUT
            employeeIdHandlers.put("PUT", () -> {
                //Modifica la informacion de un usuario, filtrandolo por ID
                try {
                    Integer id = extractIdFromProxy(proxyPath, response);
                    if (id == null) {
                        return;
                    }
                    String body = (String) input.get("body");
                    Employee updatedEmployee = gson.fromJson(body, Employee.class);
                    updatedEmployee.setId(id);
                    logger.info("Actualizando empleado con ID: {}", id);
                    employeeService.updateEmployee(updatedEmployee);
                    response.put("statusCode", 200);
                    response.put("body", FormatUtils.jsonMessage("message", "Empleado actualizado."));
                } catch (Exception e) {
                    logger.error("Error al actualizar empleado", e);
                    response.put("statusCode", 500);
                    response.put("body", FormatUtils.jsonMessage("error", "Error al actualizar empleado: " + e.getMessage()));
                }
            });

            // /employees/{id} DELETE
            employeeIdHandlers.put("DELETE", () -> {
                //Elimina un registro de la tabla Employee encontrandolo por su ID
                try {
                    Integer id = extractIdFromProxy(proxyPath, response);
                    if (id == null) {
                        return;
                    }
                    logger.info("Eliminando empleado con ID: {}", id);
                    employeeService.deleteEmployee(id);
                    response.put("statusCode", 200);
                    response.put("body", FormatUtils.jsonMessage("message", "Empleado eliminado."));
                } catch (Exception e) {
                    logger.error("Error al eliminar empleado", e);
                    response.put("statusCode", 500);
                    response.put("body", FormatUtils.jsonMessage("error", "Error al eliminar empleado: " + e.getMessage()));
                }
            });

            // /employees/salary/top GET
            topSalaryHandlers.put("GET", () -> {
                // Devuelve top 10 empleados con mayor salario
                try {
                    logger.info("Obteniendo empleados con los mayores salarios");
                    List<Employee> topEmployees = employeeService.getTopSalaries();
                    response.put("statusCode", 200);
                    response.put("body", gson.toJson(topEmployees));
                } catch (Exception e) {
                    logger.error("Error al obtener empleados con mayores salarios", e);
                    response.put("statusCode", 500);
                    response.put("body", FormatUtils.jsonMessage("error", "Error al obtener empleados con mayores salarios: " + e.getMessage()));
                }
            });

            // Cargo todos los submapas en el mapa principal para gestionarlos segun la ruta
            routeHandlers.put("/employees", employeesHandlers);
            routeHandlers.put("/employees/{id}", employeeIdHandlers);
            routeHandlers.put("/employees/salary/top", topSalaryHandlers);

            // Logica de ruteo manual basada en proxyPath y metodo HTTP
            if (proxyPath != null && proxyPath.matches("employees/\\d+") && employeeIdHandlers.containsKey(httpMethod)) {
                employeeIdHandlers.get(httpMethod).run();
            } else if ("employees".equals(proxyPath) && employeesHandlers.containsKey(httpMethod)) {
                employeesHandlers.get(httpMethod).run();
            } else if ("employees/salary/top".equals(proxyPath) && topSalaryHandlers.containsKey(httpMethod)) {
                topSalaryHandlers.get(httpMethod).run();
            } else {
                logger.warn("Ruta o metodo no encontrados: {} - {}", proxyPath, httpMethod);
                response.put("statusCode", 404);
                response.put("body", FormatUtils.jsonMessage("error", "Ruta o metodo no encontrados."));
            }

        } catch (Exception e) {
            logger.error("Error inesperado en la ejecucion del handler", e);
            response.put("statusCode", 500);
            response.put("body", FormatUtils.jsonMessage("error", "Error interno: " + e.getMessage()));
        }

        // Fuerza cabecera de respuesta como JSON
        response.put("headers", Map.of("Content-Type", "application/json"));

        logger.info("[Fin] Finaliza ejecucion con respuesta: {}", response);
        return response;
    }

    // Extrae el ID del path tipo "employees/{id}". Si es invalido, responde con error 400.
    private Integer extractIdFromProxy(String proxyPath, Map<String, Object> response) {
        if (proxyPath == null || !proxyPath.matches("employees/\\d+")) {
            response.put("statusCode", 400);
            response.put("body", FormatUtils.jsonMessage("error", "Formato de URL incorrecto o ID no proporcionado."));
            return null;
        }
        return Integer.parseInt(proxyPath.split("/")[1]);
    }
}
