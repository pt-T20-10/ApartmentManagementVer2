package view;

import model.Apartment;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public class ApartmentCard extends JPanel {

    private Apartment apartment;
    private LocalDate contractEndDate;

    private Consumer<Apartment> onSelect;
    private Consumer<Apartment> onEdit;
    private Consumer<Apartment> onDelete;

    public ApartmentCard(Apartment apartment, LocalDate contractEndDate,
            Consumer<Apartment> onSelect,
            Consumer<Apartment> onEdit,
            Consumer<Apartment> onDelete) {
        this.apartment = apartment;
        this.contractEndDate = contractEndDate;

        this.onSelect = onSelect;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        setOpaque(false);
        setPreferredSize(new Dimension(300, 185));

        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelect != null) {
                    onSelect.accept(apartment);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(new Color(252, 252, 252));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(null);
                repaint();
            }
        });

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 18, 12, 18));

        // HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleGroup.setOpaque(false);

        JLabel lblRoom = new JLabel("P. " + apartment.getRoomNumber());
        lblRoom.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblRoom.setForeground(new Color(33, 33, 33));

        String type = apartment.getApartmentType() != null ? apartment.getApartmentType() : "Std";
        JLabel lblType = new JLabel(type);
        lblType.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblType.setForeground(Color.GRAY);
        lblType.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)), new EmptyBorder(1, 5, 1, 5)
        ));

        titleGroup.add(lblRoom);
        titleGroup.add(lblType);
        headerPanel.add(titleGroup, BorderLayout.WEST);
        headerPanel.add(createStatusBadge(apartment.getStatus()), BorderLayout.EAST);

        // BODY
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(new EmptyBorder(10, 0, 5, 0));

        JPanel specRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        specRow.setOpaque(false);
        specRow.add(createIconLabel("AREA", apartment.getArea() + " m²"));
        specRow.add(Box.createHorizontalStrut(12));
        specRow.add(createIconLabel("BED", apartment.getBedroomCount() + " PN"));
        specRow.add(Box.createHorizontalStrut(12));
        specRow.add(createIconLabel("BATH", apartment.getBathroomCount() + " PT"));

        bodyPanel.add(specRow);
        bodyPanel.add(Box.createVerticalStrut(5));

        // FOOTER
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        footerPanel.add(createFooterInfo(), BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnGroup.setOpaque(false);

        // --- LOGIC ẨN NÚT SỬA/XÓA ---
        if (onEdit != null) {
            JButton btnEdit = createIconButton("EDIT", new Color(117, 117, 117));
            btnEdit.addActionListener(e -> onEdit.accept(apartment));
            btnEdit.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    e.consume();
                }
            });
            btnGroup.add(btnEdit);
        }

        if (onDelete != null) {
            JButton btnDelete = createIconButton("DELETE", new Color(229, 57, 53));
            btnDelete.addActionListener(e -> onDelete.accept(apartment));
            btnDelete.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    e.consume();
                }
            });
            btnGroup.add(btnDelete);
        }

        footerPanel.add(btnGroup, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(bodyPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    // ... (Giữ nguyên các hàm helper và Inner Class từ file gốc) ...
    // --- Copy phần dưới để file chạy được ---
    private JComponent createFooterInfo() {
        String s = apartment.getStatus() == null ? "" : apartment.getStatus().trim();
        if (s.equalsIgnoreCase("OWNED")) {
            JLabel lblOwned = new JLabel(" ĐÃ BÁN - Không cho thuê");
            lblOwned.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblOwned.setForeground(new Color(194, 24, 91));
            lblOwned.setIcon(new CardIcon("CHECK", 14, new Color(194, 24, 91)));
            return lblOwned;
        }
        boolean isRented = s.equalsIgnoreCase("RENTED") || s.equalsIgnoreCase("OCCUPIED") || s.equalsIgnoreCase("Đã thuê") || s.equalsIgnoreCase("Đang thuê");
        if (contractEndDate != null && isRented) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), contractEndDate);
            JLabel lblAlert = new JLabel();
            lblAlert.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (days < 0) {
                lblAlert.setText(" QUÁ HẠN " + Math.abs(days) + " NGÀY");
                lblAlert.setForeground(new Color(211, 47, 47));
                lblAlert.setIcon(new CardIcon("WARNING", 14, new Color(211, 47, 47)));
                return lblAlert;
            } else if (days <= 30) {
                lblAlert.setText(" Hết hạn: " + days + " ngày");
                lblAlert.setForeground(new Color(230, 81, 0));
                lblAlert.setIcon(new CardIcon("TIME", 14, new Color(230, 81, 0)));
                return lblAlert;
            }
        }
        String desc = apartment.getDescription();
        if (desc == null || desc.isEmpty()) {
            desc = "Không có ghi chú";
        }
        if (desc.length() > 18) {
            desc = desc.substring(0, 16) + "...";
        }
        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblDesc.setForeground(new Color(150, 150, 150));
        return lblDesc;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground() != null ? getBackground() : Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
        String s = apartment.getStatus() == null ? "" : apartment.getStatus().trim();
        if (s.equalsIgnoreCase("OWNED")) {
            g2.setColor(new Color(233, 30, 99));
            g2.setStroke(new BasicStroke(2f));
        } else {
            boolean isRented = s.equalsIgnoreCase("RENTED") || s.equalsIgnoreCase("OCCUPIED") || s.equalsIgnoreCase("Đã thuê");
            if (contractEndDate != null && isRented) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), contractEndDate);
                if (days < 0) {
                    g2.setColor(new Color(239, 83, 80));
                    g2.setStroke(new BasicStroke(2f));
                } else if (days <= 30) {
                    g2.setColor(new Color(255, 167, 38));
                    g2.setStroke(new BasicStroke(2f));
                } else {
                    g2.setColor(new Color(230, 230, 230));
                    g2.setStroke(new BasicStroke(1f));
                }
            } else {
                g2.setColor(new Color(230, 230, 230));
                g2.setStroke(new BasicStroke(1f));
            }
        }
        g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
        g2.dispose();
    }

    private StatusBadge createStatusBadge(String status) {
        String s = (status == null) ? "AVAILABLE" : status.trim();
        Color bg, fg;
        String text;
        if (s.equalsIgnoreCase("OWNED")) {
            bg = new Color(255, 235, 238);
            fg = new Color(194, 24, 91);
            text = "ĐÃ BÁN";
        } else if (s.equalsIgnoreCase("RENTED") || s.equalsIgnoreCase("OCCUPIED") || s.equalsIgnoreCase("Đã thuê") || s.equalsIgnoreCase("Đang thuê")) {
            bg = new Color(232, 245, 233);
            fg = new Color(46, 125, 50);
            text = "ĐÃ THUÊ";
        } else if (s.equalsIgnoreCase("MAINTENANCE") || s.equalsIgnoreCase("Bảo trì")) {
            bg = new Color(255, 243, 224);
            fg = new Color(239, 108, 0);
            text = "BẢO TRÌ";
        } else {
            bg = new Color(227, 242, 253);
            fg = new Color(25, 118, 210);
            text = "TRỐNG";
        }
        return new StatusBadge(text, bg, fg);
    }

    private JLabel createIconLabel(String icon, String text) {
        JLabel l = new JLabel(" " + text);
        l.setIcon(new CardIcon(icon, 14, new Color(100, 100, 100)));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(70, 70, 70));
        return l;
    }

    private JButton createIconButton(String iconType, Color color) {
        JButton btn = new JButton(new CardIcon(iconType, 18, color));
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static class StatusBadge extends JLabel {

        private Color bgColor, textColor;

        public StatusBadge(String text, Color bg, Color txt) {
            super(text);
            this.bgColor = bg;
            this.textColor = txt;
            setFont(new Font("Segoe UI", Font.BOLD, 10));
            setForeground(textColor);
            setBorder(new EmptyBorder(3, 10, 3, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class CardIcon implements Icon {

        private String type;
        private int size;
        private Color color;

        public CardIcon(String type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate(x, y);
            if ("EDIT".equals(type)) {
                g2.rotate(Math.toRadians(45), size / 2.0, size / 2.0);
                g2.drawRoundRect(size / 2 - 2, 0, 4, size - 4, 1, 1);
                g2.drawLine(size / 2 - 2, 3, size / 2 + 2, 3);
                Path2D tip = new Path2D.Float();
                tip.moveTo(size / 2 - 2, size - 4);
                tip.lineTo(size / 2, size);
                tip.lineTo(size / 2 + 2, size - 4);
                g2.fill(tip);
            } else if ("DELETE".equals(type)) {
                int w = size - 6;
                int h = size - 4;
                int mx = 3;
                int my = 4;
                g2.drawRoundRect(mx, my, w, h, 3, 3);
                g2.drawLine(1, my, size - 1, my);
                g2.drawArc(size / 2 - 2, 0, 4, 4, 0, 180);
                g2.drawLine(size / 2 - 2, my + 3, size / 2 - 2, my + h - 3);
                g2.drawLine(size / 2 + 2, my + 3, size / 2 + 2, my + h - 3);
            } else if ("AREA".equals(type)) {
                g2.drawRect(2, 2, size - 4, size - 4);
                g2.drawLine(size / 2, 2, size / 2, size - 2);
                g2.drawLine(2, size / 2, size - 2, size / 2);
            } else if ("BED".equals(type)) {
                g2.drawRoundRect(1, 4, size - 2, size - 8, 2, 2);
                g2.drawLine(4, 4, 4, 2);
                g2.drawLine(size - 4, 4, size - 4, 2);
            } else if ("BATH".equals(type)) {
                g2.drawRoundRect(2, size / 2, size - 4, size / 2 - 2, 3, 3);
                g2.drawLine(1, size / 2 + 2, 1, size - 3);
                g2.drawLine(size - 1, size / 2 + 2, size - 1, size - 3);
                g2.drawOval(size / 2 - 1, 3, 3, 3);
            } else if ("CHECK".equals(type)) {
                Path2D check = new Path2D.Float();
                check.moveTo(2, size / 2);
                check.lineTo(size / 2 - 1, size - 3);
                check.lineTo(size - 2, 2);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(check);
            } else if ("WARNING".equals(type)) {
                Path2D p = new Path2D.Float();
                p.moveTo(size / 2.0, 1);
                p.lineTo(1, size - 1);
                p.lineTo(size - 1, size - 1);
                p.closePath();
                g2.draw(p);
                g2.drawLine(size / 2, 5, size / 2, size - 5);
                g2.drawLine(size / 2, size - 3, size / 2, size - 3);
            } else if ("TIME".equals(type)) {
                g2.drawOval(1, 1, size - 2, size - 2);
                g2.drawLine(size / 2, 4, size / 2, size / 2);
                g2.drawLine(size / 2, size / 2, size - 4, size / 2);
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
