package view;

import dao.BuildingDAO;
import dao.FloorDAO;
import model.Building;
import model.Floor;
import util.PermissionManager;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.Consumer;

public class FloorManagementPanel extends JPanel {

    private FloorDAO floorDAO;
    private BuildingDAO buildingDAO;
    private JPanel cardsContainer;
    private Building currentBuilding;

    private JComboBox<Building> cbbBuilding;
    private JButton btnBatchAdd;
    private JButton btnAdd;
    private Consumer<Floor> onFloorSelect;

    private PermissionManager permissionManager;
    private SwingWorker<?, ?> currentWorker = null;

    public FloorManagementPanel() {
        this(null, null);
    }

    public FloorManagementPanel(Consumer<Floor> onFloorSelect) {
        this(null, onFloorSelect);
    }

    public FloorManagementPanel(Building building, Consumer<Floor> onFloorSelect) {
        this.floorDAO = new FloorDAO();
        this.buildingDAO = new BuildingDAO();
        this.permissionManager = PermissionManager.getInstance();
        this.currentBuilding = building;
        this.onFloorSelect = onFloorSelect;

        initUI();
        loadBuildingData();
    }

    public void setBuilding(Building building) {
        this.currentBuilding = building;
        if (building != null) {
            for (int i = 0; i < cbbBuilding.getItemCount(); i++) {
                Building b = cbbBuilding.getItemAt(i);
                if (b.getId() != null && b.getId().equals(building.getId())) {
                    cbbBuilding.setSelectedIndex(i);
                    break;
                }
            }
        }
        loadFloors();
    }

    private void initUI() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // === HEADER ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        // LEFT SIDE
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftHeader.setBackground(UIConstants.BACKGROUND_COLOR);

        JButton btnBack = createBackArrowButton();
        btnBack.addActionListener(e -> {
            MainDashboard main = (MainDashboard) SwingUtilities.getWindowAncestor(this);
            if (main != null) {
                main.showBuildingsPanel();
            }
        });
        leftHeader.add(btnBack);

