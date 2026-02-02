package view;

import model.Resident;
import util.UIConstants;
import util.ModernButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Resident Dialog - Popup for Add/Edit Resident Features: Date picker,
 * Phone/Email/ID Card validation
 */
public class ResidentDialog extends JDialog {

    private JTextField fullNameField;
    private JTextField dobField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField idCardField;

    private Resident resident;
    private boolean confirmed = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Constructor for Add mode
     */
    public ResidentDialog(JFrame parent) {
        this(parent, null);
    }

    /**
     * Constructor for Edit mode
     */
    public ResidentDialog(JFrame parent, Resident resident) {
        super(parent, resident == null ? "Thêm Cư Dân" : "Sửa Cư Dân", true);
        this.resident = resident;

        initializeDialog();
        createContent();

        if (resident != null) {
            loadResidentData();
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeDialog() {
        setSize(500, 600);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);
    }

    private void createContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);

        // Form
        mainPanel.add(createForm(), BorderLayout.CENTER);

        // Buttons
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JLabel iconLabel = new JLabel(resident == null ? "➕" : "✏️");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));

        JLabel titleLabel = new JLabel(resident == null ? "Thêm Cư Dân Mới" : "Sửa Thông Tin Cư Dân");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(titleLabel);

        return headerPanel;
    }

    private JPanel createForm() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // Full name
        formPanel.add(createFieldLabel("Họ và Tên *"));
        fullNameField = createTextField();
        formPanel.add(fullNameField);
        formPanel.add(Box.createVerticalStrut(15));

        // Date of birth
        formPanel.add(createFieldLabel("Ngày Sinh (yyyy-MM-dd) *"));
        dobField = createTextField();
        dobField.setToolTipText("Ví dụ: 1990-05-15");
        formPanel.add(dobField);

        // Date hint
        JLabel dateHint = new JLabel("Ví dụ: 1990-05-15");
        dateHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateHint.setForeground(UIConstants.TEXT_SECONDARY);
        dateHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(dateHint);
        formPanel.add(Box.createVerticalStrut(15));

        // Phone
        formPanel.add(createFieldLabel("Điện Thoại (10-11 số) *"));
        phoneField = createTextField();
        formPanel.add(phoneField);
        formPanel.add(Box.createVerticalStrut(15));

        // Email
        formPanel.add(createFieldLabel("Email"));
        emailField = createTextField();
        formPanel.add(emailField);
        formPanel.add(Box.createVerticalStrut(15));

        // ID Card
        formPanel.add(createFieldLabel("CMND/CCCD (9 hoặc 12 số) *"));
        idCardField = createTextField();
        formPanel.add(idCardField);

        return formPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        ModernButton cancelButton = new ModernButton("Hủy", UIConstants.TEXT_SECONDARY);
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        ModernButton saveButton = new ModernButton(
                resident == null ? "Thêm" : "Lưu",
                UIConstants.SUCCESS_COLOR
        );
        saveButton.setPreferredSize(new Dimension(100, 40));
        saveButton.addActionListener(e -> saveResident());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(UIConstants.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(UIConstants.FONT_REGULAR);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        // Focus border effect
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 2),
                        new EmptyBorder(9, 11, 9, 11)
                ));
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                        new EmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        return field;
    }

    private void loadResidentData() {
        if (resident != null) {
            fullNameField.setText(resident.getFullName());
            dobField.setText(resident.getDob() != null ? dateFormat.format(resident.getDob()) : "");
            phoneField.setText(resident.getPhone());
            emailField.setText(resident.getEmail());
            idCardField.setText(resident.getIdCard());
        }
    }

    private void saveResident() {
        if (!validateForm()) {
            return;
        }

        try {
            String fullName = fullNameField.getText().trim();
            Date dob = dateFormat.parse(dobField.getText().trim());
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String idCard = idCardField.getText().trim();

            if (resident == null) {
                // Create new resident
                resident = new Resident();
                resident.setFullName(fullName);
                resident.setDob(dob);
                resident.setPhone(phone);
                resident.setEmail(email);
                resident.setIdCard(idCard);
            } else {
                // Update existing resident
                resident.setFullName(fullName);
                resident.setDob(dob);
                resident.setPhone(phone);
                resident.setEmail(email);
                resident.setIdCard(idCard);
            }

            confirmed = true;
            dispose();
        } catch (ParseException e) {
            showError("Định dạng ngày sinh không đúng! Vui lòng nhập theo định dạng yyyy-MM-dd");
        }
    }

    private boolean validateForm() {
        // Full name
        if (fullNameField.getText().trim().isEmpty()) {
            showError("Vui lòng nhập họ tên!");
            fullNameField.requestFocus();
            return false;
        }

        if (fullNameField.getText().trim().length() < 2) {
            showError("Họ tên phải có ít nhất 2 ký tự!");
            fullNameField.requestFocus();
            return false;
        }

        // Date of birth
        if (dobField.getText().trim().isEmpty()) {
            showError("Vui lòng nhập ngày sinh!");
            dobField.requestFocus();
            return false;
        }

        // Validate date format
        try {
            Date dob = dateFormat.parse(dobField.getText().trim());

            // Check if date is not in the future
            if (dob.after(new Date())) {
                showError("Ngày sinh không thể là ngày trong tương lai!");
                dobField.requestFocus();
                return false;
            }
        } catch (ParseException e) {
            showError("Định dạng ngày sinh không đúng! Vui lòng nhập theo định dạng yyyy-MM-dd (Ví dụ: 1990-05-15)");
            dobField.requestFocus();
            return false;
        }

        // Phone
        if (phoneField.getText().trim().isEmpty()) {
            showError("Vui lòng nhập số điện thoại!");
            phoneField.requestFocus();
            return false;
        }

        String phone = phoneField.getText().trim();
        if (!phone.matches("\\d{10,11}")) {
            showError("Số điện thoại phải có 10-11 chữ số!");
            phoneField.requestFocus();
            return false;
        }

        // ID card
        if (idCardField.getText().trim().isEmpty()) {
            showError("Vui lòng nhập CMND/CCCD!");
            idCardField.requestFocus();
            return false;
        }

        String idCard = idCardField.getText().trim();
        if (!idCard.matches("\\d{9}") && !idCard.matches("\\d{12}")) {
            showError("CMND/CCCD phải có 9 hoặc 12 chữ số!");
            idCardField.requestFocus();
            return false;
        }

        // Email (optional but validate if provided)
        String email = emailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Định dạng email không hợp lệ!");
            emailField.requestFocus();
            return false;
        }

        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Cảnh Báo",
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Check if user confirmed the dialog
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Get the resident (new or updated)
     */
    public Resident getResident() {
        return resident;
    }
}
