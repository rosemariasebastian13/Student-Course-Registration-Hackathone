
package ui;

import model.User;
import repository.RegistrationRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminRegistrationsFrame extends JFrame {

    private final RegistrationRepository repository =
            new RegistrationRepository();

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[]{
                            "Student Name",
                            "Username",
                            "Course Code",
                            "Course Name",
                            "Registration Date",
                            "Registration ID",
                            "Completion Status"
                    }, 0) {

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    private final JButton certifyButton =
            new JButton("Certify Completion");

    public AdminRegistrationsFrame(User admin) {

        setTitle("Student Registrations");
        setSize(1050, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel title = new JLabel(
                "Student Registrations - " + admin.getFullName());

        title.setFont(new Font("Arial", Font.BOLD, 20));

        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(
                12, 12, 12, 12));

        topPanel.add(title, BorderLayout.CENTER);
        topPanel.add(refreshButton, BorderLayout.EAST);

        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        certifyButton.setEnabled(false);

        JPanel bottomPanel =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));

        bottomPanel.add(certifyButton);
        bottomPanel.add(closeButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Enable certification only for registered courses
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCertifyButton();
            }
        });

        refreshButton.addActionListener(e -> loadRegistrations());

        certifyButton.addActionListener(e -> certifyCompletion());

        closeButton.addActionListener(e -> dispose());

        loadRegistrations();
    }

    // Member 3: Enable button only for registered courses
    private void updateCertifyButton() {

        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            certifyButton.setEnabled(false);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);

        String status = tableModel.getValueAt(
                modelRow, 6).toString();

        certifyButton.setEnabled(
                "Registered".equalsIgnoreCase(status));
    }

    // Member 3: Certify completion with confirmation
    private void certifyCompletion() {

        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);

        String studentName = tableModel.getValueAt(
                modelRow, 0).toString();

        String courseCode = tableModel.getValueAt(
                modelRow, 2).toString();

        String courseName = tableModel.getValueAt(
                modelRow, 3).toString();

        String status = tableModel.getValueAt(
                modelRow, 6).toString();

        if (!"Registered".equalsIgnoreCase(status)) {
            certifyButton.setEnabled(false);
            return;
        }

        int registrationId = (Integer) tableModel.getValueAt(
                modelRow, 5);

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Certify completion for:\n\n"
                        + "Student: " + studentName
                        + "\nCourse: " + courseCode
                        + " - " + courseName
                        + "\n\nAre you sure?",
                "Confirm Course Completion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            boolean certified =
                    repository.certifyCompletion(registrationId);

            if (certified) {

                JOptionPane.showMessageDialog(
                        this,
                        "Course completion certified successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Registration not found or already completed.",
                        "Certification Failed",
                        JOptionPane.WARNING_MESSAGE);
            }

            loadRegistrations();

        } catch (RuntimeException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Could not certify course completion.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Load registrations and completion status
    private void loadRegistrations() {

        try {

            List<Object[]> registrations =
                    repository.findAllRegistrationDetails();

            tableModel.setRowCount(0);

            for (Object[] registration : registrations) {
                tableModel.addRow(registration);
            }

            certifyButton.setEnabled(false);

        } catch (RuntimeException e) {

            certifyButton.setEnabled(false);

            JOptionPane.showMessageDialog(
                    this,
                    "Could not load student registrations.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