        JLabel lblFilter = new JLabel("Đang xem:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblFilter.setForeground(Color.GRAY);

        cbbBuilding = new JComboBox<>();
        cbbBuilding.setPreferredSize(new Dimension(250, 40));
        cbbBuilding.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cbbBuilding.setBackground(Color.WHITE);

        cbbBuilding.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Building) {
                    setText(((Building) value).getName());
                }
                setBorder(new EmptyBorder(5, 5, 5, 5));
                return this;
            }
        });

        // Data Isolation
        if (!permissionManager.isAdmin()) {
            cbbBuilding.setEnabled(false);
        }

        cbbBuilding.addActionListener(e -> {
            Building selected = (Building) cbbBuilding.getSelectedItem();
            if (selected != null && selected.getId() != null) {
                this.currentBuilding = selected;
            } else {
                this.currentBuilding = null;
            }
            loadFloors();
        });
        leftHeader.add(lblFilter);
        leftHeader.add(cbbBuilding);

        // RIGHT SIDE
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setBackground(UIConstants.BACKGROUND_COLOR);

        // Check Permissions
        boolean canAdd = permissionManager.canAdd(PermissionManager.MODULE_FLOORS);

        btnBatchAdd = new RoundedButton(" Thêm Hàng Loạt", 15);
        btnBatchAdd.setIcon(new HeaderIcon("LAYER_PLUS", 14, Color.WHITE));
        btnBatchAdd.setPreferredSize(new Dimension(160, 40));
        btnBatchAdd.setBackground(new Color(0, 150, 136));
        btnBatchAdd.setForeground(Color.WHITE);
        btnBatchAdd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBatchAdd.addActionListener(e -> showBatchAddDialog());
        btnBatchAdd.setVisible(canAdd);

        btnAdd = new RoundedButton(" Thêm Tầng Mới", 15);
        btnAdd.setIcon(new HeaderIcon("PLUS", 14, Color.WHITE));
        btnAdd.setPreferredSize(new Dimension(160, 40));
        btnAdd.setBackground(UIConstants.PRIMARY_COLOR);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAdd.addActionListener(e -> showAddDialog());
        btnAdd.setVisible(canAdd);

        rightHeader.add(btnBatchAdd);
        rightHeader.add(btnAdd);

        headerPanel.add(leftHeader, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // === CONTENT ===
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        cardsContainer = new JPanel(new GridLayout(0, 3, 20, 20));
        cardsContainer.setBackground(UIConstants.BACKGROUND_COLOR);
        cardsContainer.setBorder(new EmptyBorder(10, 0, 10, 0));
        wrapperPanel.add(cardsContainer, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UIConstants.BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(UIConstants.BACKGROUND_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JButton createBackArrowButton() {
        JButton btn = new JButton("Quay lại");
        btn.setIcon(new HeaderIcon("BACK_ARROW", 16, UIConstants.PRIMARY_COLOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(UIConstants.PRIMARY_COLOR);
        btn.setBackground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(5, 10, 5, 15));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(new Color(235, 245, 255));
            }

            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
            }
        });
        return btn;
    }

    private void loadBuildingData() {
        List<Building> buildings = buildingDAO.getAllBuildings();
        cbbBuilding.removeAllItems();

        Long filterId = permissionManager.getBuildingFilter();

        if (buildings.isEmpty()) {
            cbbBuilding.addItem(new Building(null, "Chưa có tòa nhà nào", "", "", "", "Đang hoạt động", false));
        } else {
            for (Building b : buildings) {
                if (filterId == null || b.getId().equals(filterId)) {
                    cbbBuilding.addItem(b);
                }
            }
            if (currentBuilding != null) {
                for (int i = 0; i < cbbBuilding.getItemCount(); i++) {
                    Building b = cbbBuilding.getItemAt(i);
                    if (b.getId() != null && b.getId().equals(currentBuilding.getId())) {
                        cbbBuilding.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (cbbBuilding.getItemCount() > 0) {
                cbbBuilding.setSelectedIndex(0);
            }
        }
    }

    public void loadFloors() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        cardsContainer.removeAll();

        if (currentBuilding == null || currentBuilding.getId() == null) {
            showEmptyMessage("Vui lòng chọn một tòa nhà.", false);
            setButtonsEnabled(false);
            cardsContainer.revalidate();
            cardsContainer.repaint();
            return;
        }

        boolean isBuildingMaintenance = "Đang bảo trì".equals(currentBuilding.getStatus())
                || "MAINTENANCE".equalsIgnoreCase(currentBuilding.getStatus());

        boolean canAdd = permissionManager.canAdd(PermissionManager.MODULE_FLOORS);
        setButtonsEnabled(!isBuildingMaintenance && canAdd);

        boolean canEdit = permissionManager.canEdit(PermissionManager.MODULE_FLOORS);
        boolean canDelete = permissionManager.canDelete(PermissionManager.MODULE_FLOORS);

        SwingWorker<List<dao.FloorDAO.FloorWithStats>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<dao.FloorDAO.FloorWithStats> doInBackground() {
                if (isCancelled()) {
                    return null;
                }
                return floorDAO.getFloorsWithStatsByBuildingId(currentBuilding.getId());
            }

            @Override
            protected void done() {
                if (this != FloorManagementPanel.this.currentWorker) {
                    return;
                }
                if (isCancelled()) {
                    return;
                }

                try {
                    List<dao.FloorDAO.FloorWithStats> data = get();
                    cardsContainer.removeAll();

                    if (data.isEmpty()) {
                        showEmptyMessage("Tòa nhà này chưa có tầng nào.", false);
                    } else {
                        for (dao.FloorDAO.FloorWithStats item : data) {
                            FloorCard card = new FloorCard(
                                    item.floor,
                                    item.stats,
                                    isBuildingMaintenance,
                                    onFloorSelect,
                                    canEdit ? FloorManagementPanel.this::editFloor : null,
                                    canDelete ? FloorManagementPanel.this::deleteFloor : null
                            );
                            cardsContainer.add(card);
                        }
                    }
                    cardsContainer.revalidate();
                    cardsContainer.repaint();

                } catch (Exception e) {
                    if (this != FloorManagementPanel.this.currentWorker) {
                        return;
                    }
                    e.printStackTrace();
                    cardsContainer.removeAll();
                    showEmptyMessage("Lỗi khi tải dữ liệu tầng.", false);
                    cardsContainer.revalidate();
                    cardsContainer.repaint();
                }
            }
        };

        currentWorker = worker;
        worker.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        if (!permissionManager.canAdd(PermissionManager.MODULE_FLOORS)) {
            btnAdd.setVisible(false);
            btnBatchAdd.setVisible(false);
            return;
        }

        btnAdd.setEnabled(enabled);
        btnBatchAdd.setEnabled(enabled);

        if (enabled) {
            btnAdd.setBackground(UIConstants.PRIMARY_COLOR);
            btnBatchAdd.setBackground(new Color(0, 150, 136));
        } else {
            btnAdd.setBackground(Color.GRAY);
            btnBatchAdd.setBackground(Color.GRAY);
        }
    }

    private void showEmptyMessage(String msg, boolean isWarning) {
        JPanel msgPanel = new JPanel();
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
        msgPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        if (isWarning) {
            JLabel iconLabel = new JLabel(new HeaderIcon("MAINTENANCE_ART", 80, new Color(255, 87, 34)));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            msgPanel.add(Box.createVerticalStrut(50));
            msgPanel.add(iconLabel);
            msgPanel.add(Box.createVerticalStrut(20));
        }

        JLabel guideLabel = new JLabel("<html><center>" + msg.replace("\n", "<br>") + "</center></html>");
        guideLabel.setHorizontalAlignment(SwingConstants.CENTER);
        guideLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (isWarning) {
            guideLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            guideLabel.setForeground(new Color(211, 47, 47));
        } else {
            guideLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            guideLabel.setForeground(Color.GRAY);
        }

        msgPanel.add(guideLabel);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(UIConstants.BACKGROUND_COLOR);
        centerWrapper.add(msgPanel);

        cardsContainer.add(centerWrapper);
    }

    private void showBatchAddDialog() {
        if (currentBuilding == null || currentBuilding.getId() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Tòa nhà trước!");
            return;
        }
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        BatchAddFloorDialog dialog = new BatchAddFloorDialog(parent, currentBuilding.getId());
        dialog.setVisible(true);
        if (dialog.isSuccess()) {
            loadFloors();
        }
    }

    private void showAddDialog() {
        if (currentBuilding == null || currentBuilding.getId() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Tòa nhà trước!");
            return;
        }
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        Floor newFloor = new Floor();

        newFloor.setBuildingId(currentBuilding.getId());

        FloorDialog dialog = new FloorDialog(parent, newFloor);

        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Floor f = dialog.getFloor();

            // --- SỬA LỖI: THÊM VALIDATION ---
            if (floorDAO.isFloorNumberExists(f.getBuildingId(), f.getFloorNumber())) {
                JOptionPane.showMessageDialog(this,
                        "Tầng số " + f.getFloorNumber() + " đã tồn tại trong tòa nhà này!",
                        "Lỗi trùng lặp",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (floorDAO.insertFloor(f)) {
                JOptionPane.showMessageDialog(this, "Thêm tầng thành công!");
                loadFloors();
            } else {
                JOptionPane.showMessageDialog(this, "Thêm thất bại!");
            }
        }
    }

    private void editFloor(Floor floor) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        FloorDialog dialog = new FloorDialog(parent, floor);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Floor updated = dialog.getFloor();

            // --- SỬA LỖI: THÊM VALIDATION KHI SỬA ---
            // excludeId = floor.getId() để không báo trùng với chính nó
            if (floorDAO.isFloorNumberExists(updated.getBuildingId(), updated.getFloorNumber(), floor.getId())) {
                JOptionPane.showMessageDialog(this,
                        "Tầng số " + updated.getFloorNumber() + " đã tồn tại!",
                        "Lỗi trùng lặp",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (floorDAO.updateFloor(updated)) {
                JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
                loadFloors();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại!");
            }
        }
    }

    private void deleteFloor(Floor floor) {
        if (!floorDAO.canDeleteFloor(floor.getId())) {
            JOptionPane.showMessageDialog(this,
                    "KHÔNG THỂ XÓA TẦNG NÀY!\n\n"
                    + "Điều kiện để xóa tầng:\n"
                    + "1. Tầng phải TRỐNG (Không còn căn hộ nào, hãy xóa hết căn hộ trước).\n"
                    + "2. Không còn hợp đồng thuê active liên quan.\n\n"
                    + "Vui lòng vào chi tiết tầng và xử lý dữ liệu con trước.",
                    "Thao tác bị chặn",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa \"" + floor.getName() + "\"?\n"
                + "Dữ liệu sẽ bị xóa mềm (ẩn đi).",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (floorDAO.deleteFloor(floor.getId())) {
                JOptionPane.showMessageDialog(this, "Đã xóa tầng thành công!");
                loadFloors();
            } else {
                JOptionPane.showMessageDialog(this, "Xóa thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class RoundedButton extends JButton {

        private int arc;

        public RoundedButton(String text, int arc) {
            super(text);
            this.arc = arc;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
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

    private static class HeaderIcon implements Icon {

        private String type;
        private int size;
        private Color color;

        public HeaderIcon(String type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f));
            g2.translate(x, y);

            if ("PLUS".equals(type)) {
                g2.drawLine(0, size / 2, size, size / 2);
                g2.drawLine(size / 2, 0, size / 2, size);
            } else if ("LAYER_PLUS".equals(type)) {
                int w = size - 8;
                int h = size / 4;
                g2.drawRoundRect(4, size / 2 - 4, w, h, 3, 3);
                g2.drawRoundRect(4, size / 2 + 4, w, h, 3, 3);
                g2.drawLine(size / 2, 2, size / 2, 10);
                g2.drawLine(size / 2 - 4, 6, size / 2 + 4, 6);
            } else if ("BACK_ARROW".equals(type)) {
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Float();
                p.moveTo(size * 0.7, size * 0.2);
                p.lineTo(size * 0.3, size * 0.5);
                p.lineTo(size * 0.7, size * 0.8);
                g2.draw(p);
            } else if ("MAINTENANCE_ART".equals(type)) {
                int cx = size / 2;
                int cy = size / 2;
                int r = size / 3;
                g2.setStroke(new BasicStroke(3.0f));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                g2.setStroke(new BasicStroke(4.0f));
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    int x1 = cx + (int) (Math.cos(angle) * (r - 2));
                    int y1 = cy + (int) (Math.sin(angle) * (r - 2));
                    int x2 = cx + (int) (Math.cos(angle) * (r + 6));
                    int y2 = cy + (int) (Math.sin(angle) * (r + 6));
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.setColor(new Color(255, 87, 34));
                g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(size / 4, size - size / 4, size - size / 4, size / 4);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
