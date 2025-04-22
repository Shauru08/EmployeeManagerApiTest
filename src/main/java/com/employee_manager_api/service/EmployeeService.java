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

    // Thread pool con 3 hilos para procesar archivos S3 en paralelo
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    // Repositorio para operaciones CRUD sobre la base de datos
    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    //Creo una nueva instancia de S3EmployeeReader, enviando como parametros 2 variables obtenidas desde el entorno donde fuera lanzado la aplicacion ( Local o AWS Lambda )
    private final S3EmployeeReader s3Reader = new S3EmployeeReader(EnvLoad.get("S3_BUCKET"), EnvLoad.get("S3_REGION"));

    // Crea un nuevo empleado después de validar su formato
    public void createEmployee(Employee employee) throws Exception {
        EmployeeValidator.validateFormat(employee);
        logger.info("Creando nuevo empleado: {}", employee.getName());
        employeeRepository.createEmployee(employee);
    }

    // Devuelve todos los empleados almacenados en la base de datos
    public List<Employee> getAllEmployees() throws Exception {
        logger.info("Obteniendo lista de empleados...");
        return employeeRepository.getAllEmployees();
    }

    // Devuelve un empleado según su ID. Lanza error si el ID no es válido. ( 0 )
    public Employee getEmployeeById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del empleado debe ser un numero positivo.");
        }
        logger.info("Obteniendo empleado con ID: {}", id);
        return employeeRepository.getEmployeeById(id);
    }

    // Actualiza un empleado después de validar sus datos
    public void updateEmployee(Employee employee) throws Exception {
        EmployeeValidator.validateFormat(employee);
        logger.info("Actualizando empleado con ID: {}", employee.getId());
        employeeRepository.updateEmployee(employee);
    }

    // Elimina un empleado según su ID. Lanza error si el ID es inválido. ( 0 )
    public void deleteEmployee(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del empleado debe ser un numero positivo.");
        }
        logger.info("Eliminando empleado con ID: {}", id);
        employeeRepository.deleteEmployee(id);
    }

    // Obtiene los 10 empleados con mayores salarios desde archivos JSON en S3
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

        // Retornar los 10 con mayor salario (o todos si hay menos)
        return allEmployees.subList(0, Math.min(10, allEmployees.size()));
    }
}
