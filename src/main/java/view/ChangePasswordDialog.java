package view;

import dao.UserDAO;
import model.User;
import util.SessionManager;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChangePasswordDialog extends JDialog {

    private JPasswordField txtOldPass;
    private JPasswordField txtNewPass;
    private JPasswordField txtConfirmPass;
    private final UserDAO userDAO = new UserDAO();

    public ChangePasswordDialog(JFrame parent) {
        super(parent, "Đổi Mật Khẩu", true);
        initUI();
    }

    private void initUI() {
        setSize(400, 450);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        header.setBackground(new Color(33, 150, 243));
        header.setBorder(new EmptyBorder(15, 0, 15, 0));
        JLabel title = new JLabel("Đổi Mật Khẩu");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title);
        add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        gbc.gridy = 0;
        form.add(createLabel("Mật khẩu hiện tại *"), gbc);
        gbc.gridy = 1;
        txtOldPass = createPasswordField();
        form.add(txtOldPass, gbc);

        gbc.gridy = 2;
        form.add(createLabel("Mật khẩu mới *"), gbc);
        gbc.gridy = 3;
        txtNewPass = createPasswordField();
        form.add(txtNewPass, gbc);

        gbc.gridy = 4;
        form.add(createLabel("Xác nhận mật khẩu mới *"), gbc);
        gbc.gridy = 5;
        txtConfirmPass = createPasswordField();
        form.add(txtConfirmPass, gbc);

        add(form, BorderLayout.CENTER);

        // Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        footer.setBackground(new Color(248, 250, 252));

        JButton btnCancel = new JButton("Hủy");
        styleButton(btnCancel, new Color(226, 232, 240), Color.BLACK);
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("Lưu thay đổi");
        styleButton(btnSave, new Color(33, 150, 243), Color.WHITE);
        btnSave.addActionListener(e -> savePassword());

        footer.add(btnCancel);
        footer.add(btnSave);
        add(footer, BorderLayout.SOUTH);
    }

    private void savePassword() {
        String oldPass = new String(txtOldPass.getPassword());
        String newPass = new String(txtNewPass.getPassword());
        String confirmPass = new String(txtConfirmPass.getPassword());

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newPass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu mới phải có ít nhất 6 ký tự!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User currentUser = SessionManager.getInstance().getCurrentUser();

        // 1. Kiểm tra mật khẩu cũ
        if (!userDAO.verifyCurrentPassword(currentUser.getId(), oldPass)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu hiện tại không đúng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Cập nhật mật khẩu mới
        if (userDAO.changePassword(currentUser.getId(), newPass)) {
            JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Có lỗi xảy ra, vui lòng thử lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(new Color(100, 116, 139));
        lbl.setBorder(new EmptyBorder(0, 0, 5, 0));
        return lbl;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setPreferredSize(new Dimension(0, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(5, 10, 5, 10)
        ));
        return field;
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(120, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
