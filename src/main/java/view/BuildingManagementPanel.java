package view;

import dao.BuildingDAO;
import dao.BuildingDAO.BuildingStats;
import model.Building;
import util.BuildingContext;
import util.PermissionManager;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class BuildingManagementPanel extends JPanel {

    private final BuildingDAO buildingDAO = new BuildingDAO();
    private final PermissionManager permissionManager = PermissionManager.getInstance();

    private JPanel cardsContainer;
    private JButton btnAdd;

    private Consumer<Building> onBuildingSelect;

    public BuildingManagementPanel(Consumer<Building> onBuildingSelect) {
        this.onBuildingSelect = building -> {
            BuildingContext.getInstance().setCurrentBuilding(building);
            if (onBuildingSelect != null) {
                onBuildingSelect.accept(building);
            }
        };
        initUI();
        loadBuildings();
    }

    // =====================================================
    // UI
    // =====================================================
    private void initUI() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // ---------- HEADER ----------
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JLabel titleLabel = new JLabel("Quản Lý Tòa Nhà");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24)); // Font tiêu đề
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        btnAdd = new RoundedButton("Thêm Tòa Nhà", 10);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setBackground(UIConstants.PRIMARY_COLOR);
        btnAdd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAdd.setPreferredSize(new Dimension(160, 40));
        btnAdd.addActionListener(e -> showAddDialog());

        // RBAC: chỉ ADMIN mới được thấy nút Thêm
        btnAdd.setVisible(permissionManager.isAdmin());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(btnAdd, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // ---------- CONTENT ----------
        cardsContainer = new JPanel(new GridLayout(0, 2, 25, 25)); // Grid 2 cột
        cardsContainer.setBackground(UIConstants.BACKGROUND_COLOR);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UIConstants.BACKGROUND_COLOR);
        wrapper.add(cardsContainer, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(UIConstants.BACKGROUND_COLOR);

        add(scrollPane, BorderLayout.CENTER);
    }

    // =====================================================
    // LOAD DATA (ĐÃ FIX: Hiển thị toàn bộ danh sách được phân quyền)
    // =====================================================
    private void loadBuildings() {
        cardsContainer.removeAll();

        // ✅ FIX: Lấy trực tiếp danh sách từ DAO
        // BuildingDAO đã tự động lọc theo bảng user_buildings (Manager thấy list tòa mình quản lý)
        // Admin thấy toàn bộ.
        List<Building> displayBuildings = buildingDAO.getAllBuildings();

        // Không còn đoạn code tự filter bằng getBuildingFilter() ở đây nữa

        if (displayBuildings.isEmpty()) {
            cardsContainer.setLayout(new BorderLayout());

            String message = permissionManager.isAdmin()
                    ? "Chưa có tòa nhà nào.<br>Nhấn <b>Thêm Tòa Nhà</b> để bắt đầu."
                    : "Bạn chưa được phân công vào tòa nhà nào.<br>Vui lòng liên hệ Admin.";

            JLabel emptyLabel = new JLabel(
                    "<html><center>" + message + "</center></html>",
                    SwingConstants.CENTER
            );
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            emptyLabel.setForeground(Color.GRAY);
            cardsContainer.add(emptyLabel, BorderLayout.CENTER);
        } else {
            cardsContainer.setLayout(new GridLayout(0, 2, 25, 25)); // Reset layout
            for (Building b : displayBuildings) {
                BuildingStats stats = buildingDAO.getBuildingStatistics(b.getId());

                // Tạo Card hiển thị thông tin
                // Chỉ truyền callback edit/delete nếu là Admin
                cardsContainer.add(new BuildingCard(
                        b,
                        stats,
                        onBuildingSelect,
                        permissionManager.isAdmin() ? this::editBuilding : null,
                        permissionManager.isAdmin() ? this::deleteBuilding : null
                ));
            }
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    // =====================================================
    // CRUD OPERATIONS
    // =====================================================
private void showAddDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        BuildingDialog dialog = new BuildingDialog(parent, new Building());
        dialog.setVisible(true);

        // Dialog đã tự xử lý insert/update bên trong rồi
        // Chỉ cần reload lại danh sách nếu confirmed
        if (dialog.isConfirmed()) {
            JOptionPane.showMessageDialog(this, "Thêm tòa nhà thành công!");
            loadBuildings();
        }
    }

    private void editBuilding(Building building) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        BuildingDialog dialog = new BuildingDialog(parent, building);
        dialog.setVisible(true);

        // Dialog đã tự xử lý insert/update bên trong rồi
        // Chỉ cần reload lại danh sách nếu confirmed
        if (dialog.isConfirmed()) {
            JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
            loadBuildings();
        }
    }

    private void deleteBuilding(Building building) {
        // Chặn nếu có hợp đồng ACTIVE
        if (buildingDAO.hasActiveContracts(building.getId())) {
            JOptionPane.showMessageDialog(this,
                    "KHÔNG THỂ XÓA TÒA NHÀ!\n\n"
                    + "Tòa nhà đang có hợp đồng thuê đang hiệu lực.\n"
                    + "Vui lòng kết thúc hợp đồng trước.",
                    "Bị chặn bởi ràng buộc dữ liệu",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa tòa nhà \"" + building.getName() + "\"?\n"
                + "Dữ liệu lịch sử vẫn được giữ lại.",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (buildingDAO.deleteBuilding(building.getId())) {
                JOptionPane.showMessageDialog(this, "Đã xóa tòa nhà!");
                loadBuildings();
            } else {
                JOptionPane.showMessageDialog(this, "Xóa thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =====================================================
    // CUSTOM UI COMPONENT
    // =====================================================
    private static class RoundedButton extends JButton {

        private final int arc;

        public RoundedButton(String text, int arc) {
            super(text);
            this.arc = arc;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
            g2.dispose();
        }
    }
}