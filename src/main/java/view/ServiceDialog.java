package view;

import model.Service;
import util.MoneyFormatter;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Service Dialog - Fixed Height & Layout
 */
public class ServiceDialog extends JDialog {

    private JTextField txtName;
    private JTextField txtUnit;
    private JTextField txtPrice; // Money formatted
    private JCheckBox chkMandatory;

    private Service service;
    private boolean confirmed = false;

    public ServiceDialog(JFrame parent) {
        this(parent, null);
    }

    public ServiceDialog(JFrame parent, Service service) {
        super(parent, service == null ? "Thêm Dịch Vụ Mới" : "Cập Nhật Dịch Vụ", true);
        this.service = service;

        initUI();
        if (service != null) {
            loadData();
        }

        // ✅ TĂNG CHIỀU CAO ĐỂ KHÔNG BỊ CHE NỘI DUNG
        setSize(550, 580);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // 1. Header (Màu xanh hiện đại)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(33, 150, 243)); // Primary Blue
        header.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel lblTitle = new JLabel(service == null ? "Thêm Dịch Vụ" : "Cập Nhật Dịch Vụ");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(Color.WHITE);
        // Icon trắng đơn giản
        lblTitle.setIcon(new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(x, y + 10, 24, 4); // Ngang
                g2.fillRect(x + 10, y, 4, 24); // Dọc
                g2.dispose();
            }

            public int getIconWidth() {
                return 24;
            }

            public int getIconHeight() {
                return 24;
            }
        });

        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // 2. Form Body
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(25, 35, 25, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0); // Khoảng cách giữa các dòng
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Row 1: Tên dịch vụ
        gbc.gridy = 0;
        form.add(createLabel("Tên dịch vụ"), gbc);
        gbc.gridy = 1;
        txtName = createStyledTextField();
        form.add(txtName, gbc);

        // Row 2: Đơn vị tính
        gbc.gridy = 2;
        form.add(createLabel("Đơn vị tính (VD: kWh, m3)"), gbc);
        gbc.gridy = 3;
        txtUnit = createStyledTextField();
        form.add(txtUnit, gbc);

        // Row 3: Đơn giá
        gbc.gridy = 4;
        form.add(createLabel("Đơn giá (VNĐ)"), gbc);
        gbc.gridy = 5;
        txtPrice = MoneyFormatter.createMoneyField(45); // Height 45px
        styleTextField(txtPrice); // Apply border styles
        form.add(txtPrice, gbc);

        // Row 4: Checkbox
        gbc.gridy = 6;
        gbc.insets = new Insets(20, 0, 20, 0); // ✅ Tăng khoảng cách trên dưới cho checkbox
        chkMandatory = new JCheckBox("Dịch vụ bắt buộc (Áp dụng cho mọi hợp đồng)");
        chkMandatory.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chkMandatory.setBackground(Color.WHITE);
        chkMandatory.setFocusPainted(false);
        chkMandatory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        form.add(chkMandatory, gbc);

        // Filler (Đẩy nội dung lên trên nếu còn trống)
        gbc.gridy = 7;
        gbc.weighty = 1.0;
        form.add(Box.createGlue(), gbc);

        add(form, BorderLayout.CENTER);

        // 3. Footer Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        footer.setPreferredSize(new Dimension(0, 80)); // Tăng chiều cao footer một chút

        JButton btnCancel = createButton("Hủy", new Color(226, 232, 240), new Color(71, 85, 105));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = createButton("Lưu Dịch Vụ", new Color(33, 150, 243), Color.WHITE);
        btnSave.addActionListener(e -> save());

        footer.add(btnCancel);
        footer.add(btnSave);

        add(footer, BorderLayout.SOUTH);
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(10, 15, 10, 15) // Padding trong text field
        ));
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(0, 45)); // Cao hơn (45px)
        styleTextField(field);
        return field;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(100, 116, 139));
        label.setBorder(new EmptyBorder(0, 0, 5, 0));
        return label;
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(130, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void loadData() {
        txtName.setText(service.getName());
        txtUnit.setText(service.getUnit());
        MoneyFormatter.setValue(txtPrice, service.getUnitPrice().longValue());
        chkMandatory.setSelected(service.isMandatory());
    }

    private void save() {
        if (txtName.getText().trim().isEmpty() || txtUnit.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long priceVal = MoneyFormatter.getValue(txtPrice);
        if (priceVal == null || priceVal <= 0) {
            JOptionPane.showMessageDialog(this, "Đơn giá không hợp lệ!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (service == null) {
            service = new Service();
        }

        service.setName(txtName.getText().trim());
        service.setUnit(txtUnit.getText().trim());
        service.setUnitPrice(BigDecimal.valueOf(priceVal));
        service.setMandatory(chkMandatory.isSelected());

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Service getService() {
        return service;
    }
}
