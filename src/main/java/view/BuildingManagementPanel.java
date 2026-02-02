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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    // LOAD DATA (QUAN TRỌNG: Đã thêm logic lọc)
    // =====================================================
    private void loadBuildings() {
        cardsContainer.removeAll();

        // 1. Lấy tất cả tòa nhà từ DB
        List<Building> allBuildings = buildingDAO.getAllBuildings();
        List<Building> displayBuildings;

        // 2. Lấy ID tòa nhà được phép xem của User hiện tại
        Long userBuildingId = permissionManager.getBuildingFilter();

        // 3. Lọc danh sách
        if (userBuildingId == null) {
            // Nếu là NULL (Admin) -> Xem hết
            displayBuildings = allBuildings;
        } else {
            // Nếu có ID (Manager/Staff) -> Chỉ lấy tòa nhà trùng ID
            displayBuildings = allBuildings.stream()
                    .filter(b -> b.getId().equals(userBuildingId))
                    .collect(Collectors.toList());
        }

        // 4. Hiển thị ra giao diện
        if (displayBuildings.isEmpty()) {
            cardsContainer.setLayout(new BorderLayout());

            String message = (userBuildingId == null)
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

        if (dialog.isConfirmed()) {
            if (buildingDAO.insertBuilding(dialog.getBuilding())) {
                JOptionPane.showMessageDialog(this, "Thêm tòa nhà thành công!");
                loadBuildings();
            } else {
                JOptionPane.showMessageDialog(this, "Thêm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editBuilding(Building building) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        BuildingDialog dialog = new BuildingDialog(parent, building);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            if (buildingDAO.updateBuilding(dialog.getBuilding())) {
                JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
                loadBuildings();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
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
