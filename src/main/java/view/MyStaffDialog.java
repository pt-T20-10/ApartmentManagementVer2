package view;

import dao.UserDAO;
import model.User;
import util.SessionManager;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * MyStaffDialog - Modern Form UI
 */
public class MyStaffDialog extends JDialog {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtFullName;
    private JCheckBox chkActive;

    private boolean confirmed = false;
    private User staffUser;
    private Long buildingId;
    private boolean isEditMode;

    public MyStaffDialog(JFrame parent, Long buildingId) {
        super(parent, "Thêm Nhân Viên", true);
        this.buildingId = buildingId;
        this.staffUser = new User();
        this.isEditMode = false;
        initUI();
    }

    public MyStaffDialog(JFrame parent, User user, Long buildingId) {
        super(parent, "Sửa Nhân Viên", true);
        this.staffUser = user;
        this.buildingId = buildingId;
        this.isEditMode = true;
        initUI();
        loadData();
    }

    private void initUI() {
        setSize(450, 550); // Taller for cleaner look
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);

        // 1. Header (Blue)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(33, 150, 243));
        header.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel lblTitle = new JLabel(isEditMode ? "Cập Nhật Nhân Viên" : "Thêm Nhân Viên Mới");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setIcon(new Icon() { // Simple User Icon
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(Color.WHITE);
                g.fillOval(x + 4, y, 16, 16);
                g.fillArc(x, y + 18, 24, 12, 0, 180);
            }

            public int getIconWidth() {
                return 24;
            }

            public int getIconHeight() {
                return 30;
            }
        });

        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // 2. Form Content
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(25, 35, 25, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0); // Bottom spacing
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Username
        gbc.gridy = 0;
        form.add(createLabel("Tên đăng nhập (Username) *"), gbc);
        gbc.gridy = 1;
        txtUsername = createTextField();
        txtUsername.setEnabled(!isEditMode); // Cannot change username
        form.add(txtUsername, gbc);

        // Full Name
        gbc.gridy = 2;
        form.add(createLabel("Họ và tên *"), gbc);
        gbc.gridy = 3;
        txtFullName = createTextField();
        form.add(txtFullName, gbc);

        // Password
        gbc.gridy = 4;
        String pwdLabel = isEditMode ? "Mật khẩu (để trống nếu không đổi)" : "Mật khẩu *";
        form.add(createLabel(pwdLabel), gbc);
        gbc.gridy = 5;
        txtPassword = new JPasswordField();
        styleTextField(txtPassword);
        txtPassword.setPreferredSize(new Dimension(0, 45));
        form.add(txtPassword, gbc);

        // Active Checkbox
        gbc.gridy = 6;
        gbc.insets = new Insets(20, 0, 0, 0);
        chkActive = new JCheckBox("Kích hoạt tài khoản");
        chkActive.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chkActive.setBackground(Color.WHITE);
        chkActive.setFocusPainted(false);
        chkActive.setSelected(true); // Default active
        form.add(chkActive, gbc);

        // Filler
        gbc.gridy = 7;
        gbc.weighty = 1.0;
        form.add(Box.createGlue(), gbc);

        add(form, BorderLayout.CENTER);

        // 3. Footer Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnCancel = createButton("Hủy", new Color(226, 232, 240), new Color(71, 85, 105));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = createButton("Lưu", new Color(33, 150, 243), Color.WHITE);
        btnSave.addActionListener(e -> onSave());

        footer.add(btnCancel);
        footer.add(btnSave);

        add(footer, BorderLayout.SOUTH);
    }

    // --- UI Helpers ---
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(100, 116, 139));
        label.setBorder(new EmptyBorder(0, 0, 5, 0));
        return label;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(0, 45));
        styleTextField(field);
        return field;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(100, 42));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void loadData() {
        txtUsername.setText(staffUser.getUsername());
        txtFullName.setText(staffUser.getFullName());
        chkActive.setSelected(staffUser.isActive());
    }

    private void onSave() {
        if (txtUsername.getText().isBlank() || txtFullName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin bắt buộc!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserDAO dao = new UserDAO();
        User currentManager = SessionManager.getInstance().getCurrentUser();

        staffUser.setUsername(txtUsername.getText().trim());
        staffUser.setFullName(txtFullName.getText().trim());
        staffUser.setRole("STAFF");
        staffUser.setActive(chkActive.isSelected());
        staffUser.setBuildingId(buildingId);

        String pwd = new String(txtPassword.getPassword());

        if (!isEditMode) {
            if (pwd.length() < 6) {
                JOptionPane.showMessageDialog(this, "Mật khẩu tối thiểu 6 ký tự!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            staffUser.setPassword(pwd);
            if (dao.insertUser(staffUser, currentManager)) {
                confirmed = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Tên đăng nhập đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            if (!pwd.isEmpty()) {
                if (pwd.length() < 6) {
                    JOptionPane.showMessageDialog(this, "Mật khẩu tối thiểu 6 ký tự!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                dao.changePassword(staffUser.getId(), pwd);
            }
            dao.updateUser(staffUser);
            confirmed = true;
            dispose();
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
