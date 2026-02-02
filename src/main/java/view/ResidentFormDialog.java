package view;

import dao.ResidentDAO;
import model.Resident;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dialog for creating or editing residents
 */
public class ResidentFormDialog extends JDialog {

    private ResidentDAO residentDAO;
    private Resident resident;
    private boolean isEditMode;
    private boolean isConfirmed = false;

    // Form components
    private JTextField txtFullName;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextField txtIdentityCard;
    private JComboBox<String> cmbGender;
    private JSpinner spnDob;
    private JTextField txtHometown;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ResidentFormDialog(JFrame parent, Resident resident) {
        super(parent, "Cư Dân", true);

        this.residentDAO = new ResidentDAO();
        this.resident = resident != null ? resident : new Resident();
        this.isEditMode = resident != null && resident.getId() != null;

        initComponents();

        if (isEditMode) {
            loadResidentData();
        }

        setSize(600, 650);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);

        // Main panel with scroll
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header
        mainPanel.add(createHeader());
        mainPanel.add(Box.createVerticalStrut(20));

        // Personal Info Section
        mainPanel.add(createPersonalInfoSection());
        mainPanel.add(Box.createVerticalStrut(15));

        // Contact Info Section
        mainPanel.add(createContactInfoSection());
        mainPanel.add(Box.createVerticalStrut(15));

