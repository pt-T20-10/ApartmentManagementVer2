package view;

import dao.*;
import model.Building;
import model.User;
import util.SessionManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * Dashboard Panel - Updated with Embedded Building Selector
 */
public class DashboardPanel extends JPanel {

    private final Color CARD_BG = Color.WHITE;
    private final Color PRIMARY_COLOR = new Color(76, 132, 255);
    private final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private final Color WARNING_COLOR = new Color(255, 193, 7);
    private final Color DANGER_COLOR = new Color(220, 53, 69);
    private final Color INFO_COLOR = new Color(23, 162, 184);
    private final Color PURPLE_COLOR = new Color(111, 66, 193);

    private final DashboardNavigator navigator;
    private final User currentUser;
    
    // ✅ NEW: Mutable ID (Có thể thay đổi)
    private Long selectedBuildingId = null;

    private BuildingDAO buildingDAO;
    private ApartmentDAO apartmentDAO;
    private ResidentDAO residentDAO;
    private ContractDAO contractDAO;
    private InvoiceDAO invoiceDAO;
    
    // ✅ NEW: Container để reload dữ liệu
    private JPanel bodyPanel;

    public DashboardPanel(DashboardNavigator navigator) {
        this.navigator = navigator;
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        // Mặc định null = Tất cả (Nếu Admin) hoặc Tổng hợp (Nếu Manager)
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        buildingDAO = new BuildingDAO();
        apartmentDAO = new ApartmentDAO();
        residentDAO = new ResidentDAO();
        contractDAO = new ContractDAO();
        invoiceDAO = new InvoiceDAO();

        initUI();
    }

