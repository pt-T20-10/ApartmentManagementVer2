package view;

import dao.ContractHouseholdViewDAO;
import dao.FloorDAO;
import dao.ApartmentDAO;
import dao.BuildingDAO;

import model.ContractHouseholdViewModel;
import model.Building;
import model.Floor;
import model.Apartment;
import model.Resident;
import util.BuildingContext;
import util.PermissionManager;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resident Management Panel Fixed: Filter logic and Initial Load
 */
public class ResidentManagementPanel extends JPanel
        implements BuildingContext.ContextChangeListener {

    private ContractHouseholdViewDAO contractHouseholdDAO;
    private FloorDAO floorDAO;
    private ApartmentDAO apartmentDAO;
    private BuildingDAO buildingDAO;
    private PermissionManager permissionManager;

    private BuildingContext buildingContext;

    private JLabel contextLabel;
    private JLabel countLabel;
    private JTable contractTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    // Filter components
    private JComboBox<BuildingDisplay> buildingFilterCombo;
    private JComboBox<String> floorFilterCombo;
    private JComboBox<String> apartmentFilterCombo;

    private JPanel noContextPanel;
    private JPanel contentPanel;

    // Status radio buttons
    private JRadioButton rbShowLiving;
    private JRadioButton rbShowMoved;
    private JRadioButton rbShowAll;
    private ButtonGroup statusButtonGroup;

    // Cache data
    private List<ContractHouseholdViewModel> allContracts;
    private List<Floor> floors;
    private List<Apartment> apartments;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private boolean isUpdatingCombos = false;

    public ResidentManagementPanel() {
        this.contractHouseholdDAO = new ContractHouseholdViewDAO();
        this.floorDAO = new FloorDAO();
        this.apartmentDAO = new ApartmentDAO();
        this.buildingDAO = new BuildingDAO();
        this.permissionManager = PermissionManager.getInstance();

        this.buildingContext = BuildingContext.getInstance();

        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(new EmptyBorder(30, 30, 30, 30));

        buildingContext.addContextChangeListener(this);

        createUI();
        checkContextAndLoad();
    }

    private void createUI() {
        createNoContextPanel();
        createContentPanel();
        checkContextAndLoad();
    }

    private void createNoContextPanel() {
        noContextPanel = new JPanel(new GridBagLayout());
        noContextPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JPanel messageCard = new JPanel();
        messageCard.setLayout(new BoxLayout(messageCard, BoxLayout.Y_AXIS));
        messageCard.setBackground(Color.WHITE);
        messageCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 193, 7), 3, true),
                new EmptyBorder(50, 70, 50, 70)
        ));

        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 193, 7),
                        getWidth(), getHeight(), new Color(255, 152, 0));
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 60, 60);
            }
        };
        iconPanel.setPreferredSize(new Dimension(80, 80));
        iconPanel.setMaximumSize(new Dimension(80, 80));
        iconPanel.setOpaque(false);
        iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLabel = new JLabel("🏢", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        iconLabel.setForeground(Color.WHITE);
        iconPanel.setLayout(new GridBagLayout());
        iconPanel.add(iconLabel);

        JLabel titleLabel = new JLabel("Chưa chọn tòa nhà", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel = new JLabel(
                "<html><center><span style='color: #666; font-size: 14px;'>"
                + "Vui lòng vào Tab <b style='color: #1976d2;'>Tòa Nhà</b><br>"
                + "và chọn một tòa nhà để xem danh sách cư dân</span></center></html>",
                SwingConstants.CENTER
        );
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton goBtn = new JButton("→ Đi đến Tab Tòa Nhà") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color color1, color2;
                if (getModel().isPressed()) {
                    color1 = new Color(25, 118, 210);
                    color2 = new Color(25, 118, 210);
                } else if (getModel().isRollover()) {
                    color1 = new Color(33, 150, 243);
                    color2 = new Color(30, 136, 229);
                } else {
                    color1 = new Color(25, 118, 210);
                    color2 = new Color(21, 101, 192);
                }

                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        goBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        goBtn.setForeground(Color.WHITE);
        goBtn.setFocusPainted(false);
        goBtn.setBorderPainted(false);
        goBtn.setContentAreaFilled(false);
        goBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        goBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        goBtn.setPreferredSize(new Dimension(240, 50));
        goBtn.setMaximumSize(new Dimension(240, 50));
        goBtn.addActionListener(e -> goToBuildingTab());

        messageCard.add(iconPanel);
        messageCard.add(Box.createVerticalStrut(25));
        messageCard.add(titleLabel);
        messageCard.add(Box.createVerticalStrut(15));
        messageCard.add(messageLabel);
        messageCard.add(Box.createVerticalStrut(35));
        messageCard.add(goBtn);

        noContextPanel.add(messageCard);
    }

    private void createContentPanel() {
        contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        contentPanel.add(createModernHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        centerPanel.add(createFilterBar(), BorderLayout.NORTH);
        centerPanel.add(createModernTable(), BorderLayout.CENTER);

        contentPanel.add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createModernHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(25, 30, 25, 30)
        ));

        // ROW 1
        JPanel row1 = new JPanel(new BorderLayout(20, 0));
        row1.setBackground(Color.WHITE);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleRow.setBackground(Color.WHITE);

        JLabel iconLabel = new JLabel("👥");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));

        JLabel titleLabel = new JLabel("Quản Lý Cư Dân");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(new Color(33, 33, 33));

        titleRow.add(iconLabel);
        titleRow.add(titleLabel);

        // Context label
        JPanel contextRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contextRow.setBackground(Color.WHITE);

        contextLabel = new JLabel();
        contextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contextLabel.setForeground(new Color(117, 117, 117));

        contextRow.add(contextLabel);

        leftPanel.add(titleRow);
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(contextRow);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchPanel.setBackground(Color.WHITE);

        searchField = new JTextField(22);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));

        final String PLACEHOLDER = "Tìm theo tên, SĐT, căn hộ...";
        final Color PLACEHOLDER_COLOR = new Color(158, 158, 158);
        final Color TEXT_COLOR = new Color(33, 33, 33);

        searchField.setText(PLACEHOLDER);
        searchField.setForeground(PLACEHOLDER_COLOR);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (searchField.getText().equals(PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_COLOR);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText(PLACEHOLDER);
                    searchField.setForeground(PLACEHOLDER_COLOR);
                }
            }
        });

        searchField.addActionListener(e -> applyFilters());

        JButton searchBtn = createModernButton("Tìm", new Color(33, 150, 243));
        searchBtn.setPreferredSize(new Dimension(100, 42));
        searchBtn.addActionListener(e -> applyFilters());

        JButton refreshBtn = createModernButton("Làm mới", new Color(76, 175, 80));
        refreshBtn.setPreferredSize(new Dimension(130, 42));
        refreshBtn.addActionListener(e -> resetFilters());

        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(refreshBtn);

        row1.add(leftPanel, BorderLayout.WEST);
        row1.add(searchPanel, BorderLayout.EAST);

        // ROW 2
        JPanel row2 = new JPanel(new BorderLayout(20, 0));
        row2.setBackground(Color.WHITE);

        countLabel = new JLabel();
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        countLabel.setForeground(UIConstants.PRIMARY_COLOR);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(Color.WHITE);

        JButton statsBtn = createModernButton("Thống kê", new Color(103, 58, 181));
        statsBtn.setPreferredSize(new Dimension(140, 42));
        statsBtn.addActionListener(e -> showStatistics());

        JButton exportBtn = createModernButton("Xuất Excel", new Color(67, 160, 71));
        exportBtn.setPreferredSize(new Dimension(140, 42));
        exportBtn.addActionListener(e -> exportToExcel());

        actionPanel.add(statsBtn);
        actionPanel.add(exportBtn);

        row2.add(countLabel, BorderLayout.WEST);
        row2.add(actionPanel, BorderLayout.EAST);

        headerPanel.add(row1);
        headerPanel.add(Box.createVerticalStrut(15));
        headerPanel.add(row2);

        return headerPanel;
    }

    private JPanel createFilterBar() {
        JPanel mainFilterPanel = new JPanel();
        mainFilterPanel.setLayout(new BoxLayout(mainFilterPanel, BoxLayout.Y_AXIS));
        mainFilterPanel.setBackground(Color.WHITE);
        mainFilterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(18, 25, 18, 25)
        ));

        // ROW 1: Dropdown filters
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.setBackground(Color.WHITE);

        JLabel filterLabel = new JLabel("Bộ lọc:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        filterLabel.setForeground(new Color(66, 66, 66));

        // Building filter
        JLabel buildingLabel = new JLabel("Tòa nhà:");
        buildingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buildingFilterCombo = new JComboBox<>();
        buildingFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buildingFilterCombo.setBackground(Color.WHITE);
        buildingFilterCombo.setPreferredSize(new Dimension(180, 38));
        buildingFilterCombo.addActionListener(e -> {
            if (!isUpdatingCombos) {
                onBuildingFilterChanged();
            }
        });

        // Floor filter
        JLabel floorLabel = new JLabel("Tầng:");
        floorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        floorFilterCombo = createFilterCombo();
        floorFilterCombo.setPreferredSize(new Dimension(130, 38));
        floorFilterCombo.addActionListener(e -> {
            if (!isUpdatingCombos) {
                onFloorFilterChanged();
            }
        });

        // Apartment filter
        JLabel apartmentLabel = new JLabel("Căn hộ:");
        apartmentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        apartmentFilterCombo = createFilterCombo();
        apartmentFilterCombo.setPreferredSize(new Dimension(120, 38));
        apartmentFilterCombo.addActionListener(e -> {
            if (!isUpdatingCombos) {
                applyFilters();
            }
        });

        row1.add(filterLabel);
        row1.add(Box.createHorizontalStrut(10));
        row1.add(buildingLabel);
        row1.add(buildingFilterCombo);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(floorLabel);
        row1.add(floorFilterCombo);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(apartmentLabel);
        row1.add(apartmentFilterCombo);

        // ROW 2: Status radio buttons
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row2.setBackground(Color.WHITE);

        JLabel statusLabel = new JLabel("Hiển thị:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(66, 66, 66));

        statusButtonGroup = new ButtonGroup();
        rbShowLiving = createStatusRadioButton("Đang ở", new Color(46, 125, 50));
        rbShowMoved = createStatusRadioButton("Đã chuyển đi", new Color(158, 158, 158));
        rbShowAll = createStatusRadioButton("Tất cả", new Color(33, 150, 243));

        statusButtonGroup.add(rbShowLiving);
        statusButtonGroup.add(rbShowMoved);
        statusButtonGroup.add(rbShowAll);

        rbShowLiving.setSelected(true);

        row2.add(statusLabel);
        row2.add(Box.createHorizontalStrut(15));
        row2.add(rbShowLiving);
        row2.add(Box.createHorizontalStrut(15));
        row2.add(rbShowMoved);
        row2.add(Box.createHorizontalStrut(15));
        row2.add(rbShowAll);

        mainFilterPanel.add(row1);
        mainFilterPanel.add(Box.createVerticalStrut(12));
        mainFilterPanel.add(row2);

        return mainFilterPanel;
    }

    private JRadioButton createStatusRadioButton(String text, Color color) {
        JRadioButton radioButton = new JRadioButton(text);
        radioButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        radioButton.setForeground(color);
        radioButton.setBackground(Color.WHITE);
        radioButton.setFocusPainted(false);
        radioButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        radioButton.addActionListener(e -> applyFilters());
        return radioButton;
    }

    private JComboBox<String> createFilterCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.addItem("Tất cả");
        return combo;
    }

    private JPanel createModernTable() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true));

        String[] columns = {"Căn hộ", "Tầng", "Chủ hộ", "SĐT", "CCCD", "Tổng số người", "Trạng thái", "Thao tác"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };

        contractTable = new JTable(tableModel);
        contractTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contractTable.setRowHeight(50);
        contractTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contractTable.setShowGrid(false);
        contractTable.setIntercellSpacing(new Dimension(0, 0));
        contractTable.setSelectionBackground(new Color(232, 245, 253));
        contractTable.setSelectionForeground(new Color(33, 33, 33));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < contractTable.getColumnCount() - 1; i++) {
            contractTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        contractTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                String status = (String) value;
                JLabel label = (JLabel) c;
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));

                if ("Đang ở".equals(status)) {
                    if (!isSelected) {
                        label.setBackground(new Color(232, 245, 233));
                        label.setForeground(new Color(46, 125, 50));
                    }
                    label.setText("● Đang ở");
                } else {
                    if (!isSelected) {
                        label.setBackground(new Color(250, 250, 250));
                        label.setForeground(new Color(158, 158, 158));
                    }
                    label.setText("○ Đã chuyển");
                }

                return label;
            }
        });

        contractTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        contractTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox()));

        JTableHeader header = contractTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(250, 250, 250));
        header.setForeground(new Color(66, 66, 66));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)));
        header.setPreferredSize(new Dimension(header.getWidth(), 45));

        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        contractTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        contractTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        contractTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        contractTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        contractTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        contractTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        contractTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        contractTable.getColumnModel().getColumn(7).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(contractTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private class BuildingDisplay {

        Building building;

        BuildingDisplay(Building building) {
            this.building = building;
        }

        @Override
        public String toString() {
            return building.getName();
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("👁️ Chi tiết");
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBackground(new Color(33, 150, 243));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {

        protected JButton button;
        private boolean isPushed;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            button.setText("👁️ Chi tiết");
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setBackground(new Color(30, 136, 229));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                showHouseholdDetail(currentRow);
            }
            isPushed = false;
            return "👁️ Chi tiết";
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    private JButton createModernButton(String text, Color baseColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color color1, color2;
                if (getModel().isPressed()) {
                    color1 = baseColor.darker();
                    color2 = baseColor.darker();
                } else if (getModel().isRollover()) {
                    color1 = baseColor;
                    color2 = baseColor.brighter();
                } else {
                    color1 = baseColor;
                    color2 = baseColor.darker();
                }

                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(150, 45));
        return btn;
    }

    private void checkContextAndLoad() {
        removeAll();
        add(contentPanel, BorderLayout.CENTER);

        loadBuildingsFilter();

        if (buildingContext.hasBuildingContext()) {
            Building building = buildingContext.getCurrentBuilding();
            selectBuildingInFilter(building.getId());
        } else {
            if (buildingFilterCombo.getItemCount() > 0) {
                buildingFilterCombo.setSelectedIndex(0);
                loadInitialData();
            }
        }

        revalidate();
        repaint();
    }

    private void loadInitialData() {
        BuildingDisplay selected = (BuildingDisplay) buildingFilterCombo.getSelectedItem();
        if (selected == null) {
            tableModel.setRowCount(0);
            countLabel.setText("📋 Vui lòng chọn tòa nhà");
            contextLabel.setText("");
            return;
        }

        Long buildingId = selected.building.getId();
        contextLabel.setText("📍 " + selected.building.getName());

        isUpdatingCombos = true;

        try {
            allContracts = contractHouseholdDAO.getContractsByBuilding(buildingId);
            floors = floorDAO.getFloorsByBuildingId(buildingId);

            floorFilterCombo.removeAllItems();
            floorFilterCombo.addItem("Tất cả");
            for (Floor floor : floors) {
                String floorName = getFloorDisplayName(floor);
                floorFilterCombo.addItem(floorName);
            }

            loadApartmentsForFloor(null);

        } finally {
            isUpdatingCombos = false;
        }

        // ✅ FIX: Gọi applyFilters thay vì hiển thị tất cả
        applyFilters();
    }

    private void loadBuildingsFilter() {
        isUpdatingCombos = true;

        try {
            buildingFilterCombo.removeAllItems();

            List<Building> buildings = buildingDAO.getAllBuildings();
            Long filterId = permissionManager.getBuildingFilter();

            if (filterId == null) {
                for (Building building : buildings) {
                    buildingFilterCombo.addItem(new BuildingDisplay(building));
                }
            } else {
                for (Building building : buildings) {
                    if (building.getId().equals(filterId)) {
                        buildingFilterCombo.addItem(new BuildingDisplay(building));
                    }
                }
                if (buildingFilterCombo.getItemCount() > 0) {
                    buildingFilterCombo.setSelectedIndex(0);
                    buildingFilterCombo.setEnabled(false);
                }
            }

            if (buildingFilterCombo.getItemCount() == 0 && filterId == null) {
                tableModel.setRowCount(0);
                countLabel.setText("Không có tòa nhà nào");
                contextLabel.setText("");
            }
        } finally {
            isUpdatingCombos = false;
        }
    }

    private void selectBuildingInFilter(Long buildingId) {
        isUpdatingCombos = true;

        try {
            for (int i = 0; i < buildingFilterCombo.getItemCount(); i++) {
                BuildingDisplay bd = buildingFilterCombo.getItemAt(i);
                if (bd.building.getId().equals(buildingId)) {
                    buildingFilterCombo.setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            isUpdatingCombos = false;
        }

        loadInitialData();
    }

    private void onBuildingFilterChanged() {
        BuildingDisplay selected = (BuildingDisplay) buildingFilterCombo.getSelectedItem();
        if (selected == null) {
            return;
        }

        buildingContext.setCurrentBuilding(selected.building);

        searchField.setText("Tìm theo tên, SĐT, căn hộ...");
        searchField.setForeground(new Color(158, 158, 158));

        isUpdatingCombos = true;
        try {
            rbShowLiving.setSelected(true);
        } finally {
            isUpdatingCombos = false;
        }

        loadInitialData();
    }

    private String getFloorDisplayName(Floor floor) {
        if (floor.getName() != null && !floor.getName().trim().isEmpty()) {
            return floor.getName();
        }
        return "Tầng " + floor.getFloorNumber();
    }

    private void onFloorFilterChanged() {
        String selectedFloor = (String) floorFilterCombo.getSelectedItem();

        if ("Tất cả".equals(selectedFloor)) {
            loadApartmentsForFloor(null);
        } else {
            Floor floor = floors.stream()
                    .filter(f -> selectedFloor.equals(getFloorDisplayName(f)))
                    .findFirst()
                    .orElse(null);

            if (floor != null) {
                loadApartmentsForFloor(floor.getId());
            }
        }

        applyFilters();
    }

    private void loadApartmentsForFloor(Long floorId) {
        isUpdatingCombos = true;

        try {
            apartmentFilterCombo.removeAllItems();
            apartmentFilterCombo.addItem("Tất cả");

            BuildingDisplay selected = (BuildingDisplay) buildingFilterCombo.getSelectedItem();
            if (selected == null) {
                return;
            }

            Long buildingId = selected.building.getId();

            if (floorId == null) {
                apartments = apartmentDAO.getApartmentsByBuildingId(buildingId);
            } else {
                apartments = apartmentDAO.getApartmentsByFloorId(floorId);
            }

            for (Apartment apt : apartments) {
                apartmentFilterCombo.addItem(apt.getRoomNumber());
            }
        } finally {
            isUpdatingCombos = false;
        }
    }

    private void applyFilters() {
        if (allContracts == null) {
            return;
        }

        String searchText = searchField.getText().trim().toLowerCase();
        final String keyword = searchText.equals("tìm theo tên, sđt, căn hộ...") ? "" : searchText;

        final String selectedFloor = (String) floorFilterCombo.getSelectedItem();
        final String selectedApartment = (String) apartmentFilterCombo.getSelectedItem();

        List<ContractHouseholdViewModel> filtered = allContracts.stream()
                .filter(c -> {
                    String residencyStatus = c.getResidencyStatus();

                    if (rbShowAll.isSelected()) {
                    } else if (rbShowLiving.isSelected()) {
                        if (!"Đang ở".equals(residencyStatus)) {
                            return false;
                        }
                    } else if (rbShowMoved.isSelected()) {
                        if (!"Đã chuyển đi".equals(residencyStatus)) {
                            return false;
                        }
                    }

                    if (!keyword.isEmpty()) {
                        String residentName = c.getResidentFullName() != null ? c.getResidentFullName().toLowerCase() : "";
                        String phone = c.getResidentPhone() != null ? c.getResidentPhone().toLowerCase() : "";
                        String apartment = c.getApartmentNumber() != null ? c.getApartmentNumber().toLowerCase() : "";

                        if (!residentName.contains(keyword) && !phone.contains(keyword) && !apartment.contains(keyword)) {
                            return false;
                        }
                    }

                    // ✅ FIX: Lọc tầng an toàn hơn (dựa vào danh sách apartments đã load)
                    if (!"Tất cả".equals(selectedFloor)) {
                        // Thay vì so sánh chuỗi tên tầng (có thể lỗi nếu DB tên tầng NULL)
                        // Ta kiểm tra xem căn hộ của hợp đồng này có nằm trong danh sách căn hộ của tầng đang chọn không
                        if (apartments != null) {
                            boolean apartmentInFloor = apartments.stream()
                                    .anyMatch(a -> a.getRoomNumber().equals(c.getApartmentNumber()));
                            if (!apartmentInFloor) {
                                return false;
                            }
                        }
                    }

                    if (!"Tất cả".equals(selectedApartment)) {
                        if (!selectedApartment.equals(c.getApartmentNumber())) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        displayContracts(filtered);
    }

    private void displayContracts(List<ContractHouseholdViewModel> contracts) {
        tableModel.setRowCount(0);

        for (ContractHouseholdViewModel contract : contracts) {
            String totalPeopleDisplay = contract.getTotalPeopleDisplay();

            Object[] row = {
                contract.getApartmentNumber(),
                contract.getFloorName() != null ? contract.getFloorName() : "",
                contract.getResidentFullName(),
                contract.getResidentPhone() != null ? contract.getResidentPhone() : "",
                contract.getResidentIdentityCard() != null ? contract.getResidentIdentityCard() : "",
                totalPeopleDisplay,
                contract.getResidencyStatus(),
                "👁️ Chi tiết"
            };
            tableModel.addRow(row);
        }

        if (contracts.size() == allContracts.size()) {
            countLabel.setText("📋 Tổng số: " + contracts.size() + " hộ gia đình");
        } else {
            countLabel.setText("🔍 Hiển thị: " + contracts.size() + "/" + allContracts.size() + " hộ gia đình");
        }
    }

    private void resetFilters() {
        searchField.setText("Tìm theo tên, SĐT, căn hộ...");
        searchField.setForeground(new Color(158, 158, 158));

        isUpdatingCombos = true;
        try {
            floorFilterCombo.setSelectedIndex(0);
            apartmentFilterCombo.setSelectedIndex(0);
            rbShowLiving.setSelected(true);
        } finally {
            isUpdatingCombos = false;
        }

        loadInitialData();
    }

    private void showHouseholdDetail(int row) {
        List<ContractHouseholdViewModel> filteredContracts = getFilteredContracts();
        if (row < 0 || filteredContracts == null || row >= filteredContracts.size()) {
            return;
        }

        ContractHouseholdViewModel household = filteredContracts.get(row);
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        HouseholdDetailDialog dialog = new HouseholdDetailDialog(parentFrame, household);
        dialog.setVisible(true);
    }

    private List<ContractHouseholdViewModel> getFilteredContracts() {
        if (allContracts == null) {
            return null;
        }

        String searchText = searchField.getText().trim().toLowerCase();
        final String keyword = searchText.equals("tìm theo tên, sđt, căn hộ...") ? "" : searchText;
        final String selectedFloor = (String) floorFilterCombo.getSelectedItem();
        final String selectedApartment = (String) apartmentFilterCombo.getSelectedItem();

        return allContracts.stream()
                .filter(c -> {
                    String residencyStatus = c.getResidencyStatus();
                    if (rbShowAll.isSelected()) {
                    } else if (rbShowLiving.isSelected()) {
                        if (!"Đang ở".equals(residencyStatus)) {
                            return false;
                        }
                    } else if (rbShowMoved.isSelected()) {
                        if (!"Đã chuyển đi".equals(residencyStatus)) {
                            return false;
                        }
                    }
                    if (!keyword.isEmpty()) {
                        String residentName = c.getResidentFullName() != null ? c.getResidentFullName().toLowerCase() : "";
                        String phone = c.getResidentPhone() != null ? c.getResidentPhone().toLowerCase() : "";
                        String apartment = c.getApartmentNumber() != null ? c.getApartmentNumber().toLowerCase() : "";
                        if (!residentName.contains(keyword) && !phone.contains(keyword) && !apartment.contains(keyword)) {
                            return false;
                        }
                    }
                    // ✅ FIX: Logic lọc tầng tương tự
                    if (!"Tất cả".equals(selectedFloor)) {
                        if (apartments != null) {
                            boolean apartmentInFloor = apartments.stream()
                                    .anyMatch(a -> a.getRoomNumber().equals(c.getApartmentNumber()));
                            if (!apartmentInFloor) {
                                return false;
                            }
                        }
                    }
                    if (!"Tất cả".equals(selectedApartment)) {
                        if (!selectedApartment.equals(c.getApartmentNumber())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void showStatistics() {
        if (buildingFilterCombo.getSelectedItem() == null) {
            return;
        }

        int totalHouseholds = tableModel.getRowCount();
        int livingCount = 0;
        int movedCount = 0;
        int totalPeople = 0;

        for (ContractHouseholdViewModel contract : allContracts) {
            if ("Đang ở".equals(contract.getResidencyStatus())) {
                livingCount++;
            } else {
                movedCount++;
            }
            totalPeople += contract.getTotalPeople();
        }

        BuildingDisplay selected = (BuildingDisplay) buildingFilterCombo.getSelectedItem();

        String stats = String.format(
                "<html><body style='width: 400px; padding: 20px; font-family: Segoe UI;'>"
                + "<h2 style='color: #1976d2; text-align: center;'>📊 Thống Kê Cư Dân</h2>"
                + "<h3 style='color: #666; text-align: center; margin-top: 5px;'>%s</h3>"
                + "<hr style='border: 1px solid #e0e0e0; margin: 20px 0;'>"
                + "<table cellpadding='10' style='width: 100%%; font-size: 14px;'>"
                + "<tr style='background: #f5f5f5;'><td colspan='2'><b>📋 Tổng quan</b></td></tr>"
                + "<tr><td>Tổng số hộ gia đình:</td><td align='right'><b style='color: #1976d2; font-size: 16px;'>%d hộ</b></td></tr>"
                + "<tr><td>Tổng số cư dân:</td><td align='right'><b style='color: #1976d2; font-size: 16px;'>%d người</b></td></tr>"
                + "<tr style='background: #f5f5f5;'><td colspan='2'><b>🏠 Trạng thái</b></td></tr>"
                + "<tr><td style='padding-left: 25px;'><span style='color: #2e7d32;'>● Đang ở:</span></td><td align='right'><b>%d hộ</b></td></tr>"
                + "<tr><td style='padding-left: 25px;'><span style='color: #9e9e9e;'>○ Đã chuyển:</span></td><td align='right'>%d hộ</td></tr>"
                + "</table></body></html>",
                selected.building.getName(), totalHouseholds, totalPeople, livingCount, movedCount
        );

        JOptionPane.showMessageDialog(this, stats, "Thống kê cư dân", JOptionPane.PLAIN_MESSAGE);
    }

    private void exportToExcel() {
        JOptionPane.showMessageDialog(this, "Tính năng đang phát triển", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void goToBuildingTab() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        // Assuming MainDashboard has a public method or public navigator
        // For now, this might need adjustment based on your MainDashboard structure
        // This is kept from your original code.
    }

    @Override
    public void onContextChanged(BuildingContext context) {
        checkContextAndLoad();
    }
}