        // Additional Info Section
        mainPanel.add(createAdditionalInfoSection());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(20, 25, 20, 25)
        ));

        JLabel iconLabel = new JLabel(isEditMode ? "✏️" : "➕");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JLabel titleLabel = new JLabel(isEditMode ? "Chỉnh Sửa Cư Dân" : "Thêm Cư Dân Mới");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(33, 33, 33));

        headerPanel.add(iconLabel);
        headerPanel.add(titleLabel);

        return headerPanel;
    }

    private JPanel createPersonalInfoSection() {
        JPanel section = createSection("👤 Thông Tin Cá Nhân");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);

        // Row 1: Full Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Họ và tên:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        txtFullName = new JTextField();
        txtFullName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtFullName.setPreferredSize(new Dimension(0, 35));
        section.add(txtFullName, gbc);

        // Row 2: Gender + DOB
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        section.add(createLabel("Giới tính:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        cmbGender = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cmbGender.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbGender.setPreferredSize(new Dimension(0, 35));
        section.add(cmbGender, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        section.add(createLabel("Ngày sinh:", false), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        spnDob = createDateSpinner();
        section.add(spnDob, gbc);

        // Row 3: Identity Card
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        section.add(createLabel("CCCD/CMND:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        txtIdentityCard = new JTextField();
        txtIdentityCard.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtIdentityCard.setPreferredSize(new Dimension(0, 35));
        section.add(txtIdentityCard, gbc);

        return section;
    }

    private JPanel createContactInfoSection() {
        JPanel section = createSection("📞 Thông Tin Liên Hệ");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);

        // Row 1: Phone
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Số điện thoại:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        txtPhone = new JTextField();
        txtPhone.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPhone.setPreferredSize(new Dimension(0, 35));
        section.add(txtPhone, gbc);

        // Row 2: Email
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        section.add(createLabel("Email:", false), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        txtEmail = new JTextField();
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEmail.setPreferredSize(new Dimension(0, 35));
        section.add(txtEmail, gbc);

        return section;
    }

    private JPanel createAdditionalInfoSection() {
        JPanel section = createSection("🏠 Thông Tin Bổ Sung");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);

        // Hometown
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Quê quán:", false), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        txtHometown = new JTextField();
        txtHometown.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtHometown.setPreferredSize(new Dimension(0, 35));
        section.add(txtHometown, gbc);

        return section;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_COLOR));

        JButton btnCancel = createButton("Hủy", new Color(158, 158, 158));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = createButton(isEditMode ? "💾 Lưu" : "✅ Thêm", UIConstants.PRIMARY_COLOR);
        btnSave.addActionListener(e -> saveResident());

        panel.add(btnCancel);
        panel.add(btnSave);

        return panel;
    }

    // ===== HELPER METHODS =====
    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 15),
                new Color(66, 66, 66)
        ));
        return panel;
    }

    private JLabel createLabel(String text, boolean required) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (required) {
            label.setText("<html>" + text + " <font color='red'>*</font></html>");
        }
        return label;
    }

    private JSpinner createDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        spinner.setEditor(editor);
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(0, 35));

        // Set default to 30 years ago
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.YEAR, -30);
        spinner.setValue(cal.getTime());

        return spinner;
    }

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ===== DATA LOADING =====
    private void loadResidentData() {
        txtFullName.setText(resident.getFullName());
        txtPhone.setText(resident.getPhone());

        if (resident.getEmail() != null) {
            txtEmail.setText(resident.getEmail());
        }

        if (resident.getIdentityCard() != null) {
            txtIdentityCard.setText(resident.getIdentityCard());
        }

        if (resident.getGender() != null) {
            cmbGender.setSelectedItem(resident.getGender());
        }

        if (resident.getDob() != null) {
            spnDob.setValue(resident.getDob());
        }

        if (resident.getHometown() != null) {
            txtHometown.setText(resident.getHometown());
        }
    }

    // ===== VALIDATION & SAVE =====
    private boolean validateForm() {
        // Full Name
        if (txtFullName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập họ và tên!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            txtFullName.requestFocus();
            return false;
        }

        // Phone
        String phone = txtPhone.getText().trim();
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập số điện thoại!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            txtPhone.requestFocus();
            return false;
        }

        // Validate phone format (10-11 digits)
        if (!phone.matches("^[0-9]{10,11}$")) {
            JOptionPane.showMessageDialog(this,
                    "Số điện thoại không hợp lệ!\nVui lòng nhập 10-11 chữ số.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            txtPhone.requestFocus();
            return false;
        }

        // Identity Card
        String identityCard = txtIdentityCard.getText().trim();
        if (identityCard.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập CCCD/CMND!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            txtIdentityCard.requestFocus();
            return false;
        }

        // Validate identity card format (9 or 12 digits)
        if (!identityCard.matches("^[0-9]{9}$") && !identityCard.matches("^[0-9]{12}$")) {
            JOptionPane.showMessageDialog(this,
                    "CCCD/CMND không hợp lệ!\nCMND: 9 số, CCCD: 12 số.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            txtIdentityCard.requestFocus();
            return false;
        }

        // Email (optional but must be valid if provided)
        String email = txtEmail.getText().trim();
        if (!email.isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                JOptionPane.showMessageDialog(this,
                        "Email không hợp lệ!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                txtEmail.requestFocus();
                return false;
            }
        }

        // Check duplicate identity card (only for new resident or changed identity card)
        if (!isEditMode || !identityCard.equals(resident.getIdentityCard())) {
            if (isIdentityCardExists(identityCard)) {
                JOptionPane.showMessageDialog(this,
                        "CCCD/CMND này đã được sử dụng bởi cư dân khác!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                txtIdentityCard.requestFocus();
                return false;
            }
        }

        return true;
    }

    private boolean isIdentityCardExists(String identityCard) {
        java.util.List<Resident> residents = residentDAO.getAllResidents();
        for (Resident r : residents) {
            if (r.getIdentityCard() != null
                    && r.getIdentityCard().equals(identityCard)
                    && !r.getId().equals(resident.getId())) {
                return true;
            }
        }
        return false;
    }

    private void saveResident() {
        if (!validateForm()) {
            return;
        }

        try {
            // Set form data to resident object
            resident.setFullName(txtFullName.getText().trim());
            resident.setPhone(txtPhone.getText().trim());

            String email = txtEmail.getText().trim();
            resident.setEmail(email.isEmpty() ? null : email);

            resident.setIdentityCard(txtIdentityCard.getText().trim());
            resident.setGender((String) cmbGender.getSelectedItem());
            resident.setDob((Date) spnDob.getValue());

            String hometown = txtHometown.getText().trim();
            resident.setHometown(hometown.isEmpty() ? null : hometown);

            // Save to database
            boolean success;
            if (isEditMode) {
                success = residentDAO.updateResident(resident);
            } else {
                success = residentDAO.insertResident(resident);
            }

            if (success) {
                isConfirmed = true;
                JOptionPane.showMessageDialog(this,
                        isEditMode ? "Cập nhật cư dân thành công!" : "Thêm cư dân thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        isEditMode ? "Cập nhật cư dân thất bại!" : "Thêm cư dân thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Có lỗi xảy ra: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== GETTERS =====
    public boolean isConfirmed() {
        return isConfirmed;
    }

    public Resident getResident() {
        return resident;
    }
}