    private void initUI() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(245, 247, 250));
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(new Color(245, 247, 250));

        // 1. Header (Có ComboBox)
        main.add(createHeaderWithSelector(), BorderLayout.NORTH);

        // 2. Body (KPI + Charts)
        bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(new Color(245, 247, 250));
        
        // Load data lần đầu
        refreshBodyData();

        main.add(bodyPanel, BorderLayout.CENTER);
        wrapper.add(main, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ✅ NEW: Header chứa Tiêu đề + ComboBox
    private JPanel createHeaderWithSelector() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setBackground(new Color(245, 247, 250));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(33, 37, 41));
        header.add(title);

        // Nếu User có nhiều toà nhà (hoặc Admin), hiển thị Selector
        List<Building> buildings = buildingDAO.getAllBuildings(); // DAO tự lọc theo quyền
        
        if (!buildings.isEmpty()) {
            header.add(Box.createHorizontalStrut(20)); // Khoảng cách

            JComboBox<BuildingItem> selector = new JComboBox<>();
            selector.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            selector.setFocusable(false);
            selector.setBackground(Color.WHITE);
            
            // Item mặc định: "Tất cả"
            selector.addItem(new BuildingItem(null, "--- Tất cả tòa nhà ---"));
            
            for (Building b : buildings) {
                selector.addItem(new BuildingItem(b.getId(), b.getName()));
            }

            // Sự kiện chọn
            selector.addActionListener(e -> {
                BuildingItem item = (BuildingItem) selector.getSelectedItem();
                this.selectedBuildingId = (item != null) ? item.id : null;
                refreshBodyData(); // Reload lại phần dưới
            });

            header.add(selector);
        }

        return header;
    }

    // ✅ NEW: Hàm làm mới dữ liệu
    private void refreshBodyData() {
        bodyPanel.removeAll();
        
        bodyPanel.add(createKPISection());
        bodyPanel.add(Box.createVerticalStrut(20));
        bodyPanel.add(createChartsSection());
        
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    // Helper Class cho ComboBox
    private static class BuildingItem {
        Long id;
        String name;
        public BuildingItem(Long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    // ================= LOGIC TÍNH TOÁN (Giữ nguyên như cũ) =================
    
    private int getBuildingCount() {
        if (selectedBuildingId != null) return 1;
        if (currentUser.isAdmin()) return buildingDAO.countBuildings();
        return currentUser.getBuildingIds().size();
    }

    private int getApartmentCount() {
        if (selectedBuildingId != null) return apartmentDAO.countApartmentsByBuilding(selectedBuildingId);
        int sum = 0;
        if (currentUser.isAdmin()) return apartmentDAO.countApartments();
        for (Long id : currentUser.getBuildingIds()) sum += apartmentDAO.countApartmentsByBuilding(id);
        return sum;
    }

    private int getAvailableApartmentCount() {
        if (selectedBuildingId != null) return apartmentDAO.countAvailableApartmentsByBuilding(selectedBuildingId);
        int sum = 0;
        if (currentUser.isAdmin()) return apartmentDAO.countAvailableApartments();
        for (Long id : currentUser.getBuildingIds()) sum += apartmentDAO.countAvailableApartmentsByBuilding(id);
        return sum;
    }

    private int getRentedApartmentCount() {
        if (selectedBuildingId != null) return apartmentDAO.countRentedApartmentsByBuilding(selectedBuildingId);
        int sum = 0;
        if (currentUser.isAdmin()) return apartmentDAO.countRentedApartments();
        for (Long id : currentUser.getBuildingIds()) sum += apartmentDAO.countRentedApartmentsByBuilding(id);
        return sum;
    }

    private int getResidentCount() {
        if (selectedBuildingId != null) return residentDAO.countResidentsByBuilding(selectedBuildingId);
        if (currentUser.isAdmin()) return residentDAO.countResidents();
        
        int sum = 0;
        for (Long id : currentUser.getBuildingIds()) sum += residentDAO.countResidentsByBuilding(id);
        return sum;
    }

    private BigDecimal getTotalRevenue() {
        if (selectedBuildingId != null) return invoiceDAO.getTotalRevenueByBuilding(selectedBuildingId);
        if (currentUser.isAdmin()) return invoiceDAO.getTotalRevenue();
        
        BigDecimal sum = BigDecimal.ZERO;
        for (Long id : currentUser.getBuildingIds()) {
            sum = sum.add(invoiceDAO.getTotalRevenueByBuilding(id));
        }
        return sum;
    }

    private int getUnpaidInvoicesCount() {
        if (selectedBuildingId != null) return invoiceDAO.countUnpaidInvoicesByBuilding(selectedBuildingId);
        if (currentUser.isAdmin()) return invoiceDAO.countUnpaidInvoices();
        
        int sum = 0;
        for (Long id : currentUser.getBuildingIds()) sum += invoiceDAO.countUnpaidInvoicesByBuilding(id);
        return sum;
    }
    
    private int getActiveContractsCount() {
        if (selectedBuildingId != null) {
            return (int) contractDAO.getContractsByBuilding(selectedBuildingId).stream()
                    .filter(c -> "ACTIVE".equals(c.getStatus())).count();
        }
        if (currentUser.isAdmin()) return contractDAO.countActiveContracts();
        
        int sum = 0;
        for (Long id : currentUser.getBuildingIds()) {
            sum += (int) contractDAO.getContractsByBuilding(id).stream()
                    .filter(c -> "ACTIVE".equals(c.getStatus())).count();
        }
        return sum;
    }

    // ================= UI SECTIONS (Giữ nguyên style cũ) =================

    private JPanel createKPISection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(new Color(245, 247, 250));

        JPanel row1 = new JPanel(new GridLayout(1, 3, 15, 0));
        row1.setBackground(new Color(245, 247, 250));
        row1.add(createStatCard("Tòa Nhà", String.valueOf(getBuildingCount()), "🏢", PRIMARY_COLOR, navigator::goToBuildings));
        row1.add(createStatCard("Tổng Căn Hộ", String.valueOf(getApartmentCount()), "🏠", INFO_COLOR, navigator::goToApartments));
        row1.add(createStatCard("Đang Trống", String.valueOf(getAvailableApartmentCount()), "✓", SUCCESS_COLOR, navigator::goToApartments));

        JPanel row2 = new JPanel(new GridLayout(1, 3, 15, 0));
        row2.setBackground(new Color(245, 247, 250));
        row2.add(createStatCard("Đã Cho Thuê", String.valueOf(getRentedApartmentCount()), "☑", WARNING_COLOR, navigator::goToContracts));
        row2.add(createStatCard("Cư Dân", String.valueOf(getResidentCount()), "👥", PURPLE_COLOR, navigator::goToResidents));
        row2.add(createStatCard("Hợp Đồng", String.valueOf(getActiveContractsCount()), "📋", PRIMARY_COLOR, navigator::goToContracts));

        JPanel row3 = new JPanel(new GridLayout(1, 2, 15, 0));
        row3.setBackground(new Color(245, 247, 250));
        row3.add(createRevenueCard());
        row3.add(createUnpaidCard());

        section.add(row1);
        section.add(Box.createVerticalStrut(15));
        section.add(row2);
        section.add(Box.createVerticalStrut(15));
        section.add(row3);
        return section;
    }

    private JPanel createChartsSection() {
        JPanel section = new JPanel(new GridLayout(1, 2, 15, 0));
        section.setBackground(new Color(245, 247, 250));
        section.setPreferredSize(new Dimension(800, 350));
        section.add(createRevenueChart());
        section.add(createInvoiceStatusChart());
        return section;
    }

    private JPanel createStatCard(String title, String value, String icon, Color color, Runnable onClick) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 30));
        iconLabel.setForeground(color);
        card.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(CARD_BG);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitle.setForeground(Color.GRAY);
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(Color.DARK_GRAY);

        textPanel.add(lblTitle);
        textPanel.add(lblValue);
        card.add(textPanel, BorderLayout.CENTER);

        if (onClick != null) {
            card.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { onClick.run(); }
            });
        }
        return card;
    }

    private JPanel createRevenueCard() {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel lblTitle = new JLabel("Tổng Doanh Thu");
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel lblValue = new JLabel(formatCurrency(getTotalRevenue()));
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblValue.setForeground(SUCCESS_COLOR);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD_BG);
        p.add(lblTitle);
        p.add(lblValue);
        card.add(p, BorderLayout.CENTER);
        return card;
    }

    private JPanel createUnpaidCard() {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel lblTitle = new JLabel("Hóa Đơn Chưa Thu");
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel lblValue = new JLabel(String.valueOf(getUnpaidInvoicesCount()));
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblValue.setForeground(DANGER_COLOR);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD_BG);
        p.add(lblTitle);
        p.add(lblValue);
        card.add(p, BorderLayout.CENTER);
        return card;
    }

    private JPanel createRevenueChart() {
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBackground(CARD_BG);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel title = new JLabel("Doanh Thu 12 Tháng Gần Nhất");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        Calendar cal = Calendar.getInstance();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -i);
            int m = cal.get(Calendar.MONTH) + 1;
            int y = cal.get(Calendar.YEAR);
            labels.add(m + "/" + (y % 100));
            
            // Logic chart (đã dùng selectedBuildingId trong getMonthlyRevenue)
            if (selectedBuildingId != null) {
                values.add(invoiceDAO.getMonthlyRevenue(m, y, selectedBuildingId));
            } else if (currentUser.isAdmin()) {
                values.add(invoiceDAO.getMonthlyRevenue(m, y));
            } else {
                BigDecimal monthlySum = BigDecimal.ZERO;
                for (Long bid : currentUser.getBuildingIds()) {
                    monthlySum = monthlySum.add(invoiceDAO.getMonthlyRevenue(m, y, bid));
                }
                values.add(monthlySum);
            }
        }

        chartPanel.add(title, BorderLayout.NORTH);
        chartPanel.add(new LineChartPanel(labels, values, SUCCESS_COLOR), BorderLayout.CENTER);
        return chartPanel;
    }

    private JPanel createInvoiceStatusChart() {
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBackground(CARD_BG);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel title = new JLabel("Trạng Thái Hóa Đơn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        int paid = 0;
        int unpaid = 0;
        
        if (selectedBuildingId != null) {
            paid = invoiceDAO.countPaidInvoicesByBuilding(selectedBuildingId);
            unpaid = invoiceDAO.countUnpaidInvoicesByBuilding(selectedBuildingId);
        } else if (currentUser.isAdmin()) {
            paid = invoiceDAO.countPaidInvoicesByBuilding(null);
            unpaid = invoiceDAO.countUnpaidInvoicesByBuilding(null);
        } else {
            for (Long id : currentUser.getBuildingIds()) {
                paid += invoiceDAO.countPaidInvoicesByBuilding(id);
                unpaid += invoiceDAO.countUnpaidInvoicesByBuilding(id);
            }
        }

        chartPanel.add(title, BorderLayout.NORTH);
        chartPanel.add(new PieChartPanel(paid, unpaid, SUCCESS_COLOR, DANGER_COLOR), BorderLayout.CENTER);
        return chartPanel;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount) + " đ";
    }

    // --- INNER CLASSES FOR CHARTS ---
    private class LineChartPanel extends JPanel {
        private List<String> labels; private List<BigDecimal> values; private Color lineColor;
        public LineChartPanel(List<String> labels, List<BigDecimal> values, Color lineColor) {
            this.labels = labels; this.values = values; this.lineColor = lineColor;
            setBackground(CARD_BG); setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(); int pad = 30;
            g2.setColor(Color.LIGHT_GRAY); g2.drawLine(pad, h - pad, w - pad, h - pad); g2.drawLine(pad, pad, pad, h - pad);
            if (values == null || values.isEmpty()) return;
            BigDecimal max = values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
            if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.valueOf(100);
            int stepX = (w - 2 * pad) / Math.max(1, values.size() - 1);
            int prevX = pad, prevY = h - pad;
            g2.setColor(lineColor); g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < values.size(); i++) {
                int x = pad + i * stepX;
                double val = values.get(i).doubleValue();
                int y = h - pad - (int) ((val / max.doubleValue()) * (h - 2 * pad));
                if (i > 0) g2.drawLine(prevX, prevY, x, y);
                g2.fillOval(x - 3, y - 3, 6, 6);
                if (values.size() <= 12 || i % 2 == 0) {
                    g2.setColor(Color.GRAY); g2.setFont(new Font("Arial", Font.PLAIN, 10));
                    g2.drawString(labels.get(i), x - 10, h - pad + 15); g2.setColor(lineColor);
                }
                prevX = x; prevY = y;
            }
        }
    }

    private class PieChartPanel extends JPanel {
        private int paid, unpaid; private Color c1, c2;
        public PieChartPanel(int p, int u, Color c1, Color c2) { this.paid = p; this.unpaid = u; this.c1 = c1; this.c2 = c2; setBackground(CARD_BG); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(); int d = Math.min(w, h) - 40;
            int x = (w - d) / 2, y = (h - d) / 2;
            int total = paid + unpaid;
            if (total == 0) { g2.setColor(Color.LIGHT_GRAY); g2.drawOval(x, y, d, d); g2.drawString("Chưa có dữ liệu", x + d / 2 - 40, y + d / 2); return; }
            int angle1 = (int) Math.round((double) paid / total * 360);
            g2.setColor(c1); g2.fillArc(x, y, d, d, 90, -angle1);
            g2.setColor(c2); g2.fillArc(x, y, d, d, 90 - angle1, -(360 - angle1));
            g2.setColor(c1); g2.fillRect(w - 100, h - 40, 10, 10); g2.setColor(Color.BLACK); g2.drawString("Đã thu (" + paid + ")", w - 85, h - 30);
            g2.setColor(c2); g2.fillRect(w - 100, h - 20, 10, 10); g2.setColor(Color.BLACK); g2.drawString("Chưa thu (" + unpaid + ")", w - 85, h - 10);
        }
    }
}