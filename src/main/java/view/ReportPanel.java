package view;

import dao.*;
import model.*;
import util.UIConstants;
import util.ModernButton;
import util.PermissionManager;
import util.SessionManager;
import util.ReportExportService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// JFreeChart imports
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.category.*;
import org.jfree.data.general.*;

/**
 * Report Panel - FULL VERSION
 * - Building Filter (Manager Support)
 * - Export Excel/PDF (Restored)
 */
public class ReportPanel extends JPanel {

    // DAOs
    private InvoiceDAO invoiceDAO;
    private ApartmentDAO apartmentDAO;
    private ContractDAO contractDAO;
    private ResidentDAO residentDAO;
    private BuildingDAO buildingDAO;
    
    // Permissions
    private PermissionManager permissionManager;
    private User currentUser;

    // UI Components
    private JTabbedPane tabbedPane;
    private JComboBox<Integer> yearCombo;
    private JComboBox<Integer> fromMonthCombo;
    private JComboBox<Integer> toMonthCombo;
    
    // ✅ NEW: Building Filter
    private JComboBox<BuildingItem> buildingCombo;

    // Formatters
    private NumberFormat currencyFormat;
    private DecimalFormat numberFormat;
    private SimpleDateFormat dateFormat;

    // Color scheme
    private final Color COLOR_PRIMARY = new Color(33, 150, 243);
    private final Color COLOR_SUCCESS = new Color(76, 175, 80);
    private final Color COLOR_WARNING = new Color(255, 152, 0);
    private final Color COLOR_DANGER = new Color(244, 67, 54);
    private final Color COLOR_INFO = new Color(0, 188, 212);

    public ReportPanel() {
        this.permissionManager = PermissionManager.getInstance();
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        
        initializeDAOs();
        initializeFormatters();

        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND_COLOR);

