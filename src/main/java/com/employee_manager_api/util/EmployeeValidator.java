package com.employee_manager_api.util;

import com.employee_manager_api.domain.entity.Employee;

public class EmployeeValidator {

    // Valida que los campos obligatorios del empleado esten completos y sean validos
    public static void validateFormat(Employee employee) {

        // Verifica que el nombre no sea nulo ni vacio
        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del empleado no puede estar vacio.");
        }

        // Verifica que el cargo no sea nulo ni vacio
        if (employee.getPosition() == null || employee.getPosition().trim().isEmpty()) {
            throw new IllegalArgumentException("El cargo del empleado no puede estar vacio.");
        }

        // Verifica que el salario sea mayor a 0
        if (employee.getSalary() <= 0) {
            throw new IllegalArgumentException("El salario debe ser mayor a 0.");
        }

        // Verifica que la fecha de contratacion no sea nula ni vacia
        if (employee.getHireDate() == null || employee.getHireDate().trim().isEmpty()) {
            throw new IllegalArgumentException("La fecha de contratacion no puede estar vacia.");
        }

        // Verifica que el departamento no sea nulo ni vacio
        if (employee.getDepartment() == null || employee.getDepartment().trim().isEmpty()) {
            throw new IllegalArgumentException("El departamento no puede estar vacio.");
        }
    }
}
