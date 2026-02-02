package view;

import dao.FloorDAO;
import model.Floor;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.border.TitledBorder;

public class FloorDialog extends JDialog {

    private FloorDAO floorDAO;
    private Floor floor;
    private boolean confirmed = false;
    private boolean dataChanged = false;

    private JTextField txtName;
    private JTextField txtNumber;
    private JComboBox<String> cbbStatus;
    private JTextArea txtDesc;
    private JButton btnSave; // Khai báo nút Save để dùng cho phím tắt

    public FloorDialog(Frame owner, Floor floor) {
        super(owner, floor.getId() == null ? "Thêm Tầng Mới" : "Cập Nhật Tầng", true);
        this.floor = floor;
        this.floorDAO = new FloorDAO();

        initUI();
        fillData();

        dataChanged = false;

        setSize(500, 600); // Tăng chiều cao một chút để chứa status
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 250));

        // HEADER
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        JLabel titleLabel = new JLabel(floor.getId() == null ? "THIẾT LẬP TẦNG MỚI" : "THÔNG TIN TẦNG");
        titleLabel.setFont(UIConstants.FONT_HEADING);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // BODY
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(new Color(245, 245, 250));
        bodyPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        // Section 1: Thông tin chung
        JPanel pnlInfo = createSectionPanel("Thông Tin Chung");
        txtName = createRoundedField();
        txtNumber = createRoundedField();

        // [MỚI] Init Status ComboBox
        cbbStatus = new JComboBox<>(new String[]{"Đang hoạt động", "Đang bảo trì"});
        cbbStatus.setPreferredSize(new Dimension(100, 35));
        cbbStatus.setBackground(Color.WHITE);
        cbbStatus.setFont(UIConstants.FONT_REGULAR);

        JPanel gridPanel = new JPanel(new GridLayout(3, 1, 0, 15)); // Tăng lên 3 dòng
        gridPanel.setOpaque(false);
        gridPanel.add(createFieldGroup("Tên Tầng (VD: Tầng 1, Tầng G) (*)", txtName));
        gridPanel.add(createFieldGroup("Số Thứ Tự Tầng (Số nguyên) (*)", txtNumber));
        gridPanel.add(createFieldGroup("Trạng Thái Hoạt Động", cbbStatus)); // [MỚI]
        pnlInfo.add(gridPanel);

        // Section 2: Mô tả
        JPanel pnlDesc = createSectionPanel("Mô Tả / Ghi Chú");
        txtDesc = new JTextArea(6, 20);
        txtDesc.setFont(UIConstants.FONT_REGULAR);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);

        JScrollPane scrollDesc = new JScrollPane(txtDesc);
        scrollDesc.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        pnlDesc.add(createFieldGroup("Ghi chú thêm về tầng này", scrollDesc));

        SimpleDocumentListener docListener = new SimpleDocumentListener(() -> dataChanged = true);
        txtName.getDocument().addDocumentListener(docListener);
        txtNumber.getDocument().addDocumentListener(docListener);
        txtDesc.getDocument().addDocumentListener(docListener);
        cbbStatus.addActionListener(e -> dataChanged = true);

        bodyPanel.add(pnlInfo);
        bodyPanel.add(Box.createVerticalStrut(15));
        bodyPanel.add(pnlDesc);
        add(bodyPanel, BorderLayout.CENTER);

        // FOOTER
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new MatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JButton btnCancel = new RoundedButton("Hủy Bỏ", 10);
        btnCancel.setBackground(new Color(245, 245, 245));
        btnCancel.addActionListener(e -> handleCancel());

        btnSave = new RoundedButton("Lưu Tầng", 10);
        btnSave.setBackground(UIConstants.PRIMARY_COLOR);
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> onSave());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        add(buttonPanel, BorderLayout.SOUTH);

        configureShortcuts();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                txtName.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                handleCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    // --- CẤU HÌNH PHÍM TẮT (ENTER / ESC) ---
    private void configureShortcuts() {
        JRootPane rootPane = this.getRootPane();
        rootPane.setDefaultButton(btnSave);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        rootPane.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancel();
            }
        });
    }

    private void handleCancel() {
        if (dataChanged) {
            int choice = JOptionPane.showConfirmDialog(this, "Dữ liệu chưa lưu sẽ bị mất. Bạn có chắc muốn thoát?", "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
            }
        } else {
            dispose();
        }
    }

    // --- LOGIC LƯU DỮ LIỆU ---
    private void onSave() {
        String name = txtName.getText().trim();
        String numStr = txtNumber.getText().trim();
        String uiStatus = (String) cbbStatus.getSelectedItem();

        if (name.isEmpty() || numStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Thiếu thông tin!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int num = Integer.parseInt(numStr);
            if (floorDAO.isFloorNumberExists(floor.getBuildingId(), num, floor.getId())) {
                JOptionPane.showMessageDialog(this, "Số tầng đã tồn tại!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // [LOGIC CHẶN BẢO TRÌ]
            String oldStatus = (floor.getStatus() == null) ? "ACTIVE" : floor.getStatus();
            String newStatus = "Đang bảo trì".equals(uiStatus) ? "MAINTENANCE" : "ACTIVE";
            boolean statusChanged = !newStatus.equals(oldStatus) && floor.getId() != null;

            if ("MAINTENANCE".equals(newStatus) && floor.getId() != null) {
                // Kiểm tra nếu có hợp đồng đang active
                if (floorDAO.hasActiveContracts(floor.getId())) {
                    JOptionPane.showMessageDialog(this,
                            "KHÔNG THỂ BẢO TRÌ TẦNG NÀY!\n\n"
                            + "Lý do: Tầng đang có căn hộ được thuê (Active).\n"
                            + "Vui lòng thanh lý hợp đồng trước.",
                            "Xung đột", JOptionPane.ERROR_MESSAGE);

                    // Reset UI về Active
                    cbbStatus.setSelectedItem("Đang hoạt động");
                    return;
                }
            }

            // Hỏi xác nhận
            String msg = "Lưu thông tin?";
            if (statusChanged) {
                if ("MAINTENANCE".equals(newStatus)) {
                    msg = "<html><b>CẢNH BÁO:</b> Chuyển sang BẢO TRÌ sẽ chuyển tất cả căn hộ sang Bảo trì.<br>Tiếp tục?</html>";
                } else if ("ACTIVE".equals(newStatus)) {
                    msg = "<html><b>KÍCH HOẠT LẠI:</b> Sẽ mở khóa tất cả căn hộ (về Trống).<br>Tiếp tục?</html>";
                }
            }
            if (JOptionPane.showConfirmDialog(this, msg, "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            floor.setName(name);
            floor.setFloorNumber(num);
            floor.setDescription(txtDesc.getText());
            floor.setStatus(newStatus);

            boolean ok;
            if (floor.getId() == null) {
                ok = floorDAO.insertFloor(floor);
            } else {
                if (statusChanged) {
                    floorDAO.updateFloor(floor);
                    ok = floorDAO.updateStatusCascade(floor.getId(), newStatus);
                } else {
                    ok = floorDAO.updateFloor(floor);
                }
            }

            if (ok) {
                confirmed = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi lưu dữ liệu!");
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tầng phải là số!");
        }
    }

    private void fillData() {
        if (floor.getId() != null) {
            txtName.setText(floor.getName());
            txtNumber.setText(String.valueOf(floor.getFloorNumber()));
            txtDesc.setText(floor.getDescription());

            // Map DB Status -> UI
            String dbStatus = floor.getStatus();
            if (dbStatus != null && (dbStatus.equalsIgnoreCase("MAINTENANCE") || dbStatus.contains("bảo trì"))) {
                cbbStatus.setSelectedItem("Đang bảo trì");
            } else {
                cbbStatus.setSelectedItem("Đang hoạt động");
            }
        } else {
            // Mặc định khi thêm mới
            txtNumber.setText("");
            cbbStatus.setSelectedItem("Đang hoạt động");
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Floor getFloor() {
        return floor;
    }

    // --- Helpers UI ---
    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(new LineBorder(new Color(220, 220, 220), 1, true), title);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
        border.setTitleColor(UIConstants.PRIMARY_COLOR);
        panel.setBorder(new CompoundBorder(border, new EmptyBorder(10, 15, 10, 15)));
        return panel;
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(80, 80, 80));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JTextField createRoundedField() {
        JTextField f = new RoundedTextField(8);
        f.setPreferredSize(new Dimension(100, 35));
        f.setFont(UIConstants.FONT_REGULAR);
        return f;
    }

    private static class SimpleDocumentListener implements DocumentListener {

        private Runnable onChange;

        public SimpleDocumentListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onChange.run();
        }
    }

    private static class RoundedTextField extends JTextField {

        private int arc;

        public RoundedTextField(int arc) {
            this.arc = arc;
            setOpaque(false);
            setBorder(new EmptyBorder(5, 10, 5, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
            g2.setColor(new Color(180, 180, 180));
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
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
            g2.dispose();
        }
    }
}
