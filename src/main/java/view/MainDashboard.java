package view;

import util.UIConstants;
import util.PermissionManager;
import model.Building;
import model.Floor;
import model.User;
import util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;

/**
 * Main Dashboard - Sky Blue Theme Updated: Đã chuyển Building Selector sang
 * DashboardPanel
 */
public class MainDashboard extends JFrame implements DashboardNavigator {

    private JPanel sidebarPanel;
    private JPanel contentPanel;
    private PermissionManager permissionManager;

    // Menu buttons
    private SidebarButton btnDashboard;
    private SidebarButton btnBuildings;
    private SidebarButton btnResidents;
    private SidebarButton btnContracts;
    private SidebarButton btnServices;
    private SidebarButton btnInvoices;
    private SidebarButton btnReports;
    private SidebarButton btnUsers;
    private SidebarButton btnMyStaff;

    private SidebarButton currentActiveButton = null;

    // Colors
    private final Color SIDEBAR_TOP = new Color(56, 189, 248);
    private final Color SIDEBAR_BOTTOM = new Color(2, 132, 199);
    private final Color TEXT_IDLE = new Color(224, 242, 254);
    private final Color TEXT_ACTIVE = Color.WHITE;
    private final Color BTN_HOVER_BG = new Color(255, 255, 255, 40);
    private final Color BTN_ACTIVE_BG = new Color(255, 255, 255, 60);
    private final Color ACCENT_BAR = Color.WHITE;

    public MainDashboard() {
        this.permissionManager = PermissionManager.getInstance();
        initializeFrame();
        createSidebar();
        createContentArea();

        // Mặc định chọn Dashboard
        showDashboardPanel();
    }

