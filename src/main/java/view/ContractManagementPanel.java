package view;

import dao.ContractDAO;
import dao.BuildingDAO;
import dao.ApartmentDAO;
import dao.ResidentDAO;
import model.Contract;
import model.Building;
import model.Apartment;
import model.Resident;
import util.ExcelExporter;
import util.PermissionManager;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contract Management Panel - FIXED FILTERS Fix: Lọc dựa trên status code
 * (TERMINATED, ACTIVE...) thay vì text hiển thị. Update: Mặc định chỉ hiển thị
 * Hợp đồng Đang hiệu lực và Sắp hết hạn.
 */
public class ContractManagementPanel extends JPanel {

    private ContractDAO contractDAO;
    private BuildingDAO buildingDAO;
    private ApartmentDAO apartmentDAO;
    private ResidentDAO residentDAO;
    private PermissionManager permissionManager;

    private JLabel contextLabel;
    private JLabel countLabel;
    private JTable contractTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    // Filter components
    private JComboBox<BuildingDisplay> buildingFilterCombo;
    private JComboBox<String> typeFilterCombo;

    // Status checkboxes
    private JCheckBox chkShowActive;
    private JCheckBox chkShowExpiring;
    private JCheckBox chkShowExpired;
    private JCheckBox chkShowTerminated;

    private JPanel contentPanel;

    // Cache data
    private List<Contract> allContracts;
    private List<Building> buildings;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private boolean isUpdatingCombos = false;

    public ContractManagementPanel() {
        this.contractDAO = new ContractDAO();
        this.buildingDAO = new BuildingDAO();
        this.apartmentDAO = new ApartmentDAO();
        this.residentDAO = new ResidentDAO();
        this.permissionManager = PermissionManager.getInstance();

        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(new EmptyBorder(30, 30, 30, 30));

        createUI();
        loadInitialData();
    }

