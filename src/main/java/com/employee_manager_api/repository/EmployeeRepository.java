package com.employee_manager_api.repository;

import com.employee_manager_api.config.DatabaseConnection;
import com.employee_manager_api.domain.entity.Employee;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {

    private static final Logger logger = LogManager.getLogger(EmployeeRepository.class);

    // Devuelve una lista con todos los empleados consultando el SP sp_get_all_employees
    public List<Employee> getAllEmployees() throws Exception {
        logger.info("[DB] Obteniendo todos los empleados...");
        List<Employee> employees = new ArrayList<>();
        String query = "{ CALL sp_get_all_employees() }";

        try (Connection connection = DatabaseConnection.getInstance().getConnection(); CallableStatement stmt = connection.prepareCall(query); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }

        } catch (SQLException e) {
            logger.error("[DB] Error al obtener empleados: ", e);
            throw new Exception("Error al obtener empleados", e);
        }

        return employees;
    }

    // Busca un empleado por ID ejecutando el SP sp_get_employee_by_id
    public Employee getEmployeeById(int id) throws Exception {
        logger.info("[DB] Buscando empleado con ID: {}", id);
        String query = "{ CALL sp_get_employee_by_id(?) }";

        try (Connection connection = DatabaseConnection.getInstance().getConnection(); CallableStatement stmt = connection.prepareCall(query)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEmployee(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("[DB] Error al obtener empleado con ID {}: ", id, e);
            throw new Exception("Error al obtener empleado con ID " + id, e);
        }
        return null; // Si no se encuentra el empleado, retorna null.
    }

    // Inserta un nuevo empleado en la base usando el SP sp_create_employee
    public void createEmployee(Employee employee) throws Exception {
        logger.info("[DB] Insertando nuevo empleado: {}", employee.getName());
        String query = "{ CALL sp_create_employee(?, ?, ?, ?, ?) }";

        try (Connection connection = DatabaseConnection.getInstance().getConnection(); CallableStatement stmt = connection.prepareCall(query)) {

            stmt.setString(1, employee.getName());
            stmt.setString(2, employee.getPosition());
            stmt.setDouble(3, employee.getSalary());
            stmt.setString(4, employee.getHireDate());
            stmt.setString(5, employee.getDepartment());

            stmt.executeUpdate();
            logger.info("[DB] Empleado {} insertado correctamente.", employee.getName());

        } catch (SQLException e) {
            logger.error("[DB] Error al insertar empleado: ", e);
            throw new Exception("Error al insertar empleado", e);
        }
    }

    // Actualiza los datos de un empleado en base a su ID usando el SP sp_update_employee
    public void updateEmployee(Employee employee) throws Exception {
        logger.info("[DB] Actualizando empleado con ID: {}", employee.getId());
        String query = "{ CALL sp_update_employee(?, ?, ?, ?, ?, ?) }";

        try (Connection connection = DatabaseConnection.getInstance().getConnection(); CallableStatement stmt = connection.prepareCall(query)) {

            stmt.setInt(1, employee.getId());
            stmt.setString(2, employee.getName());
            stmt.setString(3, employee.getPosition());
            stmt.setDouble(4, employee.getSalary());
            stmt.setString(5, employee.getHireDate());
            stmt.setString(6, employee.getDepartment());

            stmt.executeUpdate();
            logger.info("[DB] Empleado con ID {} actualizado correctamente.", employee.getId());

        } catch (SQLException e) {
            logger.error("[DB] Error al actualizar empleado con ID {}: ", employee.getId(), e);
            throw new Exception("Error al actualizar empleado con ID " + employee.getId(), e);
        }
    }

    // Elimina un empleado por ID ejecutando el SP sp_delete_employee
    public void deleteEmployee(int id) throws Exception {
        logger.info("[DB] Eliminando empleado con ID: {}", id);
        String query = "{ CALL sp_delete_employee(?) }";

        try (Connection connection = DatabaseConnection.getInstance().getConnection(); CallableStatement stmt = connection.prepareCall(query)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.info("[DB] Empleado con ID {} eliminado correctamente.", id);

        } catch (SQLException e) {
            logger.error("[DB] Error al eliminar empleado con ID {}: ", id, e);
            throw new Exception("Error al eliminar empleado con ID " + id, e);
        }
    }

    // Mapea un ResultSet a un objeto Employee.
    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        //En una posible migracion a spring esto seria un RowMapper.
        return new Employee(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("position"),
                rs.getDouble("salary"),
                rs.getString("hire_date"),
                rs.getString("department")
        );
    }
}
