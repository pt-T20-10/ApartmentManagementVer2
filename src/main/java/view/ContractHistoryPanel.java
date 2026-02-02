package view;

import dao.ContractHistoryDAO;
import model.ContractHistory;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Contract History - Modern Timeline UI Layout: Timestamp -> Node Line ->
 * Detail Card
 */
public class ContractHistoryPanel extends JPanel {

    private ContractHistoryDAO historyDAO;
    private Long contractId;

    // Formatters
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private JPanel timelineContainer;
    private JLabel countLabel;

    // Colors (Sky Blue Theme)
    private final Color BG_COLOR = new Color(241, 245, 249); // Slate 100
    private final Color LINE_COLOR = new Color(203, 213, 225); // Slate 300
    private final Color CARD_BG = Color.WHITE;

    public ContractHistoryPanel(Long contractId) {
        this.contractId = contractId;
        this.historyDAO = new ContractHistoryDAO();

        setLayout(new BorderLayout());
        setBackground(BG_COLOR);

        initComponents();
        loadHistory();
    }

    private void initComponents() {
        // 1. Header
        add(createHeader(), BorderLayout.NORTH);

        // 2. Timeline Scroll Area
        timelineContainer = new JPanel();
        timelineContainer.setLayout(new BoxLayout(timelineContainer, BoxLayout.Y_AXIS));
        timelineContainer.setBackground(BG_COLOR);
        timelineContainer.setBorder(new EmptyBorder(20, 20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(timelineContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(0, 70));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        // Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        left.setOpaque(false);

        JLabel icon = new JLabel("🕒");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel title = new JLabel("Dòng Thời Gian");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(30, 41, 59));

        left.add(icon);
        left.add(title);

        // Count Badge
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        right.setOpaque(false);
        countLabel = new JLabel("0 sự kiện");
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        countLabel.setForeground(new Color(100, 116, 139));
        right.add(countLabel);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    public void loadHistory() {
        timelineContainer.removeAll();

        List<ContractHistory> histories = historyDAO.getHistoryByContractWithUser(contractId);
        countLabel.setText(histories.size() + " sự kiện");

        if (histories.isEmpty()) {
            timelineContainer.add(createEmptyState());
        } else {
            for (int i = 0; i < histories.size(); i++) {
                ContractHistory history = histories.get(i);
                boolean isLast = (i == histories.size() - 1);

                // Thêm Timeline Item
                timelineContainer.add(new TimelineItem(history, isLast));

                // Spacer
                if (!isLast) {
                    timelineContainer.add(Box.createVerticalStrut(0));
                }
            }
        }

        timelineContainer.revalidate();
        timelineContainer.repaint();
    }

    private JPanel createEmptyState() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(50, 0, 0, 0));

        JLabel lbl = new JLabel("Chưa có lịch sử ghi lại");
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lbl.setForeground(new Color(148, 163, 184));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(lbl);
        return panel;
    }

    // =========================================================================
    // INNER CLASS: TIMELINE ITEM (THE CORE VISUAL COMPONENT)
    // =========================================================================
    private class TimelineItem extends JPanel {

        private final ContractHistory history;
        private final boolean isLast;
        private final Color actionColor;

        public TimelineItem(ContractHistory history, boolean isLast) {
            this.history = history;
            this.isLast = isLast;
            this.actionColor = getActionColor(history.getAction());

            setLayout(new BorderLayout());
            setOpaque(false);

            add(createLeftTime(), BorderLayout.WEST);
            add(createCenterLine(), BorderLayout.CENTER); // Line container actually holds right content too via overlay logic or layout tricks
            // To simplify Swing layout: West (Time), Center (Graphic + Content)
        }

        // Cột thời gian (Bên trái)
        private JPanel createLeftTime() {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setOpaque(false);
            p.setPreferredSize(new Dimension(80, 100));
            p.setBorder(new EmptyBorder(15, 0, 0, 10)); // Top padding align with dot

            JLabel lblDate = new JLabel(dateFormat.format(history.getCreatedAt()));
            lblDate.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblDate.setForeground(new Color(51, 65, 85));
            lblDate.setAlignmentX(Component.RIGHT_ALIGNMENT);

            JLabel lblTime = new JLabel(timeFormat.format(history.getCreatedAt()));
            lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblTime.setForeground(new Color(148, 163, 184));
            lblTime.setAlignmentX(Component.RIGHT_ALIGNMENT);

            p.add(lblDate);
            p.add(lblTime);
            return p;
        }

