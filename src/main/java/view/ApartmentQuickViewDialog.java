package view;

import dao.ContractDAO;
import dao.InvoiceDAO;
import dao.ResidentDAO; // [QUAN TRỌNG] Import DAO Cư dân
import model.Apartment;
import model.Building;
import model.Contract;
import model.Floor;
import model.Invoice;
import model.Resident;  // [QUAN TRỌNG] Import Model Cư dân
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public class ApartmentQuickViewDialog extends JDialog {

    private Apartment apartment;
    private Building building;
    private Floor floor;

    private InvoiceDAO invoiceDAO;
    private ContractDAO contractDAO;
    private ResidentDAO residentDAO; // Khai báo ResidentDAO

    private Invoice latestInvoice;
    private Contract activeContract;
    private Resident currentResident; // Biến lưu thông tin người thuê

    private static final DecimalFormat df = new DecimalFormat("#,###");
    private boolean hasActiveContract = false;
    private Image windowIcon;

    // --- CONFIG FONT ---
    private static final Font FONT_TITLE_BIG = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_VALUE = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    public ApartmentQuickViewDialog(Frame owner, Apartment apartment, Building building, Floor floor, Consumer<Apartment> onEditAction) {
        super(owner, "Thông Tin Chi Tiết - " + apartment.getRoomNumber(), true);
        this.apartment = apartment;
        this.building = building;
        this.floor = floor;

        // Khởi tạo các DAO
        this.invoiceDAO = new InvoiceDAO();
        this.contractDAO = new ContractDAO();
        this.residentDAO = new ResidentDAO();

        // Tải dữ liệu từ Database
        loadRealData();

        updateWindowIcon();
        initUI();
        setSize(850, 600);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    // --- HÀM TẢI DỮ LIỆU GHÉP TỪ DB ---
    private void loadRealData() {
        if (apartment.getId() == null) {
            return;
        }

        try {
            // 1. Tìm hợp đồng đang hiệu lực
            this.activeContract = contractDAO.getActiveContractByApartmentId(apartment.getId());

            if (this.activeContract != null) {
                this.hasActiveContract = true;

                // 2. Nếu có hợp đồng -> Lấy thông tin Cư dân dựa vào residentId
                if (this.activeContract.getResidentId() != null) {
                    this.currentResident = residentDAO.getResidentById(this.activeContract.getResidentId());
                }

                // 3. Lấy hóa đơn mới nhất
                this.latestInvoice = invoiceDAO.getLatestInvoiceByApartmentId(apartment.getId());
            } else {
                this.hasActiveContract = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.hasActiveContract = false;
        }
    }

    private void updateWindowIcon() {
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UIConstants.PRIMARY_COLOR);
        g2.fillRoundRect(0, 0, size, size, 20, 20);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int pad = size / 4;
        Path2D p = new Path2D.Float();
        p.moveTo(pad, size - pad);
        p.lineTo(pad, size / 2);
        p.lineTo(size / 2, pad);
        p.lineTo(size - pad, size / 2);
        p.lineTo(size - pad, size - pad);
        p.closePath();
        g2.draw(p);
        g2.drawRect(size / 2 - 6, size / 2 + 2, 12, 12);
        g2.dispose();
        this.windowIcon = image;
        this.setIconImage(image);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 250));

        // === 1. HEADER ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(230, 230, 230), 0, false),
                new EmptyBorder(20, 30, 20, 30)
        ));

        JPanel titleInfo = new JPanel(new GridLayout(2, 1, 0, 5));
        titleInfo.setOpaque(false);

        JLabel lblRoom = new JLabel("Căn Hộ " + apartment.getRoomNumber());
        lblRoom.setFont(FONT_TITLE_BIG);
        lblRoom.setForeground(new Color(33, 33, 33));

        String bName = (building != null) ? building.getName() : "---";
        String fName = (floor != null) ? floor.getName() : "---";
        JLabel lblLoc = new JLabel(String.format("Tòa %s - %s", bName, fName));
        lblLoc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblLoc.setForeground(Color.GRAY);
        lblLoc.setIcon(new SimpleIcon("LOCATION", 16, Color.GRAY));

        titleInfo.add(lblRoom);
        titleInfo.add(lblLoc);

        headerPanel.add(titleInfo, BorderLayout.WEST);

        // Badge hiển thị dựa trên thực tế có hợp đồng hay không
        String statusToShow = hasActiveContract ? "RENTED" : apartment.getStatus();
        headerPanel.add(createStatusBadge(statusToShow), BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // === 2. BODY ===
        JPanel bodyPanel = new JPanel(new GridLayout(1, 2, 25, 0));
        bodyPanel.setBackground(new Color(245, 247, 250));
        bodyPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        // --- CỘT TRÁI ---
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);

        leftCol.add(createSectionPanel("Thông Tin Cốt Lõi", "HOME", createApartmentInfoPanel()));
        leftCol.add(Box.createVerticalStrut(15));

        if (checkMaintenanceLock()) {
            leftCol.add(createMaintenanceWarningPanel());
        } else {
            leftCol.add(createSectionPanel("Người Thuê / Chủ Hộ", "USER_GROUP", createTenantPanel()));
        }

        // --- CỘT PHẢI ---
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);

        rightCol.add(createSectionPanel("Hợp Đồng (Tóm tắt)", "CONTRACT", createContractPanel()));
        rightCol.add(Box.createVerticalStrut(15));

        rightCol.add(createSectionPanel("Tình Trạng Tài Chính", "MONEY", createFinancePanel()));

        bodyPanel.add(leftCol);
        bodyPanel.add(rightCol);
        add(bodyPanel, BorderLayout.CENTER);

        // === 3. FOOTER ===
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new MatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JButton btnClose = new RoundedButton("Đóng", 10);
        btnClose.setBackground(new Color(240, 240, 240));
        btnClose.setForeground(Color.BLACK);
        btnClose.setPreferredSize(new Dimension(100, 38));
        btnClose.addActionListener(e -> dispose());

        footerPanel.add(btnClose);
        add(footerPanel, BorderLayout.SOUTH);
    }

    // --- PANEL HỢP ĐỒNG (HIỂN THỊ DỮ LIỆU TỪ ACTIVE CONTRACT) ---
    private JPanel createContractPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        if (hasActiveContract && activeContract != null) {
            JPanel info = new JPanel(new GridLayout(4, 1, 0, 8));
            info.setOpaque(false);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Xử lý ngày bắt đầu
            String startStr = "---";
            if (activeContract.getStartDate() != null) {
                LocalDate ld = new java.sql.Date(activeContract.getStartDate().getTime()).toLocalDate();
                startStr = ld.format(fmt);
            }

            // Xử lý ngày kết thúc và tính hạn
            String endStr = "---";
            long daysBetween = 0;
            boolean hasEndDate = false;

            if (activeContract.getEndDate() != null) {
                LocalDate ld = new java.sql.Date(activeContract.getEndDate().getTime()).toLocalDate();
                endStr = ld.format(fmt);
                daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), ld);
                hasEndDate = true;
            }

            info.add(createRow("Ngày bắt đầu:", startStr));
            info.add(createRow("Ngày kết thúc:", endStr));

            // Tính toán trạng thái
            JLabel lblStatus = new JLabel();
            lblStatus.setFont(FONT_VALUE);

            if (hasEndDate && daysBetween < 0) {
                lblStatus.setText(" ĐÃ HẾT HẠN");
                lblStatus.setForeground(new Color(211, 47, 47)); // Đỏ
                lblStatus.setIcon(new StatusDotIcon(new Color(211, 47, 47)));
            } else if (hasEndDate && daysBetween <= 30) {
                lblStatus.setText(" Sắp hết hạn (" + daysBetween + " ngày)");
                lblStatus.setForeground(new Color(230, 81, 0)); // Cam
                lblStatus.setIcon(new StatusDotIcon(new Color(230, 81, 0)));
            } else {
                lblStatus.setText(" Đang hiệu lực");
                lblStatus.setForeground(new Color(46, 125, 50)); // Xanh
                lblStatus.setIcon(new StatusDotIcon(new Color(46, 125, 50)));
            }

            JPanel statusRow = new JPanel(new BorderLayout());
            statusRow.setOpaque(false);
            JLabel lblTitle = new JLabel("Trạng thái:");
            lblTitle.setFont(FONT_LABEL);
            lblTitle.setForeground(Color.GRAY);
            statusRow.add(lblTitle, BorderLayout.WEST);
            statusRow.add(lblStatus, BorderLayout.EAST);
            info.add(statusRow);

            String deposit = (activeContract.getDepositAmount() != null) ? df.format(activeContract.getDepositAmount()) + " đ" : "0 đ";
            info.add(createRow("Tiền cọc:", deposit));
            p.add(info, BorderLayout.CENTER);
        } else {
            JLabel lbl = new JLabel("--- Không có hợp đồng ---");
            lbl.setFont(FONT_LABEL);
            lbl.setForeground(Color.LIGHT_GRAY);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            p.add(lbl, BorderLayout.CENTER);
        }
        return p;
    }

    // --- PANEL NGƯỜI THUÊ (DÙNG DỮ LIỆU TỪ RESIDENT DAO) ---
    private JPanel createTenantPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        // Kiểm tra nếu tìm thấy Cư dân
        if (currentResident != null) {
            JPanel info = new JPanel(new GridLayout(3, 1, 0, 5));
            info.setOpaque(false);

            // Lấy thông tin từ object Resident (Không dùng Contract)
            String name = (currentResident.getFullName() != null) ? currentResident.getFullName() : "Không rõ tên";
            String phone = (currentResident.getPhone() != null) ? currentResident.getPhone() : "---";

            // Ngày vào ở lấy từ Hợp đồng
            String joinDate = "---";
            if (activeContract != null && activeContract.getStartDate() != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate ld = new java.sql.Date(activeContract.getStartDate().getTime()).toLocalDate();
                joinDate = ld.format(fmt);
            }

            info.add(createBoldRow("Họ tên:", name));
            info.add(createRow("Số điện thoại:", phone));
            info.add(createRow("Ngày vào ở:", joinDate));
            p.add(info, BorderLayout.CENTER);
        } else {
            String msg = hasActiveContract ? "Chưa cập nhật thông tin cư dân" : "Hiện chưa có người ở";
            JLabel lblEmpty = new JLabel(msg);
            lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            lblEmpty.setForeground(Color.GRAY);
            lblEmpty.setIcon(new SimpleIcon("INFO", 16, Color.GRAY));
            lblEmpty.setHorizontalAlignment(SwingConstants.CENTER);
            p.add(lblEmpty, BorderLayout.CENTER);
        }
        return p;
    }

    // --- CÁC PANEL KHÁC (GIỮ NGUYÊN) ---
    private JPanel createApartmentInfoPanel() {
        JPanel p = new JPanel(new GridLayout(3, 2, 10, 8));
        p.setOpaque(false);
        p.add(createInfoItem("Diện tích", apartment.getArea() + " m²"));
        p.add(createInfoItem("Loại căn", apartment.getApartmentType()));
        p.add(createInfoItem("Phòng ngủ", apartment.getBedroomCount() + " PN"));
        p.add(createInfoItem("Phòng tắm", apartment.getBathroomCount() + " PT"));
        String note = apartment.getDescription();
        if (note == null || note.isEmpty()) {
            note = "Không có";
        }
        p.add(createInfoItem("Ghi chú", note));

        return p;
    }

    private JPanel createFinancePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        if (latestInvoice != null) {
            JPanel info = new JPanel(new GridLayout(3, 1, 0, 5));
            info.setOpaque(false);
            info.add(createRow("Tháng:", latestInvoice.getMonth() + "/" + latestInvoice.getYear()));
            info.add(createBoldRow("Tổng tiền:", df.format(latestInvoice.getTotalAmount()) + " đ"));

            String statusStr = "PAID".equals(latestInvoice.getStatus()) ? " Đã thanh toán" : " Chưa thanh toán";
            Color color = "PAID".equals(latestInvoice.getStatus()) ? new Color(46, 125, 50) : new Color(198, 40, 40);

            JLabel status = new JLabel(statusStr);
            status.setFont(FONT_VALUE);
            status.setForeground(color);
            status.setIcon(new StatusDotIcon(color));

            JPanel statusRow = new JPanel(new BorderLayout());
            statusRow.setOpaque(false);
            JLabel lblT = new JLabel("Trạng thái:");
            lblT.setFont(FONT_LABEL);
            lblT.setForeground(Color.GRAY);
            statusRow.add(lblT, BorderLayout.WEST);
            statusRow.add(status, BorderLayout.EAST);

            info.add(statusRow);
            p.add(info, BorderLayout.CENTER);
        } else {
            JLabel lbl = new JLabel("--- Chưa có hóa đơn ---");
            lbl.setFont(FONT_LABEL);
            lbl.setForeground(Color.LIGHT_GRAY);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            p.add(lbl, BorderLayout.CENTER);
        }
        return p;
    }

    // --- UI HELPERS ---
    private JLabel createStatusBadge(String status) {
        String s = (status == null) ? "" : status.trim();
        Color bg, fg;
        String text;
        if ("RENTED".equalsIgnoreCase(s) || "Đã thuê".equalsIgnoreCase(s) || "OCCUPIED".equalsIgnoreCase(s)) {
            bg = new Color(232, 245, 233);
            fg = new Color(46, 125, 50);
            text = "ĐANG THUÊ";
        } else if ("MAINTENANCE".equalsIgnoreCase(s) || "Bảo trì".equalsIgnoreCase(s) || "Đang bảo trì".equalsIgnoreCase(s)) {
            bg = new Color(255, 243, 224);
            fg = new Color(239, 108, 0);
            text = "BẢO TRÌ";
        } else {
            bg = new Color(227, 242, 253);
            fg = new Color(25, 118, 210);
            text = "TRỐNG";
        }
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(5, 15, 5, 15));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private JPanel createSectionPanel(String title, String iconType, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(220, 220, 220), 1, true), new EmptyBorder(15, 20, 15, 20)));
        JLabel lblTitle = new JLabel(" " + title);
        lblTitle.setFont(FONT_HEADER);
        lblTitle.setForeground(UIConstants.PRIMARY_COLOR);
        if (iconType != null) {
            lblTitle.setIcon(new SimpleIcon(iconType, 16, UIConstants.PRIMARY_COLOR));
        }
        p.add(lblTitle, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private JPanel createInfoItem(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(FONT_LABEL);
        l.setForeground(Color.GRAY);
        JLabel v = new JLabel(value);
        v.setFont(FONT_VALUE);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private JPanel createRow(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(FONT_LABEL);
        l.setForeground(Color.GRAY);
        JLabel v = new JLabel(value);
        v.setFont(FONT_LABEL);
        v.setForeground(new Color(33, 33, 33));
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private JPanel createBoldRow(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(FONT_LABEL);
        l.setForeground(Color.GRAY);
        JLabel v = new JLabel(value);
        v.setFont(FONT_VALUE);
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private boolean checkMaintenanceLock() {
        boolean bm = (building.getStatus() != null && building.getStatus().contains("bảo trì"));
        String fs = floor.getStatus();
        return bm || (fs != null && (fs.toLowerCase().contains("bảo trì") || fs.toLowerCase().contains("maintenance")));
    }

    private JPanel createMaintenanceWarningPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(new Color(255, 243, 224));
        p.setBorder(new CompoundBorder(new LineBorder(new Color(255, 183, 77), 1, true), new EmptyBorder(10, 10, 10, 10)));
        JLabel icon = new JLabel(new SimpleIcon("WARNING", 24, new Color(239, 108, 0)));
        JLabel text = new JLabel("<html><b>CẢNH BÁO BẢO TRÌ</b><br>Khu vực này đang được sửa chữa.</html>");
        text.setFont(FONT_LABEL);
        text.setForeground(new Color(230, 81, 0));
        p.add(icon, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        return p;
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
            setFont(new Font("Segoe UI", Font.BOLD, 12));
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

    private static class StatusDotIcon implements Icon {

        private Color color;

        public StatusDotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x + 2, y + 4, 8, 8);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    private static class SimpleIcon implements Icon {

        private String type;
        private int size;
        private Color color;

        public SimpleIcon(String type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.translate(x, y);
            if ("HOME".equals(type) || "USER_GROUP".equals(type) || "CONTRACT".equals(type) || "MONEY".equals(type)) {
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setStroke(new BasicStroke(2f));
            }
            if ("LOCATION".equals(type)) {
                g2.drawOval(size / 2 - 4, 1, 8, 8);
                Path2D p = new Path2D.Float();
                p.moveTo(size / 2 - 4, 5);
                p.lineTo(size / 2, size - 1);
                p.lineTo(size / 2 + 4, 5);
                g2.draw(p);
                g2.fillOval(size / 2 - 1, 4, 2, 2);
            } else if ("INFO".equals(type)) {
                g2.drawOval(1, 1, size - 2, size - 2);
                g2.drawLine(size / 2, 4, size / 2, 9);
                g2.drawLine(size / 2, 11, size / 2, 12);
            } else if ("WARNING".equals(type)) {
                Path2D p = new Path2D.Float();
                p.moveTo(size / 2.0, 1);
                p.lineTo(1, size - 1);
                p.lineTo(size - 1, size - 1);
                p.closePath();
                g2.draw(p);
                g2.drawLine(size / 2, 5, size / 2, 9);
                g2.drawLine(size / 2, 11, size / 2, 11);
            } else if ("BILL".equals(type)) {
                g2.drawRect(3, 2, size - 6, size - 4);
                g2.drawLine(5, 5, size - 5, 5);
                g2.drawLine(5, 8, size - 5, 8);
                g2.drawLine(5, 11, size - 7, 11);
            } else if ("HOME".equals(type)) {
                Path2D p = new Path2D.Float();
                p.moveTo(1, size / 2);
                p.lineTo(size / 2, 1);
                p.lineTo(size - 1, size / 2);
                g2.draw(p);
                g2.drawRect(3, size / 2, size - 6, size / 2 - 1);
            } else if ("USER_GROUP".equals(type)) {
                g2.drawOval(size / 2 - 3, 1, 6, 6);
                g2.drawArc(1, size / 2 + 1, size - 2, size / 2 + 2, 0, 180);
            } else if ("CONTRACT".equals(type)) {
                g2.drawRect(3, 1, size - 6, size - 2);
                g2.drawLine(5, 4, size - 5, 4);
                g2.drawLine(5, 7, size - 5, 7);
                g2.drawLine(5, 10, size - 5, 10);
            } else if ("MONEY".equals(type)) {
                g2.drawOval(1, 1, size - 2, size - 2);
                g2.setFont(new Font("Segoe UI", Font.BOLD, size - 4));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (size - fm.stringWidth("$")) / 2;
                int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("$", tx, ty);
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
