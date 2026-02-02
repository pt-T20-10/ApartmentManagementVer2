package view;

import dao.BuildingDAO.BuildingStats;
import model.Building;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

public class BuildingCard extends JPanel {

    private Building building;
    private BuildingStats stats;
    private java.util.function.Consumer<Building> onSelect;
    private java.util.function.Consumer<Building> onEdit;
    private java.util.function.Consumer<Building> onDelete;

    public BuildingCard(Building building, BuildingStats stats,
            java.util.function.Consumer<Building> onSelect,
            java.util.function.Consumer<Building> onEdit,
            java.util.function.Consumer<Building> onDelete) {
        this.building = building;
        this.stats = stats;
        this.onSelect = onSelect;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        setOpaque(false);
        initUI();
    }

    private void initUI() {
        setPreferredSize(new Dimension(500, 240));
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(15, 20, 15, 20));

        boolean isMaintenance = "Đang bảo trì".equals(building.getStatus());

        // Cursor logic
        if (!isMaintenance) {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isMaintenance) {
                    JOptionPane.showMessageDialog(BuildingCard.this,
                            "Tòa nhà \"" + building.getName() + "\" đang bảo trì.\nKhông thể truy cập dữ liệu bên trong lúc này.",
                            "Quyền truy cập bị hạn chế",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (onSelect != null) {
                    onSelect.accept(building);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isMaintenance) {
                    setBackground(new Color(252, 252, 252));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(null);
                repaint();
            }
        });

