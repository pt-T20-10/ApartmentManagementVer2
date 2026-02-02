package view;

import dao.FloorDAO;
import model.Floor;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class BatchAddFloorDialog extends JDialog {

    private JTextField txtFrom, txtTo, txtPrefix;
    private JLabel lblPreview;
    private Long buildingId;
    private FloorDAO floorDAO;
    private boolean isSuccess = false;

    public BatchAddFloorDialog(Frame owner, Long buildingId) {
        super(owner, "Thêm Tầng Hàng Loạt", true);
        this.buildingId = buildingId;
        this.floorDAO = new FloorDAO();

        initUI();
        setSize(450, 480);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // === HEADER ===
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        headerPanel.setBackground(new Color(240, 247, 255));
        headerPanel.setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));

        JLabel iconLabel = new JLabel(new SimpleIcon("LAYER_PLUS", 32, UIConstants.PRIMARY_COLOR));
        JLabel titleLabel = new JLabel("THÊM TẦNG HÀNG LOẠT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        headerPanel.add(iconLabel);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // === FORM BODY ===
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(25, 40, 20, 40));

        txtFrom = createRoundedField();
        txtTo = createRoundedField();
        txtPrefix = createRoundedField();
        txtPrefix.setText("Tầng ");

        JPanel rowRange = new JPanel(new GridLayout(1, 2, 20, 0));
        rowRange.setBackground(Color.WHITE);
        rowRange.add(createFieldGroup("Từ Số (VD: 1)", txtFrom));
        rowRange.add(createFieldGroup("Đến Số (VD: 10)", txtTo));

        contentPanel.add(rowRange);
        contentPanel.add(Box.createVerticalStrut(20));

        contentPanel.add(createFieldGroup("Tên Hiển Thị (Tiền tố)", txtPrefix));
        contentPanel.add(Box.createVerticalStrut(10));

        lblPreview = new JLabel("Hệ thống sẽ tự động bỏ qua nếu tên tầng đã tồn tại.");
        lblPreview.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblPreview.setForeground(new Color(255, 152, 0)); // Màu cam cảnh báo
        contentPanel.add(lblPreview);

        add(contentPanel, BorderLayout.CENTER);

        // === FOOTER ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(new Color(250, 250, 250));
        buttonPanel.setBorder(new MatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JButton btnCancel = new RoundedButton("Hủy", 10);
        btnCancel.setBackground(new Color(245, 245, 245));
        btnCancel.setForeground(Color.BLACK);
        btnCancel.setPreferredSize(new Dimension(100, 38));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new RoundedButton("Tạo Nhanh", 10);
        btnSave.setBackground(UIConstants.PRIMARY_COLOR);
        btnSave.setForeground(Color.WHITE);
        btnSave.setPreferredSize(new Dimension(120, 38));
        btnSave.addActionListener(e -> onSave());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void onSave() {
        String fromStr = txtFrom.getText().trim();
        String toStr = txtTo.getText().trim();
        String prefix = txtPrefix.getText().trim();

        if (fromStr.isEmpty() || toStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập khoảng tầng!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int from = Integer.parseInt(fromStr);
            int to = Integer.parseInt(toStr);

            if (from > to) {
                JOptionPane.showMessageDialog(this, "Số bắt đầu phải nhỏ hơn số kết thúc!", "Lỗi logic", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ((to - from) > 100) {
                JOptionPane.showMessageDialog(this, "Chỉ được tạo tối đa 100 tầng một lần!", "Giới hạn", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // HỎI XÁC NHẬN TRƯỚC KHI CHẠY BATCH
            int choice = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn tạo " + (to - from + 1) + " tầng không?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION);

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            int successCount = 0;
            int skipCount = 0;

            for (int i = from; i <= to; i++) {
                String floorName = prefix + " " + i; // VD: "Tầng 1"

                // Kiểm tra trùng tên từ DAO
                if (floorDAO.isFloorNameExists(buildingId, floorName)) {
                    skipCount++;
                    continue;
                }

                Floor f = new Floor();
                f.setBuildingId(buildingId);
                f.setFloorNumber(i);
                f.setName(floorName);

                if (floorDAO.insertFloor(f)) {
                    successCount++;
                }
            }

            String msg = "Đã thêm thành công: " + successCount + " tầng.";
            if (skipCount > 0) {
                msg += "\nĐã bỏ qua " + skipCount + " tầng do trùng tên.";
            }

            JOptionPane.showMessageDialog(this, msg, "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
            isSuccess = true;
            dispose();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tầng phải là số nguyên!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    // Helpers UI
    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(100, 100, 100));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JTextField createRoundedField() {
        JTextField f = new RoundedTextField(10);
        f.setFont(UIConstants.FONT_REGULAR);
        f.setPreferredSize(new Dimension(100, 38));
        return f;
    }

    private static class RoundedTextField extends JTextField {

        private int arc;

        public RoundedTextField(int arc) {
            this.arc = arc;
            setOpaque(false);
            setBorder(new EmptyBorder(5, 12, 5, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
            g2.setColor(new Color(200, 200, 200));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
            super.paintComponent(g);
            g2.dispose();
        }
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
            setFont(new Font("Segoe UI", Font.BOLD, 13));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isArmed()) {
                g2.setColor(getBackground().darker());
            } else {
                g2.setColor(getBackground());
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
            g2.dispose();
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
            g2.setStroke(new BasicStroke(2f));
            g2.translate(x, y);
            if ("LAYER_PLUS".equals(type)) {
                int w = size - 8;
                int h = size / 4;
                g2.drawRoundRect(4, size / 2 - 4, w, h, 3, 3);
                g2.drawRoundRect(4, size / 2 + 4, w, h, 3, 3);
                g2.drawLine(size / 2, 2, size / 2, 10);
                g2.drawLine(size / 2 - 4, 6, size / 2 + 4, 6);
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
