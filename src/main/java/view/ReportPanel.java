package view;

import dao.*;
import model.*;
import util.UIConstants;
import util.ModernButton;
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

// JFreeChart imports
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.category.*;
import org.jfree.data.general.*;
import org.jfree.chart.title.TextTitle;

/**
 * Enhanced Report Panel with Professional Charts ✨ 5 Comprehensive Reports: 1.
 * Revenue Report (Line + Bar Charts) 2. Invoice & Debt Report (Donut + Bar
 * Charts) 3. Service Revenue Report (Pie + Bar Charts) 4. Apartment & Contract
 * Report (Progress + Gauge) 5. Export to Excel & PDF
 */
public class ReportPanel extends JPanel {

    // DAOs
    private InvoiceDAO invoiceDAO;
    private ApartmentDAO apartmentDAO;
    private ContractDAO contractDAO;
    private ResidentDAO residentDAO;
    private BuildingDAO buildingDAO;

    // UI Components
    private JTabbedPane tabbedPane;
    private JComboBox<Integer> yearCombo;
    private JComboBox<Integer> fromMonthCombo;
    private JComboBox<Integer> toMonthCombo;

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

    private ChartPanel createRevenueLineChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        int year = (Integer) yearCombo.getSelectedItem();
        int fromMonth = (Integer) fromMonthCombo.getSelectedItem();
        int toMonth = (Integer) toMonthCombo.getSelectedItem();

        for (int month = fromMonth; month <= toMonth; month++) {
            BigDecimal revenue = invoiceDAO.getMonthlyRevenue(month, year);
            dataset.addValue(revenue.doubleValue() / 1000000.0, "Doanh Thu", "T" + month);
        }

        JFreeChart chart = ChartFactory.createLineChart(
                null,
                "Tháng",
                "Doanh Thu (Triệu VNĐ)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        customizeChart(chart, COLOR_PRIMARY);

        return new ChartPanel(chart);
    }

    private ChartPanel createRevenueBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        int year = (Integer) yearCombo.getSelectedItem();
        int fromMonth = (Integer) fromMonthCombo.getSelectedItem();
        int toMonth = (Integer) toMonthCombo.getSelectedItem();

        for (int month = fromMonth; month <= toMonth; month++) {
            BigDecimal revenue = invoiceDAO.getMonthlyRevenue(month, year);
            dataset.addValue(revenue.doubleValue() / 1000000.0, "Doanh Thu", "T" + month);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null,
                "Tháng",
                "Doanh Thu (Triệu VNĐ)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        customizeChart(chart, COLOR_SUCCESS);

        return new ChartPanel(chart);
    }