        // Cột giữa (Đường kẻ + Chấm) và Cột phải (Nội dung) kết hợp
        private JPanel createCenterLine() {
            return new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int dotSize = 14;
                    int lineX = 15; // Vị trí trục dọc
                    int dotY = 20;  // Vị trí chấm tròn (thẳng hàng với text)

                    // 1. Vẽ đường kẻ (Timeline Line)
                    if (!isLast) {
                        g2.setColor(LINE_COLOR);
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawLine(lineX, dotY, lineX, getHeight());
                    } else {
                        // Nếu là cái cuối, vẽ mờ dần hoặc ngắn hơn
                        g2.setColor(LINE_COLOR);
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawLine(lineX, 0, lineX, dotY); // Nối từ trên xuống chấm
                    }

                    // 2. Vẽ chấm tròn (Node)
                    // Outer glow
                    g2.setColor(new Color(actionColor.getRed(), actionColor.getGreen(), actionColor.getBlue(), 50));
                    g2.fillOval(lineX - dotSize / 2 - 3, dotY - dotSize / 2 - 3, dotSize + 6, dotSize + 6);

                    // Inner dot
                    g2.setColor(actionColor);
                    g2.fillOval(lineX - dotSize / 2, dotY - dotSize / 2, dotSize, dotSize);

                    // Center white point
                    g2.setColor(Color.WHITE);
                    g2.fillOval(lineX - 2, dotY - 2, 4, 4);
                }

                {
                    setLayout(new BorderLayout());
                    setOpaque(false);
                    setBorder(new EmptyBorder(0, 40, 15, 0)); // Chừa chỗ cho đường kẻ (Left padding)

                    // Add Card Content here
                    add(createContentCard());
                }
            };
        }

        private JPanel createContentCard() {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240)),
                    new EmptyBorder(12, 15, 12, 15)
            ));

            // Header: Action Name + User
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            JLabel lblAction = new JLabel(getActionLabel(history.getAction()));
            lblAction.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblAction.setForeground(actionColor);

            String userName = history.getCreatedByName() != null ? history.getCreatedByName() : "Hệ thống";
            JLabel lblUser = new JLabel(userName);
            lblUser.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblUser.setForeground(new Color(148, 163, 184));
            lblUser.setIcon(new UserIcon(12));

            header.add(lblAction, BorderLayout.WEST);
            header.add(lblUser, BorderLayout.EAST);

            // Body: Description
            String descText = getDescription(history);
            JLabel lblDesc = new JLabel("<html>" + descText + "</html>");
            lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblDesc.setForeground(new Color(51, 65, 85));
            lblDesc.setBorder(new EmptyBorder(5, 0, 0, 0));

            card.add(header, BorderLayout.NORTH);
            card.add(lblDesc, BorderLayout.CENTER);

            return card;
        }
    }

    // =========================================================================
    // HELPERS & UTILS
    // =========================================================================
    private String getDescription(ContractHistory history) {
        String desc = history.getReason();
        if (desc == null) {
            desc = "";
        }

        // Thêm chi tiết ngày tháng nếu là gia hạn
        if (history.getOldEndDate() != null && history.getNewEndDate() != null) {
            String dates = String.format("<br><span style='color:#64748b; font-size:10px'>Gia hạn: %s ➝ %s</span>",
                    dateFormat.format(history.getOldEndDate()),
                    dateFormat.format(history.getNewEndDate())
            );
            desc += dates;
        }
        return desc;
    }

    private String getActionLabel(String action) {
        if (action == null) {
            return "KHÁC";
        }
        switch (action.toUpperCase()) {
            case "CREATED":
                return "TẠO MỚI";
            case "RENEWED":
            case "EXTENDED":
                return "GIA HẠN";
            case "UPDATED":
                return "CẬP NHẬT";
            case "TERMINATED":
                return "THANH LÝ";
            case "DELETED":
                return "ĐÃ XÓA";
            case "STATUS_CHANGED":
                return "ĐỔI TRẠNG THÁI";
            default:
                return action;
        }
    }

    private Color getActionColor(String action) {
        if (action == null) {
            return Color.GRAY;
        }
        switch (action.toUpperCase()) {
            case "CREATED":
                return new Color(34, 197, 94);  // Green
            case "RENEWED":
            case "EXTENDED":
                return new Color(59, 130, 246); // Blue
            case "UPDATED":
                return new Color(245, 158, 11);  // Amber
            case "TERMINATED":
            case "DELETED":
                return new Color(239, 68, 68);   // Red
            default:
                return new Color(100, 116, 139);        // Slate
        }
    }

    // Mini Icon for User
    private static class UserIcon implements Icon {

        int size;

        public UserIcon(int s) {
            size = s;
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(148, 163, 184));
            g2.fillOval(x, y, size, size); // Head
            // Simple dot representation
        }
    }

    public void refresh() {
        loadHistory();
    }
}
