package view;

import dao.UserDAO;
import model.User;
import util.SessionManager;
import util.UIConstants;

import dao.BuildingDAO;
import model.Building;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ✅ THÊM: Các biến cho việc chọn nhiều tòa nhà
    private JPanel buildingContainer;
    private Map<Long, JCheckBox> buildingCheckboxes = new HashMap<>();
    private final BuildingDAO buildingDAO = new BuildingDAO();

    private boolean confirmed = false;
    private User staffUser;
    private User currentManager;
    private boolean isEditMode;

    public MyStaffDialog(JFrame parent, User manager) {
            super(parent, "Thêm Nhân Viên", true);
            this.currentManager = manager;
            this.staffUser = new User();
            this.isEditMode = false;
            initUI();
        }

    public MyStaffDialog(JFrame parent, User user, User manager) {
            super(parent, "Sửa Nhân Viên", true);
            this.currentManager = manager;
            this.staffUser = user;
            this.isEditMode = true;
            initUI();
            loadData();
        }

    private void initUI() {
        setSize(450, 650); // Tăng chiều cao để đủ chỗ hiển thị list tòa nhà
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
        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // 2. Form Content
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(25, 35, 10, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 10, 0); 
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Username
        gbc.gridy = 0;
        form.add(createLabel("Tên đăng nhập (Username) *"), gbc);
        gbc.gridy = 1;
        txtUsername = createTextField();
        txtUsername.setEnabled(!isEditMode);
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
        txtPassword.setPreferredSize(new Dimension(0, 40));
        form.add(txtPassword, gbc);

        // --- PHẦN DANH SÁCH TÒA NHÀ ---
        // --- PHẦN DANH SÁCH TÒA NHÀ (ĐÃ LÀM ĐẸP) ---
        gbc.gridy = 6;
        form.add(createLabel("Phân công tòa nhà (Chọn ít nhất 1) *"), gbc);
        
        gbc.gridy = 7;
        buildingContainer = new JPanel();
        // ✅ SỬA: Dùng FlowLayout để các tòa nhà dàn hàng ngang, tự xuống dòng
        buildingContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        buildingContainer.setBackground(new Color(248, 250, 252)); // Nền xám nhạt cực nhẹ
        buildingContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Lấy dữ liệu thực tế từ DB
        List<Building> allBuildings = buildingDAO.getAllBuildings();
        List<Long> managerBuildingIds = getBuildingIdsFromDB(currentManager.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentManager.getRole());

        if (!isAdmin && (managerBuildingIds == null || managerBuildingIds.isEmpty())) {
             JLabel lblEmpty = new JLabel("Bạn chưa quản lý tòa nhà nào!");
             lblEmpty.setForeground(Color.RED);
             buildingContainer.add(lblEmpty);
        } else {
            for (Building b : allBuildings) {
                if (isAdmin || managerBuildingIds.contains(b.getId())) {
                    JCheckBox cb = new JCheckBox(b.getName());
                    cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    cb.setOpaque(false); // Để hiện nền của buildingContainer
                    cb.setFocusPainted(false);
                    cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    buildingCheckboxes.put(b.getId(), cb);
                    buildingContainer.add(cb);
                }
            }
        }
        
        // ✅ THAY THẾ: Không dùng JScrollPane nữa, dùng trực tiếp buildingContainer
        form.add(buildingContainer, gbc);
        
        // Active Checkbox
        gbc.gridy = 8;
        gbc.insets = new Insets(10, 0, 0, 0);
        chkActive = new JCheckBox("Kích hoạt tài khoản");
        chkActive.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chkActive.setBackground(Color.WHITE);
        chkActive.setSelected(true);
        form.add(chkActive, gbc);

        // Khoảng trống co giãn đẩy mọi thứ lên trên
        gbc.gridy = 9;
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
        
        // ✅ THÊM: Tự động tick vào các tòa nhà nhân viên đang phụ trách
        List<Long> assignedIds = staffUser.getBuildingIds();
        if (assignedIds != null) {
            for (Long id : assignedIds) {
                if (buildingCheckboxes.containsKey(id)) {
                    buildingCheckboxes.get(id).setSelected(true);
                }
            }
        }
    }

    private void onSave() {
        if (txtUsername.getText().isBlank() || txtFullName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin bắt buộc!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // ✅ THÊM: Thu thập danh sách ID tòa nhà được chọn
        List<Long> selectedBuildingIds = new ArrayList<>();
        for (Map.Entry<Long, JCheckBox> entry : buildingCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedBuildingIds.add(entry.getKey());
            }
        }
        
        if (selectedBuildingIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng phân công ít nhất 1 tòa nhà!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserDAO dao = new UserDAO();
        
        staffUser.setUsername(txtUsername.getText().trim());
        staffUser.setFullName(txtFullName.getText().trim());
        staffUser.setRole("STAFF");
        staffUser.setActive(chkActive.isSelected());
        staffUser.setBuildingIds(selectedBuildingIds); // ✅ QUAN TRỌNG: Lưu list ID

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
    
    // ✅ THÊM HÀM NÀY: Lấy danh sách ID tòa nhà trực tiếp từ DB để đảm bảo chính xác
    private List<Long> getManagerBuildingIds(Long userId) {
        List<Long> ids = new ArrayList<>();
        String sql = "SELECT building_id FROM user_buildings WHERE user_id = ?";
        try (java.sql.Connection conn = connection.Db_connection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("building_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }
    
    // ✅ THÊM HÀM NÀY VÀO CUỐI CLASS (Để lấy dữ liệu trực tiếp từ DB)
    private java.util.List<Long> getBuildingIdsFromDB(Long userId) {
        java.util.List<Long> ids = new ArrayList<>();
        String sql = "SELECT building_id FROM user_buildings WHERE user_id = ?";
        try (java.sql.Connection conn = connection.Db_connection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("building_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }
}
