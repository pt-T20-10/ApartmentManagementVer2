package view;

import dao.*;
import model.*;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import util.MoneyFormatter;
import java.util.Calendar;

/**
 * Contract Detail Dialog with DYNAMIC DISPLAY based on contract type Updated:
 * Removed Status column and logic * RENTAL (Thuê): - Shows: Tiền thuê/tháng,
 * Ngày bắt đầu, Ngày kết thúc - Actions: Sửa, Gia hạn, Kết thúc * OWNERSHIP (Sở
 * hữu): - Shows: Giá mua, CHỈ Ngày ký (no start/end) - Actions: Sửa, Kết thúc
 * (NO Gia hạn)
 */
public class ContractDetailDialog extends JDialog {

    private ContractDAO contractDAO;
    private ApartmentDAO apartmentDAO;
    private ResidentDAO residentDAO;
    private ContractServiceDAO contractServiceDAO;
    private ContractHistoryDAO contractHistoryDAO;
    private FloorDAO floorDAO;
    private BuildingDAO buildingDAO;

    private Contract contract;
    private Apartment apartment;
    private Resident resident;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private ContractHistoryPanel historyPanel;

    // Panel chứa tab dịch vụ để có thể refresh
    private JPanel servicesContentPanel;

    public ContractDetailDialog(JFrame parent, Long contractId) {
        super(parent, "Chi Tiết Hợp Đồng", true);

        this.contractDAO = new ContractDAO();
        this.apartmentDAO = new ApartmentDAO();
        this.residentDAO = new ResidentDAO();
        this.contractServiceDAO = new ContractServiceDAO();
        this.contractHistoryDAO = new ContractHistoryDAO();
        this.floorDAO = new FloorDAO();
        this.buildingDAO = new BuildingDAO();

        this.contract = contractDAO.getContractById(contractId);
        if (contract == null) {
            JOptionPane.showMessageDialog(parent,
                    "Không tìm thấy hợp đồng!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        this.apartment = apartmentDAO.getApartmentById(contract.getApartmentId());
        this.resident = residentDAO.getResidentById(contract.getResidentId());

        initComponents();

        setSize(950, 800);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);

        add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = createTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_COLOR),
                new EmptyBorder(20, 25, 20, 25)
        ));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setBackground(Color.WHITE);

        JLabel iconLabel = new JLabel("📋");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Hợp Đồng " + contract.getContractNumber());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(33, 33, 33));

        JLabel subtitleLabel = new JLabel(contract.getContractTypeDisplay());
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(117, 117, 117));

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitleLabel);

        leftPanel.add(iconLabel);
        leftPanel.add(textPanel);

        JPanel statusBadge = createStatusBadge();

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(statusBadge, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createStatusBadge() {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        String status = contract.getStatusDisplay();
        Color bgColor, fgColor;
        String icon;

        if ("Đang hiệu lực".equals(status)) {
            bgColor = new Color(232, 245, 233);
            fgColor = new Color(46, 125, 50);
            icon = "●";
        } else if ("Sắp hết hạn".equals(status)) {
            bgColor = new Color(255, 243, 224);
            fgColor = new Color(230, 126, 34);
            icon = "⚠";
        } else if ("Đã hết hạn".equals(status)) {
            bgColor = new Color(255, 235, 238);
            fgColor = new Color(211, 47, 47);
            icon = "✕";
        } else {
            bgColor = new Color(250, 250, 250);
            fgColor = new Color(158, 158, 158);
            icon = "○";
        }

        badge.setBackground(bgColor);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fgColor, 2, true),
                new EmptyBorder(5, 15, 5, 15)
        ));

        JLabel statusLabel = new JLabel(icon + " " + status);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(fgColor);

        badge.add(statusLabel);

        return badge;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(UIConstants.BACKGROUND_COLOR);

        JPanel infoPanel = createInfoPanel();
        tabbedPane.addTab("📋 Thông tin", infoPanel);

        servicesContentPanel = new JPanel(new BorderLayout(0, 15));
        servicesContentPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        servicesContentPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        refreshServiceTab();
        tabbedPane.addTab("🔧 Dịch vụ", servicesContentPanel);

        historyPanel = new ContractHistoryPanel(contract.getId());
        tabbedPane.addTab("📜 Lịch sử", historyPanel);

        return tabbedPane;
    }

    private void refreshServiceTab() {
        servicesContentPanel.removeAll();

        List<ContractService> services = contractServiceDAO.getServicesByContract(contract.getId());

        // Thống kê đơn giản (Tổng số dịch vụ)
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        statsPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel lblCount = new JLabel("Tổng số dịch vụ sử dụng: " + services.size());
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCount.setForeground(new Color(75, 85, 99));
        statsPanel.add(lblCount);

        servicesContentPanel.add(statsPanel, BorderLayout.NORTH);

        if (services.isEmpty()) {
            servicesContentPanel.add(createEmptyServicePanel(), BorderLayout.CENTER);
        } else {
            JPanel tablePanel = createServiceTable(services);
            servicesContentPanel.add(tablePanel, BorderLayout.CENTER);
        }

        servicesContentPanel.revalidate();
        servicesContentPanel.repaint();
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(UIConstants.BACKGROUND_COLOR);

        panel.add(createContractInfoSection());
        panel.add(Box.createVerticalStrut(15));
        panel.add(createApartmentInfoSection());
        panel.add(Box.createVerticalStrut(15));
        panel.add(createResidentInfoSection());
        panel.add(Box.createVerticalStrut(15));

        if (contract.getNotes() != null && !contract.getNotes().trim().isEmpty()) {
            panel.add(createNotesSection());
        }

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(scrollPane, BorderLayout.CENTER);

        return wrapperPanel;
    }

    private JPanel createEmptyServicePanel() {
        JPanel emptyPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(220, 220, 220));
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[]{9}, 0));
                g2d.drawRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 12, 12);
            }
        };
        emptyPanel.setBackground(Color.WHITE);
        emptyPanel.setBorder(new EmptyBorder(60, 20, 60, 20));

        JPanel emptyContent = new JPanel();
        emptyContent.setLayout(new BoxLayout(emptyContent, BoxLayout.Y_AXIS));
        emptyContent.setOpaque(false);

        JLabel emptyIcon = new JLabel("📦");
        emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptyTitle = new JLabel("Chưa có dịch vụ");
        emptyTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        emptyTitle.setForeground(new Color(117, 117, 117));
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptyDesc = new JLabel("Hợp đồng này chưa đăng ký dịch vụ nào");
        emptyDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emptyDesc.setForeground(new Color(158, 158, 158));
        emptyDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyContent.add(emptyIcon);
        emptyContent.add(Box.createVerticalStrut(15));
        emptyContent.add(emptyTitle);
        emptyContent.add(Box.createVerticalStrut(8));
        emptyContent.add(emptyDesc);

        emptyPanel.add(emptyContent);
        return emptyPanel;
    }

    private JPanel createServiceTable(List<ContractService> services) {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));

        // ✅ Cập nhật: Đã xóa cột trạng thái
        String[] columns = {"Dịch vụ", "Đơn giá", "Đơn vị", "Ngày áp dụng"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (ContractService cs : services) {
            Object[] row = {
                cs.getServiceName(),
                currencyFormat.format(cs.getUnitPrice()),
                cs.getUnitTypeDisplay(),
                dateFormat.format(cs.getAppliedDate())
            };
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(45);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(243, 244, 246));
        table.setSelectionForeground(new Color(33, 33, 33));

        // Header styling
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(249, 250, 251));
        table.getTableHeader().setForeground(new Color(75, 85, 99));
        table.getTableHeader().setPreferredSize(new Dimension(table.getTableHeader().getWidth(), 45));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.BORDER_COLOR));

        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // Column 0: Service name
        table.getColumnModel().getColumn(0).setPreferredWidth(250);

        // Column 1: Price
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                label.setForeground(new Color(22, 163, 74));
                return label;
            }
        });

        // Column 2: Unit
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        // Column 3: Date
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        return tablePanel;
    }

    private JPanel createContractInfoSection() {
        JPanel section = createSection("📋 Thông Tin Hợp Đồng");
        section.setLayout(new GridLayout(5, 4, 15, 12));
        section.setBorder(BorderFactory.createCompoundBorder(section.getBorder(), new EmptyBorder(15, 20, 15, 20)));
        section.add(createInfoLabel("Số hợp đồng:"));
        section.add(createInfoValue(contract.getContractNumber()));
        section.add(createInfoLabel("Loại hợp đồng:"));
        section.add(createInfoValue(contract.getContractTypeDisplay()));
        section.add(createInfoLabel("Ngày ký:"));
        section.add(createInfoValue(contract.getSignedDate() != null ? dateFormat.format(contract.getSignedDate()) : "Chưa ký"));
        if (contract.isRental()) {
            section.add(createInfoLabel("Ngày bắt đầu:"));
            section.add(createInfoValue(contract.getStartDate() != null ? dateFormat.format(contract.getStartDate()) : "N/A"));
        } else {
            section.add(createInfoLabel(""));
            section.add(createInfoValue(""));
        }
        if (contract.isRental()) {
            section.add(createInfoLabel("Ngày kết thúc:"));
            section.add(createInfoValue(contract.getEndDate() != null ? dateFormat.format(contract.getEndDate()) : "Vô thời hạn"));
        } else {
            section.add(createInfoLabel(""));
            section.add(createInfoValue(""));
        }
        section.add(createInfoLabel("Tiền cọc:"));
        section.add(createInfoValue(currencyFormat.format(contract.getDepositAmount())));
        section.add(createInfoLabel(contract.getPriceLabel() + ":"));
        section.add(createInfoValue(contract.getMonthlyRent() != null ? currencyFormat.format(contract.getMonthlyRent()) : "N/A"));
        section.add(createInfoLabel(""));
        section.add(createInfoValue(""));
        section.add(createInfoLabel("Ngày tạo:"));
        section.add(createInfoValue(contract.getCreatedAt() != null ? dateFormat.format(contract.getCreatedAt()) : "N/A"));
        section.add(createInfoLabel("Cập nhật lần cuối:"));
        section.add(createInfoValue(contract.getUpdatedAt() != null ? dateFormat.format(contract.getUpdatedAt()) : "N/A"));
        return section;
    }

    private JPanel createApartmentInfoSection() {
        JPanel section = createSection("🏠 Thông Tin Căn Hộ");
        section.setLayout(new GridLayout(3, 4, 15, 12));
        section.setBorder(BorderFactory.createCompoundBorder(section.getBorder(), new EmptyBorder(15, 20, 15, 20)));
        if (apartment != null) {
            Floor floor = floorDAO.getFloorById(apartment.getFloorId());
            Building building = (floor != null) ? buildingDAO.getBuildingById(floor.getBuildingId()) : null;
            section.add(createInfoLabel("Tòa nhà:"));
            section.add(createInfoValue(building != null ? building.getName() : "N/A"));
            section.add(createInfoLabel("Tầng:"));
            section.add(createInfoValue(floor != null ? (floor.getName() != null ? floor.getName() : "Tầng " + floor.getFloorNumber()) : "N/A"));
            section.add(createInfoLabel("Căn hộ:"));
            section.add(createInfoValue(apartment.getRoomNumber()));
            section.add(createInfoLabel("Diện tích:"));
            section.add(createInfoValue(apartment.getArea() + " m²"));
            section.add(createInfoLabel("Loại căn hộ:"));
            section.add(createInfoValue(apartment.getApartmentType()));
            section.add(createInfoLabel("Số phòng:"));
            section.add(createInfoValue(apartment.getBedroomCount() + " PN, " + apartment.getBathroomCount() + " PT"));
        } else {
            section.add(createInfoValue("Không tìm thấy thông tin căn hộ"));
        }
        return section;
    }

    private JPanel createResidentInfoSection() {
        JPanel section = createSection("👤 Thông Tin Chủ Hộ");
        section.setLayout(new GridLayout(2, 4, 15, 12));
        section.setBorder(BorderFactory.createCompoundBorder(section.getBorder(), new EmptyBorder(15, 20, 15, 20)));
        if (resident != null) {
            section.add(createInfoLabel("Họ tên:"));
            section.add(createInfoValue(resident.getFullName()));
            section.add(createInfoLabel("Số điện thoại:"));
            section.add(createInfoValue(resident.getPhone()));
            section.add(createInfoLabel("CCCD/CMND:"));
            section.add(createInfoValue(resident.getIdentityCard()));
            section.add(createInfoLabel("Email:"));
            section.add(createInfoValue(resident.getEmail() != null ? resident.getEmail() : "N/A"));
        } else {
            section.add(createInfoValue("Không tìm thấy thông tin cư dân"));
        }
        return section;
    }

    private JPanel createNotesSection() {
        JPanel section = createSection("📝 Ghi Chú");
        section.setLayout(new BorderLayout(0, 10));
        section.setBorder(BorderFactory.createCompoundBorder(section.getBorder(), new EmptyBorder(15, 20, 15, 20)));
        JTextArea txtNotes = new JTextArea(contract.getNotes());
        txtNotes.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        txtNotes.setEditable(false);
        txtNotes.setBackground(new Color(250, 250, 250));
        txtNotes.setBorder(new EmptyBorder(10, 10, 10, 10));
        section.add(txtNotes, BorderLayout.CENTER);
        return section;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_COLOR));
        JButton btnEdit = createButton("✏️ Sửa", new Color(33, 150, 243));
        btnEdit.addActionListener(e -> editContract());
        JButton btnRenew = createButton("🔄 Gia hạn", new Color(76, 175, 80));
        btnRenew.setForeground(Color.WHITE);
        btnRenew.addActionListener(e -> renewContract());
        btnRenew.setEnabled(contract.canBeRenewed());
        btnRenew.setVisible(contract.isRental());
        JButton btnTerminate = createButton("❌ Kết thúc", new Color(244, 67, 54));
        btnTerminate.setForeground(Color.WHITE);
        btnTerminate.addActionListener(e -> terminateContract());
        btnTerminate.setEnabled(contract.canBeTerminated());
        JButton btnClose = createButton("Đóng", new Color(158, 158, 158));
        btnClose.addActionListener(e -> dispose());
        panel.add(btnEdit);
        if (contract.isRental()) {
            panel.add(btnRenew);
        }
        panel.add(btnTerminate);
        panel.add(btnClose);
        return panel;
    }

    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true), title, TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 15), new Color(66, 66, 66)));
        return panel;
    }

    private JLabel createInfoLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(new Color(117, 117, 117));
        return l;
    }

    private JLabel createInfoValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(new Color(33, 33, 33));
        return l;
    }

    private JButton createButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(130, 40));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void editContract() {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        ContractFormDialog dialog = new ContractFormDialog(parentFrame, contract);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            contract = contractDAO.getContractById(contract.getId());
            if (historyPanel != null) {
                historyPanel.refresh();
            }
            refreshServiceTab();
            JOptionPane.showMessageDialog(parentFrame, "Cập nhật hợp đồng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void renewContract() {
        if (!contract.isRental()) {
            JOptionPane.showMessageDialog(this, "Chỉ hợp đồng thuê mới có thể gia hạn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(400, 180));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Hết hạn hiện tại:"), gbc);
        gbc.gridx = 1;
        JLabel lblCurrentEnd = new JLabel(contract.getEndDate() != null ? dateFormat.format(contract.getEndDate()) : "Vô thời hạn");
        lblCurrentEnd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCurrentEnd.setForeground(new Color(100, 116, 139));
        panel.add(lblCurrentEnd, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Ngày kết thúc mới:"), gbc);
        gbc.gridx = 1;
        JSpinner spnNewDate = new JSpinner(new SpinnerDateModel());
        spnNewDate.setEditor(new JSpinner.DateEditor(spnNewDate, "dd/MM/yyyy"));
        spnNewDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        Calendar cal = Calendar.getInstance();
        if (contract.getEndDate() != null) {
            cal.setTime(contract.getEndDate());
        }
        cal.add(Calendar.YEAR, 1);
        spnNewDate.setValue(cal.getTime());
        panel.add(spnNewDate, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Giá thuê mới (VNĐ):"), gbc);
        gbc.gridx = 1;
        JTextField txtNewPrice = MoneyFormatter.createMoneyField();
        MoneyFormatter.setValue(txtNewPrice, contract.getMonthlyRent().longValue());
        panel.add(txtNewPrice, gbc);
        int result = JOptionPane.showConfirmDialog(this, panel, "Gia Hạn Hợp Đồng", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                java.util.Date newEndDate = (java.util.Date) spnNewDate.getValue();
                if (contract.getStartDate() != null && newEndDate.before(contract.getStartDate())) {
                    JOptionPane.showMessageDialog(this, "Ngày kết thúc mới không được nhỏ hơn ngày bắt đầu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Long newPriceVal = MoneyFormatter.getValue(txtNewPrice);
                if (newPriceVal == null || newPriceVal <= 0) {
                    JOptionPane.showMessageDialog(this, "Giá thuê không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                java.math.BigDecimal newPriceBD = java.math.BigDecimal.valueOf(newPriceVal);
                if (contractDAO.renewContract(contract.getId(), newEndDate)) {
                    if (newPriceBD.compareTo(contract.getMonthlyRent()) != 0) {
                        contract.setMonthlyRent(newPriceBD);
                        contractDAO.updateContract(contract);
                    }
                    JOptionPane.showMessageDialog(this, "Gia hạn thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    if (historyPanel != null) {
                        historyPanel.refresh();
                    }
                    this.contract = contractDAO.getContractById(contract.getId());
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Gia hạn thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Dữ liệu nhập không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void terminateContract() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(450, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Ngày trả phòng:"), gbc);
        gbc.gridx = 1;
        JSpinner spnTermDate = new JSpinner(new SpinnerDateModel());
        spnTermDate.setEditor(new JSpinner.DateEditor(spnTermDate, "dd/MM/yyyy"));
        spnTermDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(spnTermDate, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Tiền cọc gốc:"), gbc);
        gbc.gridx = 1;
        JLabel lblDeposit = new JLabel(MoneyFormatter.formatMoney(contract.getDepositAmount()) + " VNĐ");
        lblDeposit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDeposit.setForeground(new Color(33, 150, 243));
        panel.add(lblDeposit, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Hoàn trả khách (VNĐ):"), gbc);
        gbc.gridx = 1;
        JTextField txtRefund = MoneyFormatter.createMoneyField();
        MoneyFormatter.setValue(txtRefund, contract.getDepositAmount().longValue());
        panel.add(txtRefund, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Lý do / Ghi chú:"), gbc);
        gbc.gridx = 1;
        JTextArea txtReason = new JTextArea(3, 20);
        txtReason.setText("Thanh lý hợp đồng");
        txtReason.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtReason.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(txtReason, gbc);
        int result = JOptionPane.showConfirmDialog(this, panel, "Thanh Lý Hợp Đồng", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String reason = txtReason.getText();
            java.util.Date termDate = (java.util.Date) spnTermDate.getValue();
            Long refundAmount = MoneyFormatter.getValue(txtRefund);
            if (contractDAO.terminateContract(contract.getId())) {
                contract.setTerminatedDate(termDate);
                contract.setStatus("TERMINATED");
                String noteAppend = String.format("\n[Thanh lý ngày: %s | Hoàn cọc: %s VNĐ | Lý do: %s]", dateFormat.format(termDate), MoneyFormatter.formatMoney(refundAmount != null ? refundAmount : 0), reason);
                String currentNote = contract.getNotes() == null ? "" : contract.getNotes();
                contract.setNotes(currentNote + noteAppend);
                contractDAO.updateContract(contract);
                JOptionPane.showMessageDialog(this, "Thanh lý hợp đồng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                if (historyPanel != null) {
                    historyPanel.refresh();
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Thanh lý thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