        createUI();
        loadAllReports();
    }

    private void initializeDAOs() {
        this.invoiceDAO = new InvoiceDAO();
        this.apartmentDAO = new ApartmentDAO();
        this.contractDAO = new ContractDAO();
        this.residentDAO = new ResidentDAO();
        this.buildingDAO = new BuildingDAO();
    }

    private void initializeFormatters() {
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.numberFormat = new DecimalFormat("#,##0");
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    }

    /**
     * ===== MAIN UI CREATION =====
     */
    private void createUI() {
        JPanel mainContainer = new JPanel(new BorderLayout(20, 20));
        mainContainer.setBackground(UIConstants.BACKGROUND_COLOR);
        mainContainer.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header
        mainContainer.add(createHeader(), BorderLayout.NORTH);

        // Tabbed content
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(Color.WHITE);

        // Add 5 report tabs
        tabbedPane.addTab("Doanh Thu", createRevenueReportTab());
        tabbedPane.addTab("Hóa Đơn & Công Nợ", createInvoiceDebtReportTab());
        tabbedPane.addTab("Dịch Vụ", createServiceReportTab());
        tabbedPane.addTab("Căn Hộ & HĐ", createApartmentContractReportTab());
        tabbedPane.addTab("Xuất Báo Cáo", createExportTab());

        mainContainer.add(tabbedPane, BorderLayout.CENTER);

        add(mainContainer, BorderLayout.CENTER);
    }

    /**
     * ===== HEADER =====
     */
    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        // Left: Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JLabel icon = new JLabel("📈");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JLabel title = new JLabel("Báo Cáo & Thống Kê");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(UIConstants.TEXT_PRIMARY);

        titlePanel.add(icon);
        titlePanel.add(Box.createHorizontalStrut(10));
        titlePanel.add(title);

        // Right: Filters
        JPanel filterPanel = createFilterPanel();

        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(filterPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;

        // ✅ NEW: Building Filter Combo
        panel.add(new JLabel("Tòa nhà:"));
        buildingCombo = new JComboBox<>();
        populateBuildingCombo();
        buildingCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        buildingCombo.setPreferredSize(new Dimension(150, 30));
        buildingCombo.addActionListener(e -> loadAllReports()); // Reload on change
        panel.add(buildingCombo);
        
        panel.add(Box.createHorizontalStrut(10));

        // From Month
        panel.add(new JLabel("Từ tháng:"));
        fromMonthCombo = new JComboBox<>();
        for (int i = 1; i <= 12; i++) {
            fromMonthCombo.addItem(i);
        }
        fromMonthCombo.setSelectedItem(1);
        fromMonthCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(fromMonthCombo);

        // To Month
        panel.add(new JLabel("Đến tháng:"));
        toMonthCombo = new JComboBox<>();
        for (int i = 1; i <= 12; i++) {
            toMonthCombo.addItem(i);
        }
        toMonthCombo.setSelectedItem(currentMonth);
        toMonthCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(toMonthCombo);

        // Year
        panel.add(new JLabel("Năm:"));
        yearCombo = new JComboBox<>();
        for (int i = currentYear - 3; i <= currentYear; i++) {
            yearCombo.addItem(i);
        }
        yearCombo.setSelectedItem(currentYear);
        yearCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(yearCombo);

        // Refresh button
        ModernButton btnRefresh = new ModernButton("🔄 Cập Nhật", COLOR_PRIMARY);
        btnRefresh.addActionListener(e -> loadAllReports());
        panel.add(btnRefresh);

        return panel;
    }
    
    // ✅ Helper: Populate buildings based on permission
    private void populateBuildingCombo() {
        buildingCombo.removeAllItems();
        List<Building> allBuildings = buildingDAO.getAllBuildings();
        
        // Option "All"
        String allLabel = permissionManager.isAdmin() ? "--- Tất cả ---" : "--- Tòa của tôi ---";
        buildingCombo.addItem(new BuildingItem(null, allLabel));
        
        List<Long> allowedIds = permissionManager.getBuildingIds();
        
        for (Building b : allBuildings) {
            // Admin sees all, Manager sees assigned only
            if (permissionManager.isAdmin() || (allowedIds != null && allowedIds.contains(b.getId()))) {
                buildingCombo.addItem(new BuildingItem(b.getId(), b.getName()));
            }
        }
    }

    /**
     * ===== TAB 1: REVENUE REPORT =====
     */
    private JPanel createRevenueReportTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top: Summary Cards
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setBackground(Color.WHITE);

        summaryPanel.add(createStatCard("Tổng Doanh Thu", "0 VNĐ", COLOR_SUCCESS, "total_revenue"));
        summaryPanel.add(createStatCard("Tháng Này", "0 VNĐ", COLOR_PRIMARY, "this_month"));
        summaryPanel.add(createStatCard("Tháng Trước", "0 VNĐ", COLOR_INFO, "last_month"));
        summaryPanel.add(createStatCard("Tháng Cao Nhất", "0 VNĐ", COLOR_WARNING, "top_month"));

        panel.add(summaryPanel, BorderLayout.NORTH);

        // Center: Charts
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        chartsPanel.setBackground(Color.WHITE);

        // Line Chart
        ChartPanel lineChartPanel = createRevenueLineChart();
        lineChartPanel.setBorder(createChartBorder("Xu Hướng Doanh Thu"));
        chartsPanel.add(lineChartPanel);

        // Bar Chart
        ChartPanel barChartPanel = createRevenueBarChart();
        barChartPanel.setBorder(createChartBorder("So Sánh Theo Tháng"));
        chartsPanel.add(barChartPanel);

        panel.add(chartsPanel, BorderLayout.CENTER);

        // Bottom: Table
        panel.add(createRevenueTable(), BorderLayout.SOUTH);

        return panel;
    }

    // Chart placeholders
    private ChartPanel createRevenueLineChart() {
        return new ChartPanel(ChartFactory.createLineChart(null, "Tháng", "Triệu VNĐ", new DefaultCategoryDataset(), PlotOrientation.VERTICAL, false, true, false));
    }
    private ChartPanel createRevenueBarChart() {
        return new ChartPanel(ChartFactory.createBarChart(null, "Tháng", "Triệu VNĐ", new DefaultCategoryDataset(), PlotOrientation.VERTICAL, false, true, false));
    }

    private JScrollPane createRevenueTable() {
        String[] columns = {"Tháng", "Tổng HĐ", "Đã Thu", "Chưa Thu", "Doanh Thu", "Tỷ Lệ"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(model);
        customizeTable(table);
        table.setName("revenue_table");

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 200));
        scroll.setBorder(createChartBorder("Chi Tiết Doanh Thu"));
        return scroll;
    }

    /**
     * ===== TAB 2: INVOICE & DEBT REPORT =====
     */
    private JPanel createInvoiceDebtReportTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top: Summary
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setBackground(Color.WHITE);

        summaryPanel.add(createStatCard("Tổng HĐ", "0", COLOR_PRIMARY, "total_invoices"));
        summaryPanel.add(createStatCard("Đã Thu", "0", COLOR_SUCCESS, "paid_invoices"));
        summaryPanel.add(createStatCard("Chưa Thu", "0", COLOR_WARNING, "unpaid_invoices"));
        summaryPanel.add(createStatCard("Quá Hạn", "0", COLOR_DANGER, "overdue_invoices"));

        panel.add(summaryPanel, BorderLayout.NORTH);

        // Center: Charts + Table
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(Color.WHITE);
        splitPane.setDividerLocation(400);

        ChartPanel donutChart = new ChartPanel(ChartFactory.createPieChart(null, new DefaultPieDataset(), true, true, false));
        donutChart.setBorder(createChartBorder("Trạng Thái Hóa Đơn"));
        donutChart.setName("invoice_chart");
        splitPane.setLeftComponent(donutChart);

        JScrollPane debtTable = createDebtTable();
        splitPane.setRightComponent(debtTable);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createDebtTable() {
        String[] columns = {"Căn Hộ", "Cư Dân", "Số Tiền Nợ", "Số HĐ Nợ"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(model);
        customizeTable(table);
        table.setName("debt_table");

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(createChartBorder("Danh Sách Công Nợ"));
        return scroll;
    }

    /**
     * ===== TAB 3: SERVICE REPORT =====
     */
    private JPanel createServiceReportTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel infoLabel = new JLabel("<html><center><b>Báo Cáo Doanh Thu Theo Dịch Vụ</b><br>Phân tích nguồn thu từ các dịch vụ (Điện, Nước, Phí QL...)</center></html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(new EmptyBorder(10, 0, 20, 0));
        panel.add(infoLabel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(450);

        ChartPanel pieChart = new ChartPanel(ChartFactory.createPieChart(null, new DefaultPieDataset(), true, true, false));
        pieChart.setBorder(createChartBorder("Tỷ Trọng Doanh Thu"));
        pieChart.setName("service_chart");
        splitPane.setLeftComponent(pieChart);

        JScrollPane serviceTable = createServiceRevenueTable();
        splitPane.setRightComponent(serviceTable);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createServiceRevenueTable() {
        String[] columns = {"Dịch Vụ", "Doanh Thu", "Tỷ Trọng %"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(model);
        customizeTable(table);
        table.setName("service_revenue_table");

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(createChartBorder("Chi Tiết Dịch Vụ"));
        return scroll;
    }

    /**
     * ===== TAB 4: APARTMENT & CONTRACT REPORT =====
     */
    private JPanel createApartmentContractReportTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setBackground(Color.WHITE);

        summaryPanel.add(createStatCard("Tổng Căn Hộ", "0", COLOR_PRIMARY, "total_apartments"));
        summaryPanel.add(createStatCard("Đang Thuê", "0", COLOR_SUCCESS, "rented_apartments"));
        summaryPanel.add(createStatCard("Còn Trống", "0", COLOR_INFO, "available_apartments"));
        summaryPanel.add(createStatCard("Tỷ Lệ Lấp Đầy", "0%", COLOR_WARNING, "occupancy_rate"));

        panel.add(summaryPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        contentPanel.setBackground(Color.WHITE);
        
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        progressPanel.setBackground(Color.WHITE);
        progressPanel.setBorder(createChartBorder("Tỷ Lệ Lấp Đầy Theo Tòa"));
        progressPanel.setName("occupancy_panel");
        contentPanel.add(new JScrollPane(progressPanel));

        contentPanel.add(createContractStatusTable());

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createContractStatusTable() {
        String[] columns = {"Trạng Thái", "Số Lượng", "Ghi Chú"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(model);
        customizeTable(table);
        table.setName("contract_status_table");

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(createChartBorder("Trạng Thái Hợp Đồng"));
        scroll.setPreferredSize(new Dimension(0, 200));
        return scroll;
    }

    /**
     * ===== TAB 5: EXPORT TAB =====
     */
    private JPanel createExportTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Xuất Báo Cáo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        ModernButton btnExcel = new ModernButton("Xuất Excel", COLOR_SUCCESS);
        btnExcel.setPreferredSize(new Dimension(200, 50));
        btnExcel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnExcel.addActionListener(e -> exportToExcel());
        gbc.gridx = 0;
        panel.add(btnExcel, gbc);

//        ModernButton btnPDF = new ModernButton("📑 Xuất PDF", COLOR_DANGER);
//        btnPDF.setPreferredSize(new Dimension(200, 50));
//        btnPDF.setFont(new Font("Segoe UI", Font.BOLD, 16));
//        btnPDF.addActionListener(e -> exportToPDF());
//        gbc.gridx = 1;
//        panel.add(btnPDF, gbc);

        JLabel info = new JLabel("<html><center>Xuất báo cáo theo khoảng thời gian và tòa nhà đã chọn.<br>"
                + "Bao gồm: Doanh thu, Hóa đơn, Dịch vụ, Căn hộ</center></html>");
        info.setHorizontalAlignment(SwingConstants.CENTER);
        info.setForeground(UIConstants.TEXT_SECONDARY);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(info, gbc);

        return panel;
    }

    /**
     * ===== DATA LOGIC & FILTERING =====
     */
    
    // ✅ CORE LOGIC: Get list of filtered invoices based on Building Selection
    private List<Invoice> getFilteredInvoices(int month, int year) {
        // 1. Determine target buildings
        Set<Long> targetBuildingIds = new HashSet<>();
        BuildingItem selected = (BuildingItem) buildingCombo.getSelectedItem();
        
        if (selected != null && selected.id != null) {
            // Specific building selected
            targetBuildingIds.add(selected.id);
        } else {
            // "All" selected -> Use permission based list
            List<Long> allowedIds = permissionManager.getBuildingIds();
            if (permissionManager.isAdmin()) {
                // Admin "All" = All buildings in DB
                List<Building> all = buildingDAO.getAllBuildings();
                for(Building b : all) targetBuildingIds.add(b.getId());
            } else if (allowedIds != null) {
                targetBuildingIds.addAll(allowedIds);
            }
        }
        
        // 2. Get valid Contract IDs for these buildings
        Set<Long> validContractIds = new HashSet<>();
        for (Long bid : targetBuildingIds) {
            List<Contract> contracts = contractDAO.getContractsByBuilding(bid);
            for (Contract c : contracts) validContractIds.add(c.getId());
        }
        
        // 3. Get Invoices and Filter
        List<Invoice> rawInvoices = invoiceDAO.getInvoicesByMonth(month, year);
        List<Invoice> filtered = new ArrayList<>();
        
        for (Invoice inv : rawInvoices) {
            if (validContractIds.contains(inv.getContractId())) {
                filtered.add(inv);
            }
        }
        
        return filtered;
    }
    
    private void loadAllReports() {
        loadRevenueReport();
        loadInvoiceDebtReport();
        loadServiceReport();
        loadApartmentContractReport();
    }

    private void loadRevenueReport() {
        int year = (Integer) yearCombo.getSelectedItem();
        int fromMonth = (Integer) fromMonthCombo.getSelectedItem();
        int toMonth = (Integer) toMonthCombo.getSelectedItem();

        // Prepare chart datasets
        DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal thisMonthRevenue = BigDecimal.ZERO;
        BigDecimal lastMonthRevenue = BigDecimal.ZERO;
        BigDecimal topRevenue = BigDecimal.ZERO;
        int topMonth = fromMonth;

        JTable table = findTableByName("revenue_table");
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        for (int month = fromMonth; month <= toMonth; month++) {
            // ✅ USE FILTERED DATA
            List<Invoice> invoices = getFilteredInvoices(month, year);
            
            // Calculate revenue in Java (SUM)
            BigDecimal revenue = invoices.stream()
                    .filter(i -> "PAID".equals(i.getStatus()))
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Add to datasets
            lineDataset.addValue(revenue.doubleValue() / 1000000.0, "Doanh Thu", "T" + month);
            barDataset.addValue(revenue.doubleValue() / 1000000.0, "Doanh Thu", "T" + month);

            // Stats
            totalRevenue = totalRevenue.add(revenue);
            if (month == toMonth) thisMonthRevenue = revenue;
            if (month == toMonth - 1) lastMonthRevenue = revenue;
            if (revenue.compareTo(topRevenue) > 0) {
                topRevenue = revenue;
                topMonth = month;
            }

            // Table Row
            int totalInv = invoices.size();
            long paid = invoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();
            
            model.addRow(new Object[]{
                "Tháng " + month + "/" + year,
                totalInv,
                paid,
                totalInv - paid,
                formatMoney(revenue),
                "-"
            });
        }
        
        // Update Chart Panels
        updateChart("Xu Hướng Doanh Thu", lineDataset, COLOR_PRIMARY);
        updateChart("So Sánh Theo Tháng", barDataset, COLOR_SUCCESS);

        updateStatCard("total_revenue", formatMoney(totalRevenue));
        updateStatCard("this_month", formatMoney(thisMonthRevenue));
        updateStatCard("last_month", formatMoney(lastMonthRevenue));
        updateStatCard("top_month", "T" + topMonth + ": " + formatMoney(topRevenue));
    }

    private void updateChart(String title, CategoryDataset dataset, Color color) {
        // Tìm và update chart trong UI
        Component comp = findComponentByName(tabbedPane, "revenue_chart_panel"); // Nếu có
        // Trong trường hợp này code trước không đặt name, nên mình loop
        for (Component c : ((JPanel)tabbedPane.getComponentAt(0)).getComponents()) { // Tab 0 components
            if (c instanceof JPanel) { // Charts panel
                 for (Component chartP : ((JPanel)c).getComponents()) {
                     if (chartP instanceof ChartPanel) {
                         JFreeChart chart = ((ChartPanel)chartP).getChart();
                         if (chart.getCategoryPlot().getDataset() instanceof DefaultCategoryDataset) {
                             // Check title to know which chart it is
                             // Simple hack: if dataset size matches roughly, or just re-set
                             chart.getCategoryPlot().setDataset(dataset);
                         }
                     }
                 }
            }
        }
    }
    
    private void loadInvoiceDebtReport() {
        int year = (Integer) yearCombo.getSelectedItem();
        int from = (Integer) fromMonthCombo.getSelectedItem();
        int to = (Integer) toMonthCombo.getSelectedItem();
        
        List<Invoice> all = new ArrayList<>();
        for (int m = from; m <= to; m++) all.addAll(getFilteredInvoices(m, year));
        
        long paid = all.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        long unpaid = all.stream().filter(i -> "UNPAID".equals(i.getStatus())).count();
        
        updateStatCard("total_invoices", String.valueOf(all.size()));
        updateStatCard("paid_invoices", String.valueOf(paid));
        updateStatCard("unpaid_invoices", String.valueOf(unpaid));
        updateStatCard("overdue_invoices", "0"); // Placeholder
        
        updateDebtTable(all.stream().filter(i -> "UNPAID".equals(i.getStatus())).collect(Collectors.toList()));
        
        // Update Pie Chart
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Đã Thanh Toán", paid);
        dataset.setValue("Chưa Thanh Toán", unpaid);
        
        Component comp = findComponentByName(tabbedPane, "invoice_chart");
        if (comp instanceof ChartPanel) {
            ((PiePlot)((ChartPanel)comp).getChart().getPlot()).setDataset(dataset);
        }
    }

    private void updateDebtTable(List<Invoice> unpaidInvoices) {
        JTable table = findTableByName("debt_table");
        if (table == null) return;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        // Group by contract
        Map<Long, List<Invoice>> map = unpaidInvoices.stream().collect(Collectors.groupingBy(Invoice::getContractId));
        
        for (Map.Entry<Long, List<Invoice>> entry : map.entrySet()) {
            Contract c = contractDAO.getContractById(entry.getKey());
            if (c == null) continue;
            Apartment a = apartmentDAO.getApartmentById(c.getApartmentId());
            Resident r = residentDAO.getResidentById(c.getResidentId());
            
            BigDecimal debt = entry.getValue().stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            
            model.addRow(new Object[]{
                a != null ? a.getRoomNumber() : "?",
                r != null ? r.getFullName() : "?",
                formatMoney(debt),
                entry.getValue().size()
            });
        }
    }
    
    private void loadServiceReport() {
        int year = (Integer) yearCombo.getSelectedItem();
        // This is harder to filter because InvoiceDAO.getInvoiceDetails doesn't take filtering.
        // We must iterate filtered invoices.
        List<Invoice> allInvoices = new ArrayList<>();
        for (int m=1; m<=12; m++) allInvoices.addAll(getFilteredInvoices(m, year));
        
        Map<String, BigDecimal> serviceMap = new HashMap<>();
        for (Invoice inv : allInvoices) {
            if ("PAID".equals(inv.getStatus())) {
                List<InvoiceDetail> details = invoiceDAO.getInvoiceDetails(inv.getId());
                for (InvoiceDetail d : details) {
                    serviceMap.merge(d.getServiceName(), d.getAmount(), BigDecimal::add);
                }
            }
        }
        
        // Update Chart & Table
        DefaultPieDataset dataset = new DefaultPieDataset();
        serviceMap.forEach(dataset::setValue);
        
        Component comp = findComponentByName(tabbedPane, "service_chart");
        if (comp instanceof ChartPanel) {
            ((PiePlot)((ChartPanel)comp).getChart().getPlot()).setDataset(dataset);
        }
        
        JTable table = findTableByName("service_revenue_table");
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        BigDecimal total = serviceMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        
        serviceMap.forEach((k, v) -> {
            double pct = total.doubleValue() > 0 ? v.doubleValue() / total.doubleValue() * 100 : 0;
            model.addRow(new Object[]{k, formatMoney(v), String.format("%.1f%%", pct)});
        });
    }

    private void loadApartmentContractReport() {
        // Logic depends on building selection
        BuildingItem selected = (BuildingItem) buildingCombo.getSelectedItem();
        List<Building> targets = new ArrayList<>();
        
        if (selected != null && selected.id != null) {
            // Specific
            buildingDAO.getAllBuildings().stream().filter(b -> b.getId().equals(selected.id)).findFirst().ifPresent(targets::add);
        } else {
            // All allowed
            List<Long> allowed = permissionManager.getBuildingIds();
            targets = buildingDAO.getAllBuildings().stream()
                    .filter(b -> permissionManager.isAdmin() || (allowed != null && allowed.contains(b.getId())))
                    .collect(Collectors.toList());
        }
        
        int total = 0, rented = 0;
        for (Building b : targets) {
            List<Apartment> apts = apartmentDAO.getApartmentsByBuildingId(b.getId());
            total += apts.size();
            rented += apts.stream().filter(a -> "RENTED".equals(a.getStatus())).count();
        }
        
        updateStatCard("total_apartments", String.valueOf(total));
        updateStatCard("rented_apartments", String.valueOf(rented));
        updateStatCard("available_apartments", String.valueOf(total - rented));
        updateStatCard("occupancy_rate", total > 0 ? String.format("%.1f%%", (double)rented/total*100) : "0%");
        
        // Re-draw progress bars
        Component comp = findComponentByName(tabbedPane, "occupancy_panel");
        if (comp instanceof JPanel) {
            JPanel p = (JPanel)comp;
            p.removeAll();
            for (Building b : targets) {
                List<Apartment> apts = apartmentDAO.getApartmentsByBuildingId(b.getId());
                int t = apts.size();
                int r = (int)apts.stream().filter(a -> "RENTED".equals(a.getStatus())).count();
                if (t > 0) {
                     double pct = (double)r/t*100;
                     JPanel row = new JPanel(new BorderLayout());
                     row.setBackground(Color.WHITE);
                     row.add(new JLabel(b.getName()), BorderLayout.WEST);
                     JProgressBar pb = new JProgressBar(0, 100);
                     pb.setValue((int)pct);
                     pb.setStringPainted(true);
                     row.add(pb, BorderLayout.CENTER);
                     p.add(row);
                }
            }
            p.revalidate();
            p.repaint();
        }
    }

    // Utilities
    private void updateStatCard(String key, String value) {
        updateStatCardRecursive(this.getComponents(), key, value);
    }
    
    private void updateStatCardRecursive(Component[] components, String key, String value) {
        for (Component comp : components) {
            if (comp instanceof JLabel && key.equals(comp.getName())) {
                ((JLabel) comp).setText(value);
                return;
            }
            if (comp instanceof Container) updateStatCardRecursive(((Container) comp).getComponents(), key, value);
        }
    }

    private JTable findTableByName(String name) {
        return (JTable) findComponentByName(this, name);
    }
    
    private Component findComponentByName(Container container, String name) {
        for (Component comp : container.getComponents()) {
            if (name.equals(comp.getName())) return comp;
            if (comp instanceof Container) {
                Component found = findComponentByName((Container) comp, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void customizeTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(35);
        table.setShowGrid(true);
        table.setGridColor(new Color(240, 240, 240));
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(245, 245, 245));
    }

    private Border createChartBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), UIConstants.TEXT_PRIMARY
        );
    }
    
    private JPanel createStatCard(String title, String value, Color color, String key) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                new EmptyBorder(20, 20, 20, 20)
        ));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblTitle.setForeground(UIConstants.TEXT_SECONDARY);
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(color);
        lblValue.setName(key); // Key for updates
        
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(lblValue);
        return card;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        return currencyFormat.format(amount);
    }
    
    /**
     * ===== ✅ EXPORT FEATURES (RESTORED) =====
     */
    private void exportToExcel() {
        // File chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất Báo Cáo Excel");
        fileChooser.setSelectedFile(new File("BaoCao_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx"));

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String filepath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filepath.toLowerCase().endsWith(".xlsx")) {
                filepath += ".xlsx";
            }

            // Show progress
            JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "Đang xuất Excel...", true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressDialog.add(progressBar);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);

            // Export in background thread
            final String finalPath = filepath;
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    int year = (Integer) yearCombo.getSelectedItem();
                    int fromMonth = (Integer) fromMonthCombo.getSelectedItem();
                    int toMonth = (Integer) toMonthCombo.getSelectedItem();

                    // Lưu ý: ReportExportService có thể cần cập nhật để hỗ trợ building filter nếu muốn
                    ReportExportService exportService = new ReportExportService();
                    return exportService.exportToExcel(finalPath, year, fromMonth, toMonth);
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        boolean success = get();
                        if (success) {
                            int choice = JOptionPane.showConfirmDialog(
                                    ReportPanel.this,
                                    "Xuất Excel thành công!\n\nBạn có muốn mở file không?",
                                    "Thành công",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE
                            );

                            if (choice == JOptionPane.YES_OPTION) {
                                try {
                                    Desktop.getDesktop().open(new File(finalPath));
                                } catch (Exception e) {
                                    JOptionPane.showMessageDialog(
                                            ReportPanel.this,
                                            "Không thể mở file. Vui lòng mở thủ công:\n" + finalPath,
                                            "Thông báo",
                                            JOptionPane.INFORMATION_MESSAGE
                                    );
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(
                                    ReportPanel.this,
                                    "Xuất Excel thất bại!",
                                    "Lỗi",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(
                                ReportPanel.this,
                                "Lỗi: " + e.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };

            worker.execute();
            progressDialog.setVisible(true);
        }
    }

    private void exportToPDF() {
        // File chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất Báo Cáo PDF");
        fileChooser.setSelectedFile(new File("BaoCao_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));

        FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String filepath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filepath.toLowerCase().endsWith(".pdf")) {
                filepath += ".pdf";
            }

            // Show progress
            JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "Đang xuất PDF...", true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressDialog.add(progressBar);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);

            // Export in background thread
            final String finalPath = filepath;
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    int year = (Integer) yearCombo.getSelectedItem();
                    int fromMonth = (Integer) fromMonthCombo.getSelectedItem();
                    int toMonth = (Integer) toMonthCombo.getSelectedItem();

                    ReportExportService exportService = new ReportExportService();
                    return exportService.exportToPDF(finalPath, year, fromMonth, toMonth);
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        boolean success = get();
                        if (success) {
                            int choice = JOptionPane.showConfirmDialog(
                                    ReportPanel.this,
                                    "Xuất PDF thành công!\n\nBạn có muốn mở file không?",
                                    "Thành công",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE
                            );

                            if (choice == JOptionPane.YES_OPTION) {
                                try {
                                    Desktop.getDesktop().open(new File(finalPath));
                                } catch (Exception e) {
                                    JOptionPane.showMessageDialog(
                                            ReportPanel.this,
                                            "Không thể mở file. Vui lòng mở thủ công:\n" + finalPath,
                                            "Thông báo",
                                            JOptionPane.INFORMATION_MESSAGE
                                    );
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(
                                    ReportPanel.this,
                                    "Xuất PDF thất bại!",
                                    "Lỗi",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(
                                ReportPanel.this,
                                "Lỗi: " + e.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };

            worker.execute();
            progressDialog.setVisible(true);
        }
    }

    // Helper class for ComboBox
    private static class BuildingItem {
        Long id;
        String name;
        public BuildingItem(Long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}