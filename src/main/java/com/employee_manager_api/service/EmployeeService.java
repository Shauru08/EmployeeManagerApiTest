package com.employee_manager_api.service;

import com.employee_manager_api.config.EnvLoad;
import com.employee_manager_api.domain.entity.Employee;
import com.employee_manager_api.repository.EmployeeRepository;
import com.employee_manager_api.util.EmployeeValidator;
import com.employee_manager_api.util.S3EmployeeReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.*;

public class EmployeeService {

    private static final Logger logger = LogManager.getLogger(EmployeeService.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    //Creo una nueva instancia de S3EmployeeReader, enviando como parametros 2 variables obtenidas desde el entorno donde fuera lanzado la aplicacion ( Local o AWS Lambda )
    private final S3EmployeeReader s3Reader = new S3EmployeeReader(EnvLoad.get("S3_BUCKET"), EnvLoad.get("S3_REGION"));

    /**
     * Crea un nuevo empleado después de validar los datos.
     */
    public void createEmployee(Employee employee) throws Exception {
        // Validar estructura y contenido del empleado
        EmployeeValidator.validateFormat(employee);

        logger.info("Creando nuevo empleado: {}", employee.getName());

        // Insertar en base de datos
        employeeRepository.createEmployee(employee);
    }

    /**
     * Obtiene todos los empleados de la base de datos.
     */
    public List<Employee> getAllEmployees() throws Exception {
        logger.info("Obteniendo lista de empleados...");

        // Llamar al repositorio
        return employeeRepository.getAllEmployees();
    }

    /**
     * Busca un empleado por su ID.
     */
    public Employee getEmployeeById(int id) throws Exception {
        // Validar que el ID sea mayor a 0
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del empleado debe ser un numero positivo.");
        }

        logger.info("Obteniendo empleado con ID: {}", id);

        // Consultar en base de datos
        return employeeRepository.getEmployeeById(id);
    }

    /**
     * Actualiza un empleado después de validar los datos.
     */
    public void updateEmployee(Employee employee) throws Exception {
        // Validar datos del empleado
        EmployeeValidator.validateFormat(employee);

        logger.info("Actualizando empleado con ID: {}", employee.getId());

        // Ejecutar actualizacion en base de datos
        employeeRepository.updateEmployee(employee);
    }

    /**
     * Elimina un empleado por ID.
     */
    public void deleteEmployee(int id) throws Exception {
        // Validar que el ID sea mayor a 0
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del empleado debe ser un numero positivo.");
        }

        logger.info("Eliminando empleado con ID: {}", id);

        // Ejecutar eliminacion
        employeeRepository.deleteEmployee(id);
    }

    /**
     * Obtiene los 10 empleados con mayor salario leyendo archivos desde S3.
     */
    public List<Employee> getTopSalaries() {
        logger.info("[Init] Obteniendo empleados desde archivos S3...");

        // Obtener lista de archivos JSON en el bucket
        List<String> files = s3Reader.listJsonFiles();

        // Crear tareas para procesar cada archivo en paralelo
        List<Future<List<Employee>>> futures = new ArrayList<>();
        for (String file : files) {
            futures.add(executorService.submit(() -> s3Reader.readEmployees(file)));
        }

        List<Employee> allEmployees = new ArrayList<>();

        // Combinar los resultados de los hilos
        try {
            for (Future<List<Employee>> future : futures) {
                allEmployees.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error al procesar archivos desde S3, Exception {}", e);
            Thread.currentThread().interrupt();
        }

        // Ordenar por salario descendente
        allEmployees.sort(Comparator.comparingDouble(Employee::getSalary).reversed());

        // Devolver top 10 o menos si hay menos empleados
        return allEmployees.subList(0, Math.min(10, allEmployees.size()));
    }
}
