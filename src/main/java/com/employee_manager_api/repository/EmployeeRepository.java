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

    /**
     * Obtiene todos los empleados de la base de datos.
     *
     * @return Lista de empleados.
     * @throws Exception si ocurre un error en la consulta.
     */
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

    /**
     * Obtiene un empleado por su ID.
     *
     * @param id Identificador único del empleado.
     * @return Objeto Employee encontrado o null si no existe.
     * @throws Exception si ocurre un error en la consulta.
     */
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

    /**
     * Inserta un nuevo empleado en la base de datos.
     *
     * @param employee Objeto Employee con los datos a insertar.
     * @throws Exception si ocurre un error en la base de datos.
     */
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

    /**
     * Actualiza la información de un empleado en la base de datos.
     *
     * @param employee Objeto Employee con los datos actualizados.
     * @throws Exception si ocurre un error en la base de datos.
     */
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

    /**
     * Elimina un empleado de la base de datos por su ID.
     *
     * @param id Identificador del empleado a eliminar.
     * @throws Exception si ocurre un error en la base de datos.
     */
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

    /**
     * Mapea un ResultSet a un objeto Employee.
     *
     * @param rs ResultSet con los datos del empleado.
     * @return Objeto Employee.
     * @throws SQLException si ocurre un error al acceder a los datos.
     */
    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
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
