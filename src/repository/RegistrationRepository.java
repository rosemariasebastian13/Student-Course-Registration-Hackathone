
package repository;

import database.DBConnection;
import model.Course;
import model.Registration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RegistrationRepository {

    // Register a student only if the course is available
    // and has not reached its capacity.
    public void save(int studentId, int courseId) {

        String sql = """
                INSERT INTO registrations (student_id, course_id)
                SELECT ?, c.course_id
                FROM courses c
                WHERE c.course_id = ?
                  AND c.available = 1
                  AND (
                      SELECT COUNT(*)
                      FROM registrations r
                      WHERE r.course_id = c.course_id
                  ) < c.capacity
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, studentId);
            statement.setInt(2, courseId);

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted == 0) {
                throw new IllegalArgumentException(
                        "Course is unavailable, full, or not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not register student. The student may already "
                    + "be registered for this course, or the student/course "
                    + "ID may be invalid.", e);
        }
    }

    // Cancel a student's registration
    public void delete(int studentId, int courseId) {

        String sql = """
                DELETE FROM registrations
                WHERE student_id = ? AND course_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, studentId);
            statement.setInt(2, courseId);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not cancel registration.", e);
        }
    }

    // Find registration records belonging to a student
    public List<Registration> findByStudentId(int studentId) {

        String sql = """
                SELECT registration_id, student_id, course_id,
                       registration_date
                FROM registrations
                WHERE student_id = ?
                ORDER BY registration_date DESC
                """;

        List<Registration> registrations = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, studentId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    registrations.add(new Registration(
                            result.getInt("registration_id"),
                            result.getInt("student_id"),
                            result.getInt("course_id"),
                            result.getString("registration_date")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not retrieve student registrations.", e);
        }

        return registrations;
    }

    // Check whether a student is registered for a course
    public boolean exists(int studentId, int courseId) {

        String sql = """
                SELECT 1
                FROM registrations
                WHERE student_id = ? AND course_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, studentId);
            statement.setInt(2, courseId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not check registration.", e);
        }
    }

    // Find registered courses with capacity and enrolled count
    public List<Course> findRegisteredCourses(int studentId) {

        String sql = """
                SELECT c.course_id,
                       c.course_code,
                       c.course_name,
                       c.credits,
                       c.available,
                       c.capacity,
                       COUNT(allr.registration_id) AS enrolled
                FROM registrations studentr
                JOIN courses c
                    ON c.course_id = studentr.course_id
                LEFT JOIN registrations allr
                    ON allr.course_id = c.course_id
                WHERE studentr.student_id = ?
                GROUP BY c.course_id
                ORDER BY c.course_code
                """;

        List<Course> courses = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, studentId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    courses.add(new Course(
                            result.getInt("course_id"),
                            result.getString("course_code"),
                            result.getString("course_name"),
                            result.getInt("credits"),
                            result.getInt("available") == 1,
                            result.getInt("capacity"),
                            result.getInt("enrolled")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not retrieve registered courses.", e);
        }

        return courses;
    }

    // Get registration details for the admin dashboard
    public List<Object[]> findAllRegistrationDetails() {

        String sql = """
                SELECT u.first_name, u.last_name, u.username,
                       c.course_code, c.course_name,
                       r.registration_date
                FROM registrations r
                JOIN users u
                    ON u.user_id = r.student_id
                JOIN courses c
                    ON c.course_id = r.course_id
                WHERE u.role = 'STUDENT'
                ORDER BY u.last_name, u.first_name, c.course_code
                """;

        List<Object[]> registrations = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                String fullName = (
                        result.getString("first_name") + " "
                        + result.getString("last_name")).trim();

                registrations.add(new Object[]{
                        fullName,
                        result.getString("username"),
                        result.getString("course_code"),
                        result.getString("course_name"),
                        result.getString("registration_date")
                });
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not retrieve all registration details.", e);
        }

        return registrations;
    }
    // Member 3: Certify a registered student's course completion
    public boolean certifyCompletion(int registrationId) {

        String sql = """
            UPDATE registrations
            SET completed = 1
            WHERE registration_id = ?
              AND completed = 0
              AND student_id IN (
                  SELECT user_id FROM users
                  WHERE role = 'STUDENT'
              )
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, registrationId);

            int updatedRows = statement.executeUpdate();

            return updatedRows == 1;

        } catch (SQLException e) {
            throw new RuntimeException(
                "Could not certify course completion.", e);
        }
    }

}