    private void createUI() {
        contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        contentPanel.add(createModernHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        centerPanel.add(createFilterBar(), BorderLayout.NORTH);
        centerPanel.add(createModernTable(), BorderLayout.CENTER);

        contentPanel.add(centerPanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createModernHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(25, 30, 25, 30)
        ));

        // ROW 1: Title + Search
        JPanel row1 = new JPanel(new BorderLayout(20, 0));
        row1.setBackground(Color.WHITE);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleRow.setBackground(Color.WHITE);

        JLabel iconLabel = new JLabel("📋");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));

        JLabel titleLabel = new JLabel("Quản Lý Hợp Đồng");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(new Color(33, 33, 33));

        titleRow.add(iconLabel);
        titleRow.add(titleLabel);

        JPanel contextRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contextRow.setBackground(Color.WHITE);

        contextLabel = new JLabel("📊 Tất cả hợp đồng");
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

        final String PLACEHOLDER = "Tìm số HĐ, chủ hộ, căn hộ...";
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

        // ROW 2: Count + Action Buttons
        JPanel row2 = new JPanel(new BorderLayout(20, 0));
        row2.setBackground(Color.WHITE);

        countLabel = new JLabel();
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        countLabel.setForeground(UIConstants.PRIMARY_COLOR);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(Color.WHITE);

        JButton addBtn = createModernButton("Tạo hợp đồng", new Color(99, 102, 241));
        addBtn.setPreferredSize(new Dimension(160, 42));
        addBtn.addActionListener(e -> showCreateContractDialog());
        addBtn.setVisible(permissionManager.canAdd(PermissionManager.MODULE_CONTRACTS));

        JButton statsBtn = createModernButton("Thống kê", new Color(103, 58, 181));
        statsBtn.setPreferredSize(new Dimension(140, 42));
        statsBtn.addActionListener(e -> showStatistics());

        JButton exportBtn = createModernButton("Xuất Excel", new Color(67, 160, 71));
        exportBtn.setPreferredSize(new Dimension(140, 42));
        exportBtn.addActionListener(e -> exportToExcel());

        actionPanel.add(addBtn);
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

        JLabel buildingLabel = new JLabel("Tòa nhà:");
        buildingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buildingFilterCombo = new JComboBox<>();
        buildingFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buildingFilterCombo.setBackground(Color.WHITE);
        buildingFilterCombo.setPreferredSize(new Dimension(180, 38));
        buildingFilterCombo.addActionListener(e -> {
            if (!isUpdatingCombos) {
                applyFilters();
            }
        });

        JLabel typeLabel = new JLabel("Loại:");
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        typeFilterCombo = createFilterCombo();
        typeFilterCombo.addItem("Tất cả");
        typeFilterCombo.addItem("Thuê");
        typeFilterCombo.addItem("Sở hữu");
        typeFilterCombo.setPreferredSize(new Dimension(120, 38));
        typeFilterCombo.addActionListener(e -> {
            if (!isUpdatingCombos) {
                applyFilters();
            }
        });

        row1.add(filterLabel);
        row1.add(Box.createHorizontalStrut(10));
        row1.add(buildingLabel);
        row1.add(buildingFilterCombo);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(typeLabel);
        row1.add(typeFilterCombo);

        // ROW 2: Status checkboxes
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row2.setBackground(Color.WHITE);

        JLabel statusLabel = new JLabel("Hiển thị:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(66, 66, 66));

        // ✅ CẬP NHẬT: Mặc định chỉ chọn Active và Expiring, bỏ chọn Expired và Terminated
        chkShowActive = createStatusCheckbox("Đang hiệu lực", new Color(46, 125, 50), true);
        chkShowExpiring = createStatusCheckbox("Sắp hết hạn", new Color(230, 126, 34), true);
        chkShowExpired = createStatusCheckbox("Đã hết hạn", new Color(211, 47, 47), false);
        chkShowTerminated = createStatusCheckbox("Đã thanh lý/Kết thúc", new Color(117, 117, 117), false); // Changed to false

        row2.add(statusLabel);
        row2.add(Box.createHorizontalStrut(15));
        row2.add(chkShowActive);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(chkShowExpiring);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(chkShowExpired);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(chkShowTerminated);

        mainFilterPanel.add(row1);
        mainFilterPanel.add(Box.createVerticalStrut(12));
        mainFilterPanel.add(row2);

        return mainFilterPanel;
    }

    private JCheckBox createStatusCheckbox(String text, Color color, boolean selected) {
        JCheckBox checkbox = new JCheckBox(text);
        checkbox.setSelected(selected);
        checkbox.setFont(new Font("Segoe UI", Font.BOLD, 13));
        checkbox.setForeground(color);
        checkbox.setBackground(Color.WHITE);
        checkbox.setFocusPainted(false);
        checkbox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        checkbox.addActionListener(e -> applyFilters());
        return checkbox;
    }

    private JComboBox<String> createFilterCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        return combo;
    }

    private JPanel createModernTable() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true));

        String[] columns = {
            "Số HĐ", "Căn hộ", "Chủ hộ", "Loại", "Ngày ký/BĐ", "Kết thúc", "Trạng thái", "Thao tác"
        };
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

        // Context Menu
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem itemDetail = new JMenuItem("Xem chi tiết");
        itemDetail.addActionListener(e -> {
            int row = contractTable.getSelectedRow();
            if (row != -1) {
                showContractDetail(row);
            }
        });

        if (permissionManager.canEdit(PermissionManager.MODULE_CONTRACTS)) {
            JMenuItem itemEdit = new JMenuItem("Chỉnh sửa thông tin");
            itemEdit.addActionListener(e -> {
                int row = contractTable.getSelectedRow();
                if (row != -1) {
                    performEditContract(row);
                }
            });
            JMenuItem itemRenew = new JMenuItem("Gia hạn hợp đồng");
            itemRenew.addActionListener(e -> {
                int row = contractTable.getSelectedRow();
                if (row != -1) {
                    showContractDetail(row);
                }
            });
            JMenuItem itemTerminate = new JMenuItem("Thanh lý hợp đồng");
            itemTerminate.setForeground(new Color(211, 47, 47));
            itemTerminate.addActionListener(e -> {
                int row = contractTable.getSelectedRow();
                if (row != -1) {
                    showContractDetail(row);
                }
            });

            popupMenu.add(itemDetail);
            popupMenu.addSeparator();
            popupMenu.add(itemEdit);
            popupMenu.addSeparator();
            popupMenu.add(itemRenew);
            popupMenu.add(itemTerminate);
        } else {
            popupMenu.add(itemDetail);
        }

        contractTable.setComponentPopupMenu(popupMenu);
        contractTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = contractTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < contractTable.getRowCount()) {
                        contractTable.setRowSelectionInterval(row, row);
                    }
                }
            }
        });

        // Columns Config
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < contractTable.getColumnCount() - 1; i++) {
            contractTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        contractTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
        contractTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        contractTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox()));

        JTableHeader header = contractTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(250, 250, 250));
        header.setForeground(new Color(66, 66, 66));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)));
        header.setPreferredSize(new Dimension(header.getWidth(), 45));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        contractTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        contractTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        contractTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        contractTable.getColumnModel().getColumn(6).setPreferredWidth(130);
        contractTable.getColumnModel().getColumn(7).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(contractTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    // ✅ Helper method to categorize contract status (Logic cốt lõi)
    private String getContractCategory(Contract c) {
        if ("TERMINATED".equalsIgnoreCase(c.getStatus()) || "CANCELLED".equalsIgnoreCase(c.getStatus())) {
            return "TERMINATED";
        }
        if ("ACTIVE".equalsIgnoreCase(c.getStatus())) {
            if (c.getEndDate() == null) {
                return "ACTIVE"; // Vô thời hạn
            }
            long diff = c.getEndDate().getTime() - System.currentTimeMillis();
            if (diff < 0) {
                return "EXPIRED";
            }
            if (diff <= 30L * 24 * 60 * 60 * 1000) {
                return "EXPIRING";
            }
            return "ACTIVE";
        }
        return "UNKNOWN";
    }

    // ✅ Helper method to get display string consistent with logic
    private String getStatusDisplayString(Contract c) {
        String cat = getContractCategory(c);
        switch (cat) {
            case "ACTIVE":
                return "Đang hiệu lực";
            case "EXPIRING":
                return "Sắp hết hạn";
            case "EXPIRED":
                return "Đã hết hạn";
            case "TERMINATED":
                return "Đã thanh lý"; // Changed text to match logic
            default:
                return "Không xác định";
        }
    }

    // ✅ FIXED: Apply Filters using robust logic
    private void applyFilters() {
        if (allContracts == null) {
            return;
        }
        String searchText = searchField.getText().trim().toLowerCase();
        final String keyword = searchText.equals("tìm số hđ, chủ hộ, căn hộ...") ? "" : searchText;
        final BuildingDisplay selectedBuilding = (BuildingDisplay) buildingFilterCombo.getSelectedItem();
        final String selectedType = (String) typeFilterCombo.getSelectedItem();

        List<Contract> filtered = allContracts.stream().filter(contract -> {
            // 1. Status Filter (Using Logic, not Text)
            String category = getContractCategory(contract);
            boolean showThis = false;

            if (chkShowActive.isSelected() && "ACTIVE".equals(category)) {
                showThis = true;
            }
            if (chkShowExpiring.isSelected() && "EXPIRING".equals(category)) {
                showThis = true;
            }
            if (chkShowExpired.isSelected() && "EXPIRED".equals(category)) {
                showThis = true;
            }
            if (chkShowTerminated.isSelected() && "TERMINATED".equals(category)) {
                showThis = true;
            }

            if (!showThis) {
                return false;
            }

            // 2. Keyword Filter
            if (!keyword.isEmpty()) {
                String contractNumber = contract.getContractNumber() != null ? contract.getContractNumber().toLowerCase() : "";
                Apartment apt = apartmentDAO.getApartmentById(contract.getApartmentId());
                String apartmentNumber = apt != null ? apt.getRoomNumber().toLowerCase() : "";
                Resident resident = residentDAO.getResidentById(contract.getResidentId());
                String residentName = resident != null ? resident.getFullName().toLowerCase() : "";
                if (!contractNumber.contains(keyword) && !apartmentNumber.contains(keyword) && !residentName.contains(keyword)) {
                    return false;
                }
            }

            // 3. Building Filter
            if (selectedBuilding != null && selectedBuilding.building.getId() != null) {
                Apartment apt = apartmentDAO.getApartmentById(contract.getApartmentId());
                if (apt == null) {
                    return false;
                }
                List<Apartment> buildingApts = apartmentDAO.getApartmentsByBuildingId(selectedBuilding.building.getId());
                boolean inBuilding = buildingApts.stream().anyMatch(a -> a.getId().equals(apt.getId()));
                if (!inBuilding) {
                    return false;
                }
            }

            // 4. Type Filter
            if (!"Tất cả".equals(selectedType)) {
                if (!selectedType.equals(contract.getContractTypeDisplay())) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        displayContracts(filtered);
    }

    private void displayContracts(List<Contract> contracts) {
        tableModel.setRowCount(0);
        for (Contract contract : contracts) {
            Apartment apartment = apartmentDAO.getApartmentById(contract.getApartmentId());
            String apartmentNumber = apartment != null ? apartment.getRoomNumber() : "N/A";
            Resident resident = residentDAO.getResidentById(contract.getResidentId());
            String residentName = resident != null ? resident.getFullName() : "N/A";

            String startDateStr = "";
            String endDateStr = "";
            if (contract.isRental()) {
                startDateStr = contract.getStartDate() != null ? dateFormat.format(contract.getStartDate()) : "";
                endDateStr = contract.getEndDate() != null ? dateFormat.format(contract.getEndDate()) : "Vô thời hạn";
            } else {
                startDateStr = contract.getSignedDate() != null ? "Ký: " + dateFormat.format(contract.getSignedDate()) : "N/A";
                endDateStr = "—";
            }

            // ✅ Use standardized status string
            String statusStr = getStatusDisplayString(contract);

            Object[] row = {
                contract.getContractNumber(), apartmentNumber, residentName, contract.getContractTypeDisplay(),
                startDateStr, endDateStr, statusStr, "👁️ Chi tiết"
            };
            tableModel.addRow(row);
        }
        countLabel.setText(contracts.size() == allContracts.size()
                ? "📋 Tổng số: " + contracts.size() + " hợp đồng"
                : "🔍 Hiển thị: " + contracts.size() + "/" + allContracts.size() + " hợp đồng");
    }

    // ✅ FIXED: Statistics using robust logic
    private void showStatistics() {
        if (allContracts == null) {
            return;
        }
        int totalContracts = allContracts.size();

        int activeCount = 0, expiringCount = 0, expiredCount = 0, terminatedCount = 0;

        for (Contract c : allContracts) {
            String cat = getContractCategory(c);
            switch (cat) {
                case "ACTIVE":
                    activeCount++;
                    break;
                case "EXPIRING":
                    expiringCount++;
                    break;
                case "EXPIRED":
                    expiredCount++;
                    break;
                case "TERMINATED":
                    terminatedCount++;
                    break;
            }
        }

        int rentalCount = (int) allContracts.stream().filter(c -> "RENTAL".equals(c.getContractType())).count();
        int ownershipCount = (int) allContracts.stream().filter(c -> "OWNERSHIP".equals(c.getContractType())).count();

        String stats = String.format("<html><body style='width: 400px; padding: 20px; font-family: Segoe UI;'>"
                + "<h2 style='color: #1976d2; text-align: center;'>📊 Thống Kê Hợp Đồng</h2>"
                + "<hr style='border: 1px solid #e0e0e0; margin: 20px 0;'>"
                + "<table cellpadding='10' style='width: 100%%; font-size: 14px;'>"
                + "<tr style='background: #f5f5f5;'><td colspan='2'><b>📋 Tổng quan</b></td></tr>"
                + "<tr><td>Tổng số hợp đồng:</td><td align='right'><b style='color: #1976d2; font-size: 16px;'>%d</b></td></tr>"
                + "<tr style='background: #f5f5f5;'><td colspan='2'><b>📑 Theo loại</b></td></tr>"
                + "<tr><td style='padding-left: 25px;'>Thuê:</td><td align='right'><b>%d</b></td></tr>"
                + "<tr><td style='padding-left: 25px;'>Sở hữu:</td><td align='right'><b>%d</b></td></tr>"
                + "<tr style='background: #f5f5f5;'><td colspan='2'><b>🎯 Theo trạng thái</b></td></tr>"
                + "<tr><td style='padding-left: 25px;'><span style='color: #2e7d32;'>● Đang hiệu lực:</span></td><td align='right'><b>%d</b></td></tr>"
                + "<tr><td style='padding-left: 25px;'><span style='color: #e67e22;'>⚠ Sắp hết hạn:</span></td><td align='right'><b>%d</b></td></tr>"
                + "<tr><td style='padding-left: 25px;'><span style='color: #d32f2f;'>✕ Đã hết hạn:</span></td><td align='right'><b>%d</b></td></tr>"
                + "<tr><td style='padding-left: 25px;'><span style='color: #757575;'>○ Đã thanh lý:</span></td><td align='right'>%d</td></tr>"
                + "</table></body></html>",
                totalContracts, rentalCount, ownershipCount, activeCount, expiringCount, expiredCount, terminatedCount);
        JOptionPane.showMessageDialog(this, stats, "Thống kê hợp đồng", JOptionPane.PLAIN_MESSAGE);
    }

    class StatusCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String status = (String) value;
            JLabel label = (JLabel) c;
            label.setOpaque(true);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));

            if ("Đang hiệu lực".equals(status)) {
                if (!isSelected) {
                    label.setBackground(new Color(232, 245, 233));
                    label.setForeground(new Color(46, 125, 50));
                }
                label.setText("● " + status);
            } else if ("Sắp hết hạn".equals(status)) {
                if (!isSelected) {
                    label.setBackground(new Color(255, 243, 224));
                    label.setForeground(new Color(230, 126, 34));
                }
                label.setText("⚠ " + status);
            } else if ("Đã hết hạn".equals(status)) {
                if (!isSelected) {
                    label.setBackground(new Color(255, 235, 238));
                    label.setForeground(new Color(211, 47, 47));
                }
                label.setText("✕ " + status);
            } else if ("Đã thanh lý".equals(status)) { // ✅ Updated text
                if (!isSelected) {
                    label.setBackground(new Color(245, 245, 245));
                    label.setForeground(new Color(117, 117, 117));
                }
                label.setText("○ " + status);
            }

            return label;
        }
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
                showContractDetail(currentRow);
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
        return btn;
    }

    private void loadInitialData() {
        isUpdatingCombos = true;
        try {
            buildings = buildingDAO.getAllBuildings();
            buildingFilterCombo.removeAllItems();
            List<Long> buildingIds = permissionManager.getBuildingIds();

            // ✅ FIX: Nếu Admin (buildingIds = null) → Hiển thị "Tất cả" + các tòa
            if (buildingIds == null) {
                buildingFilterCombo.addItem(new BuildingDisplay(new Building(null, "Tất cả", null, null, null, null, false)));
                for (Building building : buildings) {
                    buildingFilterCombo.addItem(new BuildingDisplay(building));
                }
                buildingFilterCombo.setEnabled(true);
            } 
            // ✅ FIX: Nếu Manager/Staff → Hiển thị các tòa được phân quyền
            else {
                for (Building building : buildings) {
                    if (buildingIds.contains(building.getId())) {
                        buildingFilterCombo.addItem(new BuildingDisplay(building));
                    }
                }
                
                // ✅ QUAN TRỌNG: Luôn enable dropdown để cho phép chọn giữa các tòa
                buildingFilterCombo.setEnabled(true);
                
                if (buildingFilterCombo.getItemCount() > 0) {
                    buildingFilterCombo.setSelectedIndex(0);
                }
            }
            
            allContracts = contractDAO.getAllContracts();
        } finally {
            isUpdatingCombos = false;
        }
        applyFilters();
    }

    private void resetFilters() {
        searchField.setText("Tìm số HĐ, chủ hộ, căn hộ...");
        searchField.setForeground(new Color(158, 158, 158));
        isUpdatingCombos = true;
        try {
            buildingFilterCombo.setSelectedIndex(0);
            typeFilterCombo.setSelectedIndex(0);

            // ✅ CẬP NHẬT MẶC ĐỊNH KHI RESET:
            chkShowActive.setSelected(true);
            chkShowExpiring.setSelected(true);
            chkShowExpired.setSelected(false);
            chkShowTerminated.setSelected(false);
        } finally {
            isUpdatingCombos = false;
        }
        loadInitialData();
    }

    private void showContractDetail(int row) {
        List<Contract> filteredContracts = getFilteredContracts();
        if (row < 0 || filteredContracts == null || row >= filteredContracts.size()) {
            return;
        }
        Contract selectedContract = filteredContracts.get(row);
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        ContractDetailDialog dialog = new ContractDetailDialog(parentFrame, selectedContract.getId());
        dialog.setVisible(true);
        reloadData();
    }

    private void performEditContract(int row) {
        List<Contract> filteredContracts = getFilteredContracts();
        if (row < 0 || filteredContracts == null || row >= filteredContracts.size()) {
            return;
        }

        Contract selectedContract = filteredContracts.get(row);

        if ("TERMINATED".equals(selectedContract.getStatus()) || "CANCELLED".equals(selectedContract.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Không thể chỉnh sửa hợp đồng đã kết thúc hoặc đã hủy!",
                    "Thao tác bị chặn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        ContractFormDialog dialog = new ContractFormDialog(parentFrame, selectedContract);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            reloadData();
        }
    }

    private List<Contract> getFilteredContracts() {
        if (allContracts == null) {
            return null;
        }
        String searchText = searchField.getText().trim().toLowerCase();
        final String keyword = searchText.equals("tìm số hđ, chủ hộ, căn hộ...") ? "" : searchText;
        final BuildingDisplay selectedBuilding = (BuildingDisplay) buildingFilterCombo.getSelectedItem();
        final String selectedType = (String) typeFilterCombo.getSelectedItem();

        return allContracts.stream().filter(contract -> {
            String category = getContractCategory(contract);
            boolean showThis = false;
            if (chkShowActive.isSelected() && "ACTIVE".equals(category)) {
                showThis = true;
            }
            if (chkShowExpiring.isSelected() && "EXPIRING".equals(category)) {
                showThis = true;
            }
            if (chkShowExpired.isSelected() && "EXPIRED".equals(category)) {
                showThis = true;
            }
            if (chkShowTerminated.isSelected() && "TERMINATED".equals(category)) {
                showThis = true;
            }
            if (!showThis) {
                return false;
            }

            if (!keyword.isEmpty()) {
                String contractNumber = contract.getContractNumber() != null ? contract.getContractNumber().toLowerCase() : "";
                Apartment apt = apartmentDAO.getApartmentById(contract.getApartmentId());
                String apartmentNumber = apt != null ? apt.getRoomNumber().toLowerCase() : "";
                Resident resident = residentDAO.getResidentById(contract.getResidentId());
                String residentName = resident != null ? resident.getFullName().toLowerCase() : "";
                if (!contractNumber.contains(keyword) && !apartmentNumber.contains(keyword) && !residentName.contains(keyword)) {
                    return false;
                }
            }
            if (selectedBuilding != null && selectedBuilding.building.getId() != null) {
                Apartment apt = apartmentDAO.getApartmentById(contract.getApartmentId());
                if (apt == null) {
                    return false;
                }
                List<Apartment> buildingApts = apartmentDAO.getApartmentsByBuildingId(selectedBuilding.building.getId());
                if (buildingApts.stream().noneMatch(a -> a.getId().equals(apt.getId()))) {
                    return false;
                }
            }
            if (!"Tất cả".equals(selectedType)) {
                if (!selectedType.equals(contract.getContractTypeDisplay())) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    private void showCreateContractDialog() {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        ContractFormDialog dialog = new ContractFormDialog(parentFrame, null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            reloadData();
        }
    }

    private void exportToExcel() {
        ExcelExporter.exportTable(contractTable, "HopDong", "DANH SÁCH HỢP ĐỒNG", this);
    }

    public void reloadData() {
        loadInitialData();
    }
}