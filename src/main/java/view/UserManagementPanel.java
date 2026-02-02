package view;

import dao.UserDAO;
import model.User;
import util.UIConstants;
import util.ModernButton;
import util.PermissionManager;
import util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Modern User Management Panel with improved UI Only accessible by ADMIN
 */
public class UserManagementPanel extends JPanel {

    private UserDAO userDAO;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel statsLabel;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Color scheme
    private final Color ACTIVE_COLOR = new Color(220, 252, 231);      // Light green
    private final Color INACTIVE_COLOR = new Color(254, 226, 226);    // Light red
    private final Color ADMIN_COLOR = new Color(224, 231, 255);       // Light blue
    private final Color STAFF_COLOR = new Color(254, 243, 199);       // Light yellow
    // Đã xóa ACCOUNTANT_COLOR vì không còn dùng

    public UserManagementPanel() {
        this.userDAO = new UserDAO();

        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND_COLOR);

        createModernHeader();
        createStatsPanel();
        createModernTablePanel();
        createModernActionPanel();

        loadUsers();
    }

    /**
     * Create modern header with gradient background
     */
    private void createModernHeader() {
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Gradient background
                GradientPaint gp = new GradientPaint(
                        0, 0, UIConstants.PRIMARY_COLOR,
                        getWidth(), 0, UIConstants.PRIMARY_DARK
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(0, 120));
        headerPanel.setBorder(new EmptyBorder(25, 35, 25, 35));

        // Title section
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel("👥");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Quản Lý Tài Khoản");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Quản lý người dùng hệ thống");
        subtitleLabel.setFont(UIConstants.FONT_REGULAR);
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitleLabel);

        titlePanel.add(iconLabel);
        titlePanel.add(textPanel);

        // Search section
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchPanel.setOpaque(false);

        searchField = new JTextField(25);
        searchField.setFont(UIConstants.FONT_REGULAR);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(),
                new EmptyBorder(10, 15, 10, 15)
        ));
        searchField.setBackground(Color.WHITE);

        // Add search icon placeholder
        searchField.putClientProperty("JTextField.placeholderText", "🔍 Tìm kiếm theo tên hoặc username...");

        // Real-time search
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterUsers();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterUsers();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterUsers();
            }
        });

        ModernButton refreshButton = new ModernButton("🔄 Làm Mới", Color.WHITE);
        refreshButton.setForeground(UIConstants.PRIMARY_COLOR);
        refreshButton.setPreferredSize(new Dimension(130, 40));
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadUsers();
        });

        searchPanel.add(searchField);
        searchPanel.add(refreshButton);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
    }

    /**
     * Create statistics panel
     */
    private void createStatsPanel() {
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_COLOR),
                new EmptyBorder(5, 35, 5, 35)
        ));

        statsLabel = new JLabel();
        statsLabel.setFont(UIConstants.FONT_REGULAR);
        statsLabel.setForeground(UIConstants.TEXT_SECONDARY);

        statsPanel.add(statsLabel);

        add(statsPanel, BorderLayout.NORTH);
    }

    /**
     * Create modern table panel with custom renderer
     */
    private void createModernTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(new EmptyBorder(0, 35, 20, 35));

        String[] columns = {"ID", "Username", "Họ Tên", "Vai Trò", "Trạng Thái", "Đăng Nhập Cuối"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setFont(UIConstants.FONT_REGULAR);
        userTable.setRowHeight(55);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setShowGrid(false);
        userTable.setIntercellSpacing(new Dimension(0, 8));
        userTable.setSelectionBackground(new Color(99, 102, 241, 30));
        userTable.setSelectionForeground(UIConstants.TEXT_PRIMARY);

        // Modern table header
        JTableHeader header = userTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(UIConstants.BACKGROUND_COLOR);
        header.setForeground(UIConstants.TEXT_SECONDARY);
        header.setPreferredSize(new Dimension(0, 45));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.BORDER_COLOR));

        // Column widths
        userTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        userTable.getColumnModel().getColumn(0).setMaxWidth(80);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        userTable.getColumnModel().getColumn(5).setPreferredWidth(180);

        // Custom cell renderer
        userTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : UIConstants.BACKGROUND_COLOR);
                }

                ((JLabel) c).setBorder(new EmptyBorder(8, 12, 8, 12));
                setFont(UIConstants.FONT_REGULAR);

                // Special formatting for role column
                if (column == 3) { // Role column
                    String role = value.toString();
                    JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                    rolePanel.setOpaque(false);

                    JLabel roleLabel = new JLabel(value.toString());
                    roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    roleLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
                    roleLabel.setOpaque(true);

                    if (role.contains("Quản trị")) {
                        roleLabel.setBackground(ADMIN_COLOR);
                        roleLabel.setForeground(new Color(55, 65, 81));
                    } else if (role.contains("Nhân viên")) {
                        roleLabel.setBackground(STAFF_COLOR);
                        roleLabel.setForeground(new Color(146, 64, 14));
                    } else if (role.contains("Quản lý")) {
                        // Thêm màu cho Manager nếu chưa có
                        roleLabel.setBackground(new Color(224, 242, 254)); // Light Blue 100
                        roleLabel.setForeground(new Color(3, 105, 161));   // Sky 700
                    }

                    return roleLabel;
                }

                // Special formatting for status column
                if (column == 4) { // Status column
                    String status = value.toString();
                    JLabel statusLabel = new JLabel(value.toString());
                    statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    statusLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
                    statusLabel.setOpaque(true);
                    statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

                    if (status.equals("Hoạt động")) {
                        statusLabel.setText("✓ Hoạt động");
                        statusLabel.setBackground(ACTIVE_COLOR);
                        statusLabel.setForeground(new Color(21, 128, 61));
                    } else {
                        statusLabel.setText("✗ Vô hiệu");
                        statusLabel.setBackground(INACTIVE_COLOR);
                        statusLabel.setForeground(new Color(185, 28, 28));
                    }

                    return statusLabel;
                }

                return c;
            }
        });

        // Double-click to edit
        userTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    editUser();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * Create modern action panel with styled buttons
     */
    private void createModernActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(new EmptyBorder(0, 35, 30, 35));

        ModernButton addButton = new ModernButton("➕ Thêm Tài Khoản", UIConstants.SUCCESS_COLOR);
        addButton.setPreferredSize(new Dimension(180, 45));
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addButton.addActionListener(e -> addUser());

        ModernButton editButton = new ModernButton("✏️ Sửa Thông Tin", UIConstants.WARNING_COLOR);
        editButton.setPreferredSize(new Dimension(160, 45));
        editButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        editButton.addActionListener(e -> editUser());

        ModernButton toggleButton = new ModernButton("🔄 Đổi Trạng Thái", UIConstants.INFO_COLOR);
        toggleButton.setPreferredSize(new Dimension(160, 45));
        toggleButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        toggleButton.addActionListener(e -> toggleUserStatus());

        ModernButton deleteButton = new ModernButton("🗑️ Xóa Vĩnh Viễn", UIConstants.DANGER_COLOR);
        deleteButton.setPreferredSize(new Dimension(160, 45));
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteButton.addActionListener(e -> deleteUser());

        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(toggleButton);
        actionPanel.add(deleteButton);

        add(actionPanel, BorderLayout.SOUTH);
    }

    /**
     * Load users from database - ĐÃ SỬA LỖI COMPILATION
     */
    private void loadUsers() {
        tableModel.setRowCount(0);
        List<User> users = userDAO.getAllUsers();

        int activeCount = 0;
        int adminCount = 0;
        int managerCount = 0;

        for (User user : users) {
            // ✅ SỬA: Ẩn tài khoản STAFF khỏi danh sách của Admin
            if ("STAFF".equalsIgnoreCase(user.getRole())) {
                continue;
            }

            String lastLogin = (user.getLastLogin() != null)
                    ? dateFormat.format(user.getLastLogin()) : "Chưa đăng nhập";

            Object[] row = {
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRoleDisplayName(),
                user.isActive() ? "Hoạt động" : "Vô hiệu",
                lastLogin
            };
            tableModel.addRow(row);

            // Count statistics (chỉ đếm những người hiển thị)
            if (user.isActive()) activeCount++;
            if (user.isAdmin()) adminCount++;
            else if (user.isManager()) managerCount++;
        }

        // Update statistics (Bỏ đếm Staff)
        statsLabel.setText(String.format(
                "📊 Tổng: %d tài khoản  |  ✓ Hoạt động: %d  |  👑 Admin: %d  |  🏢 Manager: %d",
                tableModel.getRowCount(), activeCount, adminCount, managerCount
        ));
    }

    /**
     * Filter users based on search text
     */
    private void filterUsers() {
        String searchText = searchField.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            loadUsers();
            return;
        }

        tableModel.setRowCount(0);
        List<User> users = userDAO.getAllUsers();

        for (User user : users) {
            // ✅ SỬA: Ẩn tài khoản STAFF khi tìm kiếm
            if ("STAFF".equalsIgnoreCase(user.getRole())) {
                continue;
            }

            if (user.getUsername().toLowerCase().contains(searchText)
                    || user.getFullName().toLowerCase().contains(searchText)) {

                String lastLogin = (user.getLastLogin() != null)
                        ? dateFormat.format(user.getLastLogin()) : "Chưa đăng nhập";

                Object[] row = {
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getRoleDisplayName(),
                    user.isActive() ? "Hoạt động" : "Vô hiệu",
                    lastLogin
                };
                tableModel.addRow(row);
            }
        }
    }

    /**
     * Add new user
     */
    private void addUser() {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        UserDialog dialog = new UserDialog(parentFrame);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            User user = dialog.getUser();
            if (userDAO.insertUser(user)) {
                JOptionPane.showMessageDialog(this,
                        "✓ Thêm tài khoản thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this,
                        "✗ Thêm tài khoản thất bại!\nUsername có thể đã tồn tại.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Edit selected user
     */
    private void editUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "⚠ Vui lòng chọn tài khoản cần sửa!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        User user = userDAO.getUserById(id);

        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "✗ Không tìm thấy tài khoản!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        UserDialog dialog = new UserDialog(parentFrame, user);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            User updatedUser = dialog.getUser();
            if (userDAO.updateUser(updatedUser)) {
                JOptionPane.showMessageDialog(this,
                        "✓ Cập nhật tài khoản thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this,
                        "✗ Cập nhật tài khoản thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Toggle user status (Active/Inactive)
     */
    private void toggleUserStatus() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "⚠ Vui lòng chọn tài khoản!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);
        String currentStatus = (String) tableModel.getValueAt(selectedRow, 4);

        // Check if trying to disable own account
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(id)) {
            JOptionPane.showMessageDialog(this,
                    "⚠ Không thể vô hiệu hóa tài khoản đang đăng nhập!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isActive = currentStatus.equals("Hoạt động");
        String action = isActive ? "vô hiệu hóa" : "kích hoạt";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn " + action + " tài khoản '" + username + "'?",
                "Xác Nhận",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            User user = userDAO.getUserById(id);
            user.setActive(!isActive);

            if (userDAO.updateUser(user)) {
                JOptionPane.showMessageDialog(this,
                        "✓ Đã " + action + " tài khoản thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this,
                        "✗ " + action + " tài khoản thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Delete user permanently
     */
    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "⚠ Vui lòng chọn tài khoản cần xóa!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);

        // Check if trying to delete own account
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(id)) {
            JOptionPane.showMessageDialog(this,
                    "⚠ Không thể xóa tài khoản đang đăng nhập!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Warning message
        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠️ CẢNH BÁO: Hành động này không thể hoàn tác!\n\n"
                + "Bạn có chắc chắn muốn XÓA VĨNH VIỄN tài khoản '" + username + "'?\n\n"
                + "Nếu chỉ muốn tạm thời vô hiệu hóa, hãy dùng nút 'Đổi Trạng Thái'.",
                "Xác Nhận Xóa Vĩnh Viễn",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (userDAO.permanentlyDeleteUser(id)) {
                JOptionPane.showMessageDialog(this,
                        "✓ Đã xóa tài khoản vĩnh viễn!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this,
                        "✗ Xóa tài khoản thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