    private void initializeFrame() {
        setTitle("Hệ Thống Quản Lý Chung Cư");
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);
    }

    private void createSidebar() {
        sidebarPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, SIDEBAR_TOP, 0, getHeight(), SIDEBAR_BOTTOM);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        sidebarPanel.setPreferredSize(new Dimension(280, getHeight()));

        // --- TOP: LOGO ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(30, 20, 20, 20));

        JLabel iconLabel = new JLabel(new HeaderIcon(48, Color.WHITE));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("QUẢN LÝ CHUNG CƯ");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(10, 0, 15, 0));

        topPanel.add(iconLabel);
        topPanel.add(titleLabel);

        // (Đã xóa Building Selector ở đây)
        sidebarPanel.add(topPanel, BorderLayout.NORTH);

        // --- CENTER: MENU ---
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 15, 4, 15);
        gbc.weightx = 1.0;

        btnDashboard = createMenuButton("Dashboard", "\u2637");
        btnBuildings = createMenuButton("Tòa Nhà", "\u25A3");
        btnResidents = createMenuButton("Cư Dân", "\u265B");
        btnContracts = createMenuButton("Hợp Đồng", "\u2709");
        btnServices = createMenuButton("Dịch Vụ", "\u26A1");
        btnInvoices = createMenuButton("Hóa Đơn", "\u263C");
        btnReports = createMenuButton("Báo Cáo", "\u2630");
        btnUsers = createMenuButton("Tài Khoản", "\u265F");
        btnMyStaff = createMenuButton("Nhân Viên", "👥");

        addMenuButton(menuPanel, btnDashboard, gbc);
        addMenuButton(menuPanel, btnBuildings, gbc);
        addMenuButton(menuPanel, btnResidents, gbc);
        addMenuButton(menuPanel, btnContracts, gbc);
        addMenuButton(menuPanel, btnServices, gbc);
        addMenuButton(menuPanel, btnInvoices, gbc);
        addMenuButton(menuPanel, btnReports, gbc);

        if (permissionManager.isAdmin() || permissionManager.isManager()) {
            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(255, 255, 255, 80));
            sep.setBackground(new Color(255, 255, 255, 80));
            gbc.insets = new Insets(15, 30, 15, 30);
            menuPanel.add(sep, gbc);
            gbc.insets = new Insets(4, 15, 4, 15);
            gbc.gridy++;
        }

        if (permissionManager.isAdmin()) {
            addMenuButton(menuPanel, btnUsers, gbc);
        }
        if (permissionManager.isManager()) {
            addMenuButton(menuPanel, btnMyStaff, gbc);
        }

        gbc.weighty = 1.0;
        menuPanel.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, gbc);

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        sidebarPanel.add(scrollPane, BorderLayout.CENTER);
        sidebarPanel.add(createUserProfile(), BorderLayout.SOUTH);

        add(sidebarPanel, BorderLayout.WEST);
        setupButtonActions();
        applyRoleBasedAccess();
    }

    private void addMenuButton(JPanel panel, SidebarButton btn, GridBagConstraints gbc) {
        panel.add(btn, gbc);
        gbc.gridy++;
    }

    private JPanel createUserProfile() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        String name = (currentUser != null) ? currentUser.getFullName() : "User";
        String role = (currentUser != null) ? currentUser.getRoleDisplayName() : "Role";

        JPanel userPanel = new JPanel(new GridBagLayout());
        userPanel.setBackground(new Color(255, 255, 255, 30));
        userPanel.setBorder(new EmptyBorder(15, 20, 15, 10));
        userPanel.setPreferredSize(new Dimension(280, 85));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 12);
        userPanel.add(new JLabel(new UserIcon(42)), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 0, 0);
        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(Color.WHITE);
        userPanel.add(lblName, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel lblRole = new JLabel(role);
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblRole.setForeground(new Color(224, 242, 254));
        userPanel.add(lblRole, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 5, 0, 0);

        JButton btnSettings = new JButton(new SettingsIcon(24, Color.WHITE));
        btnSettings.setBorderPainted(false);
        btnSettings.setContentAreaFilled(false);
        btnSettings.setFocusPainted(false);
        btnSettings.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSettings.addActionListener(e -> showUserMenu(btnSettings));
        userPanel.add(btnSettings, gbc);

        return userPanel;
    }

    private void setupButtonActions() {
        btnDashboard.addActionListener(e -> showDashboardPanel());
        btnBuildings.addActionListener(e -> showBuildingsPanel());
        btnResidents.addActionListener(e -> showResidentsPanel());
        btnContracts.addActionListener(e -> showContractsPanel());
        btnServices.addActionListener(e -> showServicesPanel());
        btnInvoices.addActionListener(e -> showInvoicesPanel());
        btnReports.addActionListener(e -> showReportsPanel());
        if (btnUsers != null) {
            btnUsers.addActionListener(e -> showUsersPanel());
        }
        if (btnMyStaff != null) {
            btnMyStaff.addActionListener(e -> showMyStaffPanel());
        }
    }

    private SidebarButton createMenuButton(String text, String icon) {
        return new SidebarButton(text, icon);
    }

    private class SidebarButton extends JButton {

        private boolean isSelected = false;
        private boolean isHovered = false;
        private String iconSymbol;

        public SidebarButton(String text, String iconSymbol) {
            super(text);
            this.iconSymbol = iconSymbol;
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(240, 48));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (isSelected) {
                g2.setColor(BTN_ACTIVE_BG);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setColor(ACCENT_BAR);
                g2.fillRoundRect(0, 10, 4, h - 20, 2, 2);
            } else if (isHovered) {
                g2.setColor(BTN_HOVER_BG);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
            }

            Color fg = (isSelected || isHovered) ? TEXT_ACTIVE : TEXT_IDLE;
            g2.setColor(fg);

            Font iconFont = new Font("Segoe UI Symbol", Font.PLAIN, 18);
            Font textFont = getFont();
            FontMetrics fmText = g2.getFontMetrics(textFont);
            FontMetrics fmIcon = g2.getFontMetrics(iconFont);

            int iconW = fmIcon.stringWidth(iconSymbol);
            int textW = fmText.stringWidth(getText());
            int gap = 12;
            int startX = (w - (iconW + gap + textW)) / 2;

            g2.setFont(iconFont);
            g2.drawString(iconSymbol, startX, (h + fmIcon.getAscent() - 4) / 2);
            g2.setFont(textFont);
            g2.drawString(getText(), startX + iconW + gap, (h + fmText.getAscent() - 4) / 2);
            g2.dispose();
        }
    }

    private void setActiveMenuButton(SidebarButton activeButton) {
        SidebarButton[] allButtons = {btnDashboard, btnBuildings, btnResidents, btnContracts, btnServices, btnInvoices, btnReports, btnUsers, btnMyStaff};
        for (SidebarButton btn : allButtons) {
            if (btn != null) {
                btn.setSelected(false);
            }
        }
        if (activeButton != null) {
            activeButton.setSelected(true);
            currentActiveButton = activeButton;
        }
    }

    private void createContentArea() {
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void showPanel(JPanel panel, String title, SidebarButton menuButton) {
        setActiveMenuButton(menuButton);
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showDashboardPanel() {
        // ✅ NEW: Không cần truyền buildingId từ MainDashboard nữa
        showPanel(new DashboardPanel(this), "Dashboard", btnDashboard);
    }

    public void showBuildingsPanel() {
        if (permissionManager.canAccess(PermissionManager.MODULE_BUILDINGS)) {
            showPanel(new BuildingManagementPanel(this::showFloorsOfBuilding), "Quản Lý Tòa Nhà", btnBuildings);
        } else {
            permissionManager.showAccessDeniedMessage(this, "truy cập Tòa Nhà");
        }
    }

    public void showFloorsOfBuilding(Building building) {
        if (permissionManager.canAccess(PermissionManager.MODULE_FLOORS)) {
            FloorManagementPanel floorPanel = new FloorManagementPanel(this::showApartmentsOfFloor);
            floorPanel.setBuilding(building);
            showPanel(floorPanel, "Quản Lý Tầng", btnBuildings);
        } else {
            permissionManager.showAccessDeniedMessage(this, "truy cập Tầng");
        }
    }

    public void showApartmentsOfFloor(Floor floor) {
        if (permissionManager.canAccess(PermissionManager.MODULE_APARTMENTS)) {
            ApartmentManagementPanel aptPanel = new ApartmentManagementPanel();
            aptPanel.setFloor(floor);
            showPanel(aptPanel, "Quản Lý Căn Hộ", btnBuildings);
        } else {
            permissionManager.showAccessDeniedMessage(this, "truy cập Căn Hộ");
        }
    }

    private void showResidentsPanel() {
        if (permissionManager.canAccess(PermissionManager.MODULE_RESIDENTS)) {
            showPanel(new ResidentManagementPanel(), "Quản Lý Cư Dân", btnResidents);
        }
    }

    private void showContractsPanel() {
        if (permissionManager.canAccess(PermissionManager.MODULE_CONTRACTS)) {
            showPanel(new ContractManagementPanel(), "Quản Lý Hợp Đồng", btnContracts);
        }
    }

    private void showServicesPanel() {
        if (permissionManager.canAccess(PermissionManager.MODULE_SERVICES)) {
            showPanel(new ServiceManagementPanel(), "Quản Lý Dịch Vụ", btnServices);
        }
    }

    private void showInvoicesPanel() {
        if (permissionManager.canAccess(PermissionManager.MODULE_INVOICES)) {
            showPanel(new InvoiceManagementPanel(), "Quản Lý Hóa Đơn", btnInvoices);
        }
    }

    private void showReportsPanel() {
        if (permissionManager.canAccess(PermissionManager.MODULE_REPORTS)) {
            showPanel(new ReportPanel(), "Báo Cáo", btnReports);
        }
    }

    private void showUsersPanel() {
        showPanel(new UserManagementPanel(), "Quản Lý Tài Khoản", btnUsers);
    }

    private void showMyStaffPanel() {
        showPanel(new MyStaffPanel(), "Nhân Viên Thuộc Tòa", btnMyStaff);
    }

    private void applyRoleBasedAccess() {
        btnBuildings.setVisible(permissionManager.canAccess(PermissionManager.MODULE_BUILDINGS));
        btnResidents.setVisible(permissionManager.canAccess(PermissionManager.MODULE_RESIDENTS));
        btnContracts.setVisible(permissionManager.canAccess(PermissionManager.MODULE_CONTRACTS));
        btnServices.setVisible(permissionManager.canAccess(PermissionManager.MODULE_SERVICES));
        btnInvoices.setVisible(permissionManager.canAccess(PermissionManager.MODULE_INVOICES));
        btnReports.setVisible(permissionManager.canAccess(PermissionManager.MODULE_REPORTS));
    }

    private void showUserMenu(Component invoker) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        JMenuItem changePass = new JMenuItem("Đổi mật khẩu");
        changePass.setBackground(Color.WHITE);
        changePass.addActionListener(e -> showChangePasswordDialog());
        JMenuItem logout = new JMenuItem("Đăng xuất");
        logout.setBackground(Color.WHITE);
        logout.setForeground(new Color(225, 29, 72));
        logout.addActionListener(e -> performLogout());
        popup.add(changePass);
        popup.addSeparator();
        popup.add(logout);
        popup.show(invoker, -100, -70);
    }

    private void performLogout() {
        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            SessionManager.getInstance().logout();
            new LoginFrame();
            dispose();
        }
    }

    private void showChangePasswordDialog() {
        JOptionPane.showMessageDialog(this, "Chức năng đổi mật khẩu đang được cập nhật.");
    }

    // Icons classes (Giữ nguyên)
    private static class HeaderIcon implements Icon {

        int size;
        Color color;

        public HeaderIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, size, size, 12, 12);
            g2.setColor(new Color(2, 132, 199));
            int p = size / 4;
            g2.fillRect(x + p, y + p, size / 2, size / 2);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x + size / 2, y + p, x + size / 2, y + size - p);
            g2.drawLine(x + p, y + size / 2, x + size - p, y + size / 2);
            g2.dispose();
        }
    }

    private static class UserIcon implements Icon {

        int size;

        public UserIcon(int size) {
            this.size = size;
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillOval(x, y, size, size);
            g2.setColor(Color.WHITE);
            g2.setClip(new Ellipse2D.Float(x, y, size, size));
            g2.fillOval(x + size / 4, y + size / 5, size / 2, size / 2);
            g2.fillOval(x + size / 6, y + size / 2 + size / 6, size * 2 / 3, size / 2);
            g2.dispose();
        }
    }

    private static class SettingsIcon implements Icon {

        int size;
        Color color;

        public SettingsIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int dotSize = 4;
            int centerX = x + size / 2 - dotSize / 2;
            int startY = y + 4;
            int gap = 6;
            g2.fillOval(centerX, startY, dotSize, dotSize);
            g2.fillOval(centerX, startY + gap, dotSize, dotSize);
            g2.fillOval(centerX, startY + gap * 2, dotSize, dotSize);
            g2.dispose();
        }
    }

    @Override
    public void goToBuildings() {
        showBuildingsPanel();
    }

    @Override
    public void goToApartments() {
        showBuildingsPanel();
    }

    @Override
    public void goToResidents() {
        showResidentsPanel();
    }

    @Override
    public void goToContracts() {
        showContractsPanel();
    }

    @Override
    public void goToInvoices() {
        showInvoicesPanel();
    }

    @Override
    public void goToReports() {
        showReportsPanel();
    }

    @Override
    public void goToServices() {
        showServicesPanel();
    }
}