        // === TOP PANEL ===
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));
        topPanel.setOpaque(false);

        Color iconColor = isMaintenance ? Color.GRAY : new Color(69, 90, 100);
        JLabel iconLabel = new JLabel(new SimpleIcon("BUILDING_COMPLEX", 48, iconColor)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(227, 242, 253));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLabel.setPreferredSize(new Dimension(80, 80));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(iconLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 0, 5));
        infoPanel.setOpaque(false);

        JLabel lblName = new JLabel(building.getName());
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblName.setForeground(isMaintenance ? Color.GRAY : new Color(33, 33, 33));

        JLabel lblAddress = new JLabel(building.getAddress());
        lblAddress.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblAddress.setForeground(Color.GRAY);

        String manager = (building.getManagerName() == null || building.getManagerName().isEmpty()) ? "Chưa có" : building.getManagerName();
        JLabel lblManager = createStatLabel("USER", "Quản lý: " + manager);
        lblManager.setIcon(new SimpleIcon("USER", 14, new Color(46, 125, 50)));

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statsPanel.setOpaque(false);
        JLabel lblFloors = createStatLabel("FLOOR", stats.totalFloors + " Tầng");
        JLabel lblApts = createStatLabel("DOOR", stats.totalApartments + " Căn hộ");
        statsPanel.add(lblFloors);
        statsPanel.add(Box.createHorizontalStrut(25));
        statsPanel.add(lblApts);

        infoPanel.add(lblName);
        infoPanel.add(lblAddress);
        infoPanel.add(lblManager);
        infoPanel.add(statsPanel);

        topPanel.add(infoPanel, BorderLayout.CENTER);

        StatusBadge statusBadge = new StatusBadge(building.getStatus());
        JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        statusWrapper.setOpaque(false);
        statusWrapper.add(statusBadge);
        topPanel.add(statusWrapper, BorderLayout.EAST);

        // === CENTER & BOTTOM ===
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        OccupancyBar progressBar = new OccupancyBar(stats.getOccupancyRate(), stats.rentedApartments, stats.totalApartments);
        centerPanel.add(progressBar, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // --- LOGIC ẨN NÚT SỬA/XÓA ---
        // Chỉ thêm nút nếu callback != null
        if (onEdit != null) {
            JButton btnEdit = createIconButton("EDIT", new Color(117, 117, 117));
            btnEdit.addActionListener(e -> onEdit.accept(building));
            btnEdit.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    e.consume();
                }
            });
            bottomPanel.add(btnEdit);
        }

        if (onDelete != null) {
            JButton btnDelete = createIconButton("DELETE", new Color(239, 83, 80));
            btnDelete.addActionListener(e -> onDelete.accept(building));
            btnDelete.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    e.consume();
                }
            });
            bottomPanel.add(btnDelete);
        }

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(topPanel, BorderLayout.NORTH);
        content.add(centerPanel, BorderLayout.CENTER);
        content.add(bottomPanel, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground() != null ? getBackground() : Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        g2.setColor(new Color(220, 220, 220));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        g2.dispose();
    }

    // ... (Giữ nguyên các Inner Class: StatusBadge, OccupancyBar, SimpleIcon như file gốc của bạn) ...
    // Để tiết kiệm không gian chat, mình không paste lại phần Inner Class nếu không sửa đổi.
    // Bạn hãy giữ nguyên phần Inner Class từ file gốc nhé.
    // --- Copy lại phần Inner Class để đảm bảo file hoàn chỉnh ---
    private static class StatusBadge extends JLabel {

        private Color bgColor, textColor;

        public StatusBadge(String rawStatus) {
            String displayText = (rawStatus == null) ? "Không xác định" : rawStatus;
            String normalized = (rawStatus == null) ? "" : rawStatus.trim();
            if (normalized.equalsIgnoreCase("ACTIVE") || normalized.equalsIgnoreCase("Hoạt động") || normalized.equalsIgnoreCase("Đang hoạt động")) {
                displayText = "Đang hoạt động";
                bgColor = new Color(232, 245, 233);
                textColor = new Color(46, 125, 50);
            } else if (normalized.equalsIgnoreCase("MAINTENANCE") || normalized.toLowerCase().contains("bảo trì")) {
                displayText = "Đang bảo trì";
                bgColor = new Color(255, 243, 224);
                textColor = new Color(239, 108, 0);
            } else {
                bgColor = new Color(245, 245, 245);
                textColor = new Color(97, 97, 97);
            }
            setText(displayText);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBorder(new EmptyBorder(5, 12, 5, 12));
            setForeground(textColor);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private JLabel createStatLabel(String iconType, String text) {
        JLabel l = new JLabel(" " + text);
        l.setIcon(new SimpleIcon(iconType, 14, Color.GRAY));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(Color.GRAY);
        return l;
    }

    private JButton createIconButton(String iconType, Color color) {
        JButton btn = new JButton(new SimpleIcon(iconType, 22, color));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private class OccupancyBar extends JPanel {

        int percent, rented, total;

        public OccupancyBar(int percent, int rented, int total) {
            this.percent = percent;
            this.rented = rented;
            this.total = total;
            setPreferredSize(new Dimension(100, 45));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), arc = 12;
            g2.setColor(new Color(230, 230, 230));
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            int blueW = (int) (w * (percent / 100.0));
            if (blueW > 0) {
                g2.setColor(new Color(25, 118, 210));
                if (percent == 100) {
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                } else {
                    g2.fillRoundRect(0, 0, blueW, h, arc, arc);
                    g2.fillRect(blueW - arc, 0, arc, h);
                }
            }
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            if (percent > 0) {
                String txt = percent + "% Đang thuê (" + rented + " căn)";
                if (blueW > fm.stringWidth(txt) + 30) {
                    g2.setColor(Color.WHITE);
                    g2.drawString(txt, 20, (h + fm.getAscent()) / 2 - 2);
                }
            }
            String rightTxt = (100 - percent) + "% Trống";
            g2.setColor(new Color(66, 66, 66));
            if (w - blueW > fm.stringWidth(rightTxt) + 30) {
                g2.drawString(rightTxt, w - fm.stringWidth(rightTxt) - 20, (h + fm.getAscent()) / 2 - 2);
            }
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
            } else if ("BUILDING_COMPLEX".equals(type)) {
                g2.setStroke(new BasicStroke(1.5f));
                int baseY = size - 4;
                int midW = size * 36 / 100;
                int midH = size * 80 / 100;
                int midX = (size - midW) / 2;
                g2.fillRect(midX, baseY - midH, midW, midH);
                int leftW = size * 15 / 100;
                int leftH = size * 40 / 100;
                int leftX = midX - leftW - 2;
                g2.drawRect(leftX, baseY - leftH, leftW, leftH);
                g2.fillRect(leftX + leftW, baseY - leftH + 5, 2, leftH - 5);
                int rightW = size * 18 / 100;
                int rightH = size * 60 / 100;
                int rightX = midX + midW + 2;
                g2.drawRect(rightX, baseY - rightH, rightW, rightH);
                g2.setColor(Color.WHITE);
                int winSize = 4;
                int gap = 3;
                int startWX = midX + (midW - (3 * winSize + 2 * gap)) / 2;
                int startWY = baseY - midH + 8;
                for (int r = 0; r < 4; r++) {
                    for (int c1 = 0; c1 < 3; c1++) {
                        g2.fillRect(startWX + c1 * (winSize + gap), startWY + r * (winSize + gap), winSize, winSize);
                    }
                }
                g2.fillRect(midX + midW / 2 - 4, baseY - 10, 8, 10);
                g2.setColor(color);
                for (int i = 0; i < 3; i++) {
                    g2.fillRect(rightX + rightW / 2 - 2, baseY - rightH + 8 + (i * 8), 4, 4);
                }
                g2.drawLine(leftX - 2, baseY, rightX + rightW + 2, baseY);
            } else if ("USER".equals(type)) {
                g2.fillOval(size / 2 - 2, 0, 4, 4);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Arc2D.Double(size / 2.0 - 5, 5, 10, 10, 0, 180, Arc2D.OPEN));
            } else if ("FLOOR".equals(type)) {
                int w = size - 2;
                int h = 3;
                g2.fillRect(1, 2, w, h);
                g2.fillRect(1, 6, w, h);
                g2.fillRect(1, 10, w, h);
            } else if ("DOOR".equals(type)) {
                g2.drawRect(3, 1, 8, 12);
                g2.fillOval(9, 7, 2, 2);
            }
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
