package view;

import model.User;
import dao.UserDAO;
import util.UIConstants;
import util.ModernButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * User Dialog - Popup for Add/Edit User Only accessible by ADMIN
 */
public class UserDialog extends JDialog {

    private UserDAO userDAO;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField fullNameField;
    private JComboBox<String> roleCombo;
    private JCheckBox activeCheckbox;

    private User user;
    private boolean confirmed = false;
    private boolean isEditMode = false;

    /**
     * Constructor for Add mode
     */
    public UserDialog(JFrame parent) {
        this(parent, null);
    }

    /**
     * Constructor for Edit mode
     */
    public UserDialog(JFrame parent, User user) {
        super(parent, user == null ? "Thêm Tài Khoản" : "Sửa Tài Khoản", true);
        this.user = user;
        this.isEditMode = (user != null);
        this.userDAO = new UserDAO();

        initializeDialog();
        createContent();

        if (user != null) {
            loadUserData();
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeDialog() {
        setSize(450, 550);
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

        JLabel iconLabel = new JLabel(isEditMode ? "✏️" : "➕");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));

        JLabel titleLabel = new JLabel(isEditMode ? "Sửa Tài Khoản" : "Thêm Tài Khoản Mới");
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

        // Username
        formPanel.add(createFieldLabel("Tên đăng nhập *"));
        usernameField = createTextField();
        usernameField.setEnabled(!isEditMode); // Disable in edit mode
        formPanel.add(usernameField);
        if (isEditMode) {
            JLabel hint = new JLabel("(Không thể thay đổi username)");
            hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            hint.setForeground(UIConstants.TEXT_SECONDARY);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            formPanel.add(hint);
        }
        formPanel.add(Box.createVerticalStrut(15));

        // Password
        formPanel.add(createFieldLabel(isEditMode ? "Mật khẩu (để trống nếu không đổi)" : "Mật khẩu *"));
        passwordField = new JPasswordField();
        passwordField.setFont(UIConstants.FONT_REGULAR);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        formPanel.add(passwordField);
        formPanel.add(Box.createVerticalStrut(15));

        // Full Name
        formPanel.add(createFieldLabel("Họ và tên *"));
        fullNameField = createTextField();
        formPanel.add(fullNameField);
        formPanel.add(Box.createVerticalStrut(15));

        // Role
        // Role
        formPanel.add(createFieldLabel("Vai trò *"));
        
        // ✅ SỬA: Chỉ cho phép tạo ADMIN hoặc MANAGER
        roleCombo = new JComboBox<>(new String[]{"ADMIN", "MANAGER"});
        
        roleCombo.setFont(UIConstants.FONT_REGULAR);
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(roleCombo);
        formPanel.add(Box.createVerticalStrut(15));

        // Active checkbox
        activeCheckbox = new JCheckBox("Tài khoản hoạt động");
        activeCheckbox.setFont(UIConstants.FONT_REGULAR);
        activeCheckbox.setBackground(Color.WHITE);
        activeCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        activeCheckbox.setSelected(true);
        formPanel.add(activeCheckbox);

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
                isEditMode ? "Lưu" : "Thêm",
                UIConstants.SUCCESS_COLOR
        );
        saveButton.setPreferredSize(new Dimension(100, 40));
        saveButton.addActionListener(e -> saveUser());

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

    private void loadUserData() {
        if (user != null) {
            usernameField.setText(user.getUsername());
            // Don't set password field
            fullNameField.setText(user.getFullName());
            roleCombo.setSelectedItem(user.getRole());
            activeCheckbox.setSelected(user.isActive());
        }
    }

    private void saveUser() {
        if (!validateForm()) {
            return;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String fullName = fullNameField.getText().trim();
        String role = (String) roleCombo.getSelectedItem();
        boolean isActive = activeCheckbox.isSelected();

        if (user == null) {
            // Create new user
            user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setFullName(fullName);
            user.setRole(role);
            user.setActive(isActive);
        } else {
            // Update existing user
            user.setFullName(fullName);
            user.setRole(role);
            user.setActive(isActive);

            // Only update password if provided
            if (!password.isEmpty()) {
                user.setPassword(password);
            }
        }

        confirmed = true;
        dispose();
    }

    private boolean validateForm() {
        // Username (only for new users)
        if (!isEditMode) {
            if (usernameField.getText().trim().isEmpty()) {
                showError("Vui lòng nhập tên đăng nhập!");
                usernameField.requestFocus();
                return false;
            }

            if (usernameField.getText().trim().length() < 3) {
                showError("Tên đăng nhập phải có ít nhất 3 ký tự!");
                usernameField.requestFocus();
                return false;
            }

            // Check if username exists
            if (userDAO.usernameExists(usernameField.getText().trim(), null)) {
                showError("Tên đăng nhập đã tồn tại!");
                usernameField.requestFocus();
                return false;
            }
        }

        // Password (required for new users, optional for edit)
        String password = new String(passwordField.getPassword()).trim();
        if (!isEditMode && password.isEmpty()) {
            showError("Vui lòng nhập mật khẩu!");
            passwordField.requestFocus();
            return false;
        }

        if (!password.isEmpty() && password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự!");
            passwordField.requestFocus();
            return false;
        }

        // Full name
        if (fullNameField.getText().trim().isEmpty()) {
            showError("Vui lòng nhập họ và tên!");
            fullNameField.requestFocus();
            return false;
        }

        if (fullNameField.getText().trim().length() < 2) {
            showError("Họ và tên phải có ít nhất 2 ký tự!");
            fullNameField.requestFocus();
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
     * Get the user (new or updated)
     */
    public User getUser() {
        return user;
    }
}