    private JScrollPane createRevenueTable() {
        String[] columns = {"Tháng", "Tổng HĐ", "Đã Thu", "Chưa Thu", "Doanh Thu", "Tỷ Lệ"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        customizeTable(table);

        // Store model for update
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

        // Left: Donut Chart
        ChartPanel donutChart = createInvoiceStatusDonutChart();
        donutChart.setBorder(createChartBorder("Trạng Thái Hóa Đơn"));
        splitPane.setLeftComponent(donutChart);

        // Right: Debt Table
        JScrollPane debtTable = createDebtTable();
        splitPane.setRightComponent(debtTable);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private ChartPanel createInvoiceStatusDonutChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        int year = (Integer) yearCombo.getSelectedItem();
        List<Invoice> allInvoices = new ArrayList<>();

        int fromMonth = (Integer) fromMonthCombo.getSelectedItem();
        int toMonth = (Integer) toMonthCombo.getSelectedItem();

        for (int month = fromMonth; month <= toMonth; month++) {
            allInvoices.addAll(invoiceDAO.getInvoicesByMonth(month, year));
        }

        long paid = allInvoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        long unpaid = allInvoices.stream().filter(i -> "UNPAID".equals(i.getStatus())).count();

        dataset.setValue("Đã Thanh Toán", paid);
        dataset.setValue("Chưa Thanh Toán", unpaid);

        JFreeChart chart = ChartFactory.createPieChart(
                null,
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("Đã Thanh Toán", COLOR_SUCCESS);
        plot.setSectionPaint("Chưa Thanh Toán", COLOR_WARNING);
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(Color.WHITE);

        return new ChartPanel(chart);
    }

    private JScrollPane createDebtTable() {
        String[] columns = {"Căn Hộ", "Cư Dân", "Số Tiền Nợ", "Số HĐ Nợ"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
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

        // Top: Message
        JLabel infoLabel = new JLabel(
                "<html><center><b>Báo Cáo Doanh Thu Theo Dịch Vụ</b><br>"
                + "Phân tích nguồn thu từ các dịch vụ (Điện, Nước, Phí QL, Gửi Xe...)</center></html>"
        );
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(new EmptyBorder(10, 0, 20, 0));
        panel.add(infoLabel, BorderLayout.NORTH);

        // Center: Pie Chart + Table
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(450);

        ChartPanel pieChart = createServiceRevenuePieChart();
        pieChart.setBorder(createChartBorder("Tỷ Trọng Doanh Thu"));
        splitPane.setLeftComponent(pieChart);

        JScrollPane serviceTable = createServiceRevenueTable();
        splitPane.setRightComponent(serviceTable);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private ChartPanel createServiceRevenuePieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        // Lấy dữ liệu từ invoice_details
        int year = (Integer) yearCombo.getSelectedItem();
        Map<String, BigDecimal> serviceRevenue = getServiceRevenue(year);

        for (Map.Entry<String, BigDecimal> entry : serviceRevenue.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        JFreeChart chart = ChartFactory.createPieChart(
                null,
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(Color.WHITE);

        return new ChartPanel(chart);
    }

    private JScrollPane createServiceRevenueTable() {
        String[] columns = {"Dịch Vụ", "Doanh Thu", "Tỷ Trọng %"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
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

        // Top: Summary Cards
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setBackground(Color.WHITE);

        summaryPanel.add(createStatCard("Tổng Căn Hộ", "0", COLOR_PRIMARY, "total_apartments"));
        summaryPanel.add(createStatCard("Đang Thuê", "0", COLOR_SUCCESS, "rented_apartments"));
        summaryPanel.add(createStatCard("Còn Trống", "0", COLOR_INFO, "available_apartments"));
        summaryPanel.add(createStatCard("Tỷ Lệ Lấp Đầy", "0%", COLOR_WARNING, "occupancy_rate"));

        panel.add(summaryPanel, BorderLayout.NORTH);

        // Center: Progress Bar + Table
        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        contentPanel.setBackground(Color.WHITE);

        contentPanel.add(createOccupancyProgressPanel());
        contentPanel.add(createContractStatusTable());

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createOccupancyProgressPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(createChartBorder("Tỷ Lệ Lấp Đầy Theo Tòa"));

        List<Building> buildings = buildingDAO.getAllBuildings();

        for (Building building : buildings) {
            List<Apartment> apartments = apartmentDAO.getApartmentsByBuildingId(building.getId());
            int total = apartments.size();
            int rented = (int) apartments.stream().filter(a -> "RENTED".equals(a.getStatus())).count();

            if (total > 0) {
                double percent = (rented * 100.0) / total;

                JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
                rowPanel.setBackground(Color.WHITE);
                rowPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
                rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

                JLabel nameLabel = new JLabel(building.getName());
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                nameLabel.setPreferredSize(new Dimension(150, 30));

                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setValue((int) percent);
                progressBar.setStringPainted(true);
                progressBar.setString(String.format("%d/%d (%.1f%%)", rented, total, percent));
                progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
                progressBar.setForeground(percent > 80 ? COLOR_SUCCESS : (percent > 50 ? COLOR_WARNING : COLOR_DANGER));

                rowPanel.add(nameLabel, BorderLayout.WEST);
                rowPanel.add(progressBar, BorderLayout.CENTER);

                panel.add(rowPanel);
            }
        }

        return panel;
    }

    private JScrollPane createContractStatusTable() {
        String[] columns = {"Trạng Thái", "Số Lượng", "Ghi Chú"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
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
     * ===== TAB 5: EXPORT =====
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
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;

        // Excel Export
        ModernButton btnExportExcel = new ModernButton("Xuất Excel", COLOR_SUCCESS);
        btnExportExcel.setPreferredSize(new Dimension(200, 50));
        btnExportExcel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnExportExcel.addActionListener(e -> exportToExcel());
        gbc.gridx = 0;
        panel.add(btnExportExcel, gbc);

        // PDF Export
        ModernButton btnExportPDF = new ModernButton("📑 Xuất PDF", COLOR_DANGER);
        btnExportPDF.setPreferredSize(new Dimension(200, 50));
        btnExportPDF.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnExportPDF.addActionListener(e -> exportToPDF());
        gbc.gridx = 1;
        panel.add(btnExportPDF, gbc);

        // Info
        JLabel info = new JLabel("<html><center>Xuất báo cáo theo khoảng thời gian đã chọn<br>"
                + "Bao gồm: Doanh thu, Hóa đơn, Dịch vụ, Căn hộ</center></html>");
        info.setHorizontalAlignment(SwingConstants.CENTER);
        info.setForeground(UIConstants.TEXT_SECONDARY);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(info, gbc);

        return panel;
    }

    /**
     * ===== HELPER METHODS =====
     */
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
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(color);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblValue.setName(key); // For update

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(lblValue);

        return card;
    }

    private void customizeChart(JFreeChart chart, Color color) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.getPlot().setOutlineVisible(false);

        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setRangeGridlinePaint(new Color(230, 230, 230));
            plot.getRenderer().setSeriesPaint(0, color);
        }
    }

    private void customizeTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(35);
        table.setShowGrid(true);
        table.setGridColor(new Color(240, 240, 240));
        table.setSelectionBackground(new Color(232, 245, 255));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(new Color(66, 66, 66));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
    }

    private Border createChartBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                UIConstants.TEXT_PRIMARY
        );
    }

    /**
     * ===== DATA LOADING =====
     */
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

        // Calculate stats
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal thisMonthRevenue = invoiceDAO.getMonthlyRevenue(toMonth, year);
        BigDecimal lastMonthRevenue = toMonth > 1
                ? invoiceDAO.getMonthlyRevenue(toMonth - 1, year) : BigDecimal.ZERO;

        BigDecimal topMonthRevenue = BigDecimal.ZERO;
        int topMonth = fromMonth;

        for (int month = fromMonth; month <= toMonth; month++) {
            BigDecimal revenue = invoiceDAO.getMonthlyRevenue(month, year);
            totalRevenue = totalRevenue.add(revenue);

            if (revenue.compareTo(topMonthRevenue) > 0) {
                topMonthRevenue = revenue;
                topMonth = month;
            }
        }

        // Update cards
        updateStatCard("total_revenue", formatMoney(totalRevenue));
        updateStatCard("this_month", formatMoney(thisMonthRevenue));
        updateStatCard("last_month", formatMoney(lastMonthRevenue));
        updateStatCard("top_month", "T" + topMonth + ": " + formatMoney(topMonthRevenue));

        // Update table
        updateRevenueTable(year, fromMonth, toMonth, totalRevenue);
    }

