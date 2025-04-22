package com.employee_manager_api.util;

import com.employee_manager_api.domain.entity.Employee;

public class EmployeeValidator {

    /**
     * Valida los datos del empleado antes de guardarlos o actualizarlos.
     *
     * @param employee Objeto Employee a validar.
     * @throws IllegalArgumentException si algún dato es inválido.
     */
    public static void validateFormat(Employee employee) {
        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del empleado no puede estar vacio.");
        }
        if (employee.getPosition() == null || employee.getPosition().trim().isEmpty()) {
            throw new IllegalArgumentException("El cargo del empleado no puede estar vacio.");
        }
        if (employee.getSalary() <= 0) {
            throw new IllegalArgumentException("El salario debe ser mayor a 0.");
        }
        if (employee.getHireDate() == null || employee.getHireDate().trim().isEmpty()) {
            throw new IllegalArgumentException("La fecha de contratacion no puede estar vacia.");
        }
        if (employee.getDepartment() == null || employee.getDepartment().trim().isEmpty()) {
            throw new IllegalArgumentException("El departamento no puede estar vacio.");
        }
    }
}