    private void updateRevenueTable(int year, int fromMonth, int toMonth, BigDecimal totalRevenue) {
        JTable table = findTableByName("revenue_table");
        if (table == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        for (int month = fromMonth; month <= toMonth; month++) {
            List<Invoice> invoices = invoiceDAO.getInvoicesByMonth(month, year);

            int totalInvoices = invoices.size();
            long paidCount = invoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();
            long unpaidCount = totalInvoices - paidCount;

            BigDecimal revenue = invoiceDAO.getMonthlyRevenue(month, year);
            double percent = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? (revenue.doubleValue() / totalRevenue.doubleValue() * 100) : 0;

            model.addRow(new Object[]{
                "Tháng " + month + "/" + year,
                totalInvoices,
                paidCount,
                unpaidCount,
                formatMoney(revenue),
                String.format("%.1f%%", percent)
            });
        }
    }

    private void loadInvoiceDebtReport() {
        int year = (Integer) yearCombo.getSelectedItem();
        int fromMonth = (Integer) fromMonthCombo.getSelectedItem();
        int toMonth = (Integer) toMonthCombo.getSelectedItem();

        List<Invoice> allInvoices = new ArrayList<>();
        for (int month = fromMonth; month <= toMonth; month++) {
            allInvoices.addAll(invoiceDAO.getInvoicesByMonth(month, year));
        }

        long paid = allInvoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        long unpaid = allInvoices.stream().filter(i -> "UNPAID".equals(i.getStatus())).count();

        updateStatCard("total_invoices", String.valueOf(allInvoices.size()));
        updateStatCard("paid_invoices", String.valueOf(paid));
        updateStatCard("unpaid_invoices", String.valueOf(unpaid));
        updateStatCard("overdue_invoices", "0"); // TODO: Calculate overdue

        updateDebtTable(allInvoices);
    }

    private void updateDebtTable(List<Invoice> unpaidInvoices) {
        JTable table = findTableByName("debt_table");
        if (table == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        // Group by contract
        Map<Long, List<Invoice>> debtByContract = new HashMap<>();
        for (Invoice invoice : unpaidInvoices) {
            if ("UNPAID".equals(invoice.getStatus())) {
                debtByContract.computeIfAbsent(invoice.getContractId(), k -> new ArrayList<>()).add(invoice);
            }
        }

        // Add to table
        for (Map.Entry<Long, List<Invoice>> entry : debtByContract.entrySet()) {
            Contract contract = contractDAO.getContractById(entry.getKey());
            if (contract == null) {
                continue;
            }

            Apartment apt = apartmentDAO.getApartmentById(contract.getApartmentId());
            Resident res = residentDAO.getResidentById(contract.getResidentId());

            BigDecimal totalDebt = entry.getValue().stream()
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addRow(new Object[]{
                apt != null ? apt.getRoomNumber() : "N/A",
                res != null ? res.getFullName() : "N/A",
                formatMoney(totalDebt),
                entry.getValue().size()
            });
        }
    }

    private void loadServiceReport() {
        int year = (Integer) yearCombo.getSelectedItem();
        Map<String, BigDecimal> serviceRevenue = getServiceRevenue(year);

        BigDecimal total = serviceRevenue.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JTable table = findTableByName("service_revenue_table");
        if (table == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        for (Map.Entry<String, BigDecimal> entry : serviceRevenue.entrySet()) {
            double percent = total.compareTo(BigDecimal.ZERO) > 0
                    ? (entry.getValue().doubleValue() / total.doubleValue() * 100) : 0;

            model.addRow(new Object[]{
                entry.getKey(),
                formatMoney(entry.getValue()),
                String.format("%.1f%%", percent)
            });
        }
    }

    private Map<String, BigDecimal> getServiceRevenue(int year) {
        Map<String, BigDecimal> result = new HashMap<>();

        // Get all invoices of year
        List<Invoice> invoices = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            invoices.addAll(invoiceDAO.getInvoicesByMonth(month, year));
        }

        // Get invoice details and group by service
        for (Invoice invoice : invoices) {
            if (!"PAID".equals(invoice.getStatus())) {
                continue;
            }

            List<InvoiceDetail> details = invoiceDAO.getInvoiceDetails(invoice.getId());
            for (InvoiceDetail detail : details) {
                String serviceName = detail.getServiceName();
                BigDecimal amount = detail.getAmount();

                result.merge(serviceName, amount, BigDecimal::add);
            }
        }

        return result;
    }

    private void loadApartmentContractReport() {
        int totalApt = apartmentDAO.countApartments();
        int rented = apartmentDAO.countRentedApartments();
        int available = apartmentDAO.countAvailableApartments();
        double rate = totalApt > 0 ? (rented * 100.0 / totalApt) : 0;

        updateStatCard("total_apartments", String.valueOf(totalApt));
        updateStatCard("rented_apartments", String.valueOf(rented));
        updateStatCard("available_apartments", String.valueOf(available));
        updateStatCard("occupancy_rate", String.format("%.1f%%", rate));

        updateContractStatusTable();
    }

    private void updateContractStatusTable() {
        JTable table = findTableByName("contract_status_table");
        if (table == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        int active = contractDAO.countContractsByStatus("ACTIVE");
        int expired = contractDAO.countContractsByStatus("EXPIRED");
        List<Contract> expiring = contractDAO.getExpiringContracts(30);

        model.addRow(new Object[]{"Đang hiệu lực", active, "Hợp đồng đang hoạt động"});
        model.addRow(new Object[]{"Sắp hết hạn", expiring.size(), "Còn <= 30 ngày"});
        model.addRow(new Object[]{"Đã hết hạn", expired, "Cần gia hạn hoặc kết thúc"});
    }

    /**
     * ===== EXPORT FUNCTIONS =====
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

    /**
     * ===== UTILITIES =====
     */
    private void updateStatCard(String key, String value) {
        updateStatCardRecursive(getComponents(), key, value);
    }

    private void updateStatCardRecursive(Component[] components, String key, String value) {
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (key.equals(label.getName())) {
                    label.setText(value);
                    return;
                }
            }
            if (comp instanceof Container) {
                updateStatCardRecursive(((Container) comp).getComponents(), key, value);
            }
        }
    }

    private JTable findTableByName(String name) {
        return findTableRecursive(getComponents(), name);
    }

    private JTable findTableRecursive(Component[] components, String name) {
        for (Component comp : components) {
            if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                if (name.equals(table.getName())) {
                    return table;
                }
            }
            if (comp instanceof Container) {
                JTable result = findTableRecursive(((Container) comp).getComponents(), name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "0 VNĐ";
        }

        long value = amount.longValue();
        if (value >= 1_000_000_000) {
            return String.format("%.2f tỷ", value / 1_000_000_000.0);
        } else if (value >= 1_000_000) {
            return String.format("%.1f tr", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%,d k", value / 1_000);
        } else {
            return String.format("%,d VNĐ", value);
        }
    }
}
