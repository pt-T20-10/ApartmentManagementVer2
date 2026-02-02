package view;

import dao.BuildingDAO;
import dao.UserDAO;
import model.Building;
import model.User;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.util.List;

public class BuildingDialog extends JDialog {

    private JTextField txtName, txtAddress;
    private JComboBox<User> cbbManager;  // ✅ Đúng tên biến
    private JComboBox<String> cbbStatus;
    private JTextArea txtDesc;

    private boolean confirmed = false;
    private Building building;
    private boolean dataChanged = false;

    private UserDAO userDAO;
    private BuildingDAO buildingDAO;

    public BuildingDialog(Frame owner, Building building) {
        super(owner, building == null || building.getId() == null ? "Thêm Mới Tòa Nhà" : "Chi Tiết Tòa Nhà", true);
        this.building = (building == null) ? new Building() : building;
        this.userDAO = new UserDAO();
        this.buildingDAO = new BuildingDAO();

        initUI();
        loadManagers();  // ✅ Load managers AFTER initUI
        fillData();

        dataChanged = false;

        setSize(650, 680);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 250));

        // === HEADER ===
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));

   
        JLabel titleLabel = new JLabel(building.getId() == null ? "THIẾT LẬP TÒA NHÀ MỚI" : "CẬP NHẬT THÔNG TIN");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

   
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // === BODY ===
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(new Color(245, 245, 250));
        bodyPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Init Fields
        txtName = createRoundedField();
        txtAddress = createRoundedField();

        // ✅ Cấu hình Dropdown Manager
        cbbManager = new JComboBox<>();
        cbbManager.setFont(UIConstants.FONT_REGULAR);
        cbbManager.setBackground(Color.WHITE);
        cbbManager.setPreferredSize(new Dimension(200, 35));

        // ✅ Custom Renderer - hiển thị tên + username
        cbbManager.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User) {
                    User u = (User) value;
                    // ✅ Hiển thị: Tên (username)
                    setText(u.getFullName() + " (" + u.getUsername() + ")");
                }
                return this;
            }
        });

        cbbStatus = new JComboBox<>(new String[]{"Đang hoạt động", "Đang bảo trì"});
        cbbStatus.setFont(UIConstants.FONT_REGULAR);
        cbbStatus.setBackground(Color.WHITE);
        cbbStatus.setPreferredSize(new Dimension(200, 35));

        txtDesc = new JTextArea(8, 20);
        txtDesc.setFont(UIConstants.FONT_REGULAR);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDesc);
        scrollDesc.setBorder(new LineBorder(new Color(200, 200, 200), 1));

        // Listeners
        SimpleDocumentListener docListener = new SimpleDocumentListener(() -> dataChanged = true);
        txtName.getDocument().addDocumentListener(docListener);
        txtAddress.getDocument().addDocumentListener(docListener);
        txtDesc.getDocument().addDocumentListener(docListener);
        cbbStatus.addActionListener(e -> dataChanged = true);
        cbbManager.addActionListener(e -> dataChanged = true);

        // Layout Form
        JPanel pnlGeneral = createSectionPanel("Thông Tin Cơ Bản");
        JPanel row1 = new JPanel(new GridLayout(1, 2, 15, 0));
        row1.setOpaque(false);
        row1.add(createFieldGroup("Tên Tòa Nhà (*)", txtName));
        row1.add(createFieldGroup("Trạng Thái", cbbStatus));
        pnlGeneral.add(row1);
        pnlGeneral.add(Box.createVerticalStrut(15));

        pnlGeneral.add(createFieldGroup("Địa Chỉ Chi Tiết (*)", txtAddress));

        JPanel pnlExtra = createSectionPanel("Quản Lý & Ghi Chú");
        pnlExtra.add(createFieldGroup("Người Quản Lý (*)", cbbManager));
        pnlExtra.add(Box.createVerticalStrut(15));
        pnlExtra.add(createFieldGroup("Mô Tả Thêm", scrollDesc));

        bodyPanel.add(pnlGeneral);
        bodyPanel.add(Box.createVerticalStrut(15));
        bodyPanel.add(pnlExtra);
        add(bodyPanel, BorderLayout.CENTER);

        // === FOOTER ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new MatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JButton btnCancel = new RoundedButton("Đóng", 10);
        btnCancel.setBackground(new Color(245, 245, 245));
        btnCancel.setPreferredSize(new Dimension(100, 38));
        btnCancel.addActionListener(e -> handleCancel());

        JButton btnSave = new RoundedButton("Lưu Thay Đổi", 10);
        btnSave.setBackground(UIConstants.PRIMARY_COLOR);
        btnSave.setForeground(Color.WHITE);
        btnSave.setIcon(new SimpleIcon("CHECK", 12, Color.WHITE));
        btnSave.setPreferredSize(new Dimension(140, 38));
        btnSave.addActionListener(e -> onSave());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        add(buttonPanel, BorderLayout.SOUTH);

        configureShortcuts(btnSave);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCancel();
            }
        });
    }

    // ✅ FIX: Load ONLY MANAGER users
    private void loadManagers() {
        cbbManager.removeAllItems();

        try {
            // ✅ Thêm option "Không chọn" ở đầu
            User emptyOption = new User();
            emptyOption.setId(null);
            emptyOption.setFullName("-- Chọn người quản lý --");
            emptyOption.setUsername("");
            cbbManager.addItem(emptyOption);

            // ✅ Load ONLY MANAGER users
            List<User> managers = userDAO.getAllManagers();

            for (User manager : managers) {
                cbbManager.addItem(manager);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi tải danh sách người quản lý: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configureShortcuts(JButton defaultButton) {
        getRootPane().setDefaultButton(defaultButton);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancel();
            }
        });
    }

    private void handleCancel() {
        if (dataChanged) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc muốn đóng? Dữ liệu chưa lưu sẽ bị mất.",
                    "Xác nhận đóng",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
    }

    private void onSave() {
        // 1. Lấy dữ liệu từ Form
        String name = txtName.getText().trim();
        String address = txtAddress.getText().trim();
        User selectedManager = (User) cbbManager.getSelectedItem();
        String uiStatus = (String) cbbStatus.getSelectedItem();

        // 2. Validation (Kiểm tra dữ liệu)
        StringBuilder errors = new StringBuilder();
        if (name.isEmpty()) {
            errors.append("- Tên tòa nhà không được để trống.\n");
        }
        if (address.isEmpty()) {
            errors.append("- Địa chỉ không được để trống.\n");
        }

        // ✅ Check nếu chọn option "-- Chọn người quản lý --"
        if (selectedManager == null || selectedManager.getId() == null) {
            errors.append("- Vui lòng chọn Người quản lý.\n");
        }

        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng kiểm tra lại:\n" + errors.toString(),
                    "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. Chuẩn bị Trạng thái Mới & Cũ
        String oldStatus = (building.getStatus() == null) ? "ACTIVE" : building.getStatus();
        String newStatus = "ACTIVE"; // Mặc định

        if ("Đang bảo trì".equals(uiStatus)) {
            newStatus = "MAINTENANCE";
        } else if ("Ngưng sử dụng".equals(uiStatus)) {
            newStatus = "INACTIVE";
        } else if ("Đang hoạt động".equals(uiStatus)) {
            newStatus = "ACTIVE";
        }

        boolean isStatusChanged = !newStatus.equals(oldStatus) && building.getId() != null;

        // 4. [LOGIC CHẶN] Kiểm tra điều kiện nếu chuyển sang BẢO TRÌ
        if ("MAINTENANCE".equals(newStatus) && building.getId() != null) {
            if (buildingDAO.hasActiveContracts(building.getId())) {
                JOptionPane.showMessageDialog(this,
                        "KHÔNG THỂ CHUYỂN SANG BẢO TRÌ!\n\n"
                        + "Lý do: Tòa nhà đang có hợp đồng thuê ACTIVE (Đang hiệu lực).\n"
                        + "Bạn phải thanh lý hết hợp đồng trước khi bảo trì tòa nhà.",
                        "Xung đột trạng thái",
                        JOptionPane.ERROR_MESSAGE);

                fillData(); // Reset lại UI về trạng thái cũ
                return;
            }
        }

        // 5. Tạo thông báo xác nhận (Tùy biến theo hành động)
        String confirmMsg = "Bạn có chắc chắn muốn lưu thông tin này?";
        int msgType = JOptionPane.QUESTION_MESSAGE;

        if (isStatusChanged) {
            if ("MAINTENANCE".equals(newStatus)) {
                confirmMsg = "<html><b>CẢNH BÁO QUAN TRỌNG:</b><br>"
                        + "Bạn đang chuyển trạng thái sang <b>ĐANG BẢO TRÌ</b>.<br>"
                        + "- Tất cả Tầng và Căn hộ trong tòa sẽ tự động chuyển sang Bảo trì.<br>"
                        + "- Các chức năng thuê mới sẽ bị khóa.<br><br>"
                        + "Bạn có chắc chắn muốn tiếp tục?</html>";
                msgType = JOptionPane.WARNING_MESSAGE;
            } else if ("ACTIVE".equals(newStatus) && "MAINTENANCE".equals(oldStatus)) {
                confirmMsg = "<html><b>KÍCH HOẠT LẠI TÒA NHÀ:</b><br>"
                        + "Hệ thống sẽ mở khóa (Set Active/Available) cho tất cả Tầng và Căn hộ.<br>"
                        + "Tiếp tục?</html>";
                msgType = JOptionPane.INFORMATION_MESSAGE;
            }
        }

        int choice = JOptionPane.showConfirmDialog(this, confirmMsg, "Xác nhận lưu", JOptionPane.YES_NO_OPTION, msgType);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        // 6. Cập nhật dữ liệu vào Object
        building.setName(name);
        building.setAddress(address);
        building.setDescription(txtDesc.getText().trim());
        building.setStatus(newStatus);

        // ✅ Set manager info
        if (selectedManager != null && selectedManager.getId() != null) {
            building.setManagerUserId(selectedManager.getId());
            building.setManagerName(selectedManager.getFullName());
        }

        if (building.getId() == null) {
            building.setDeleted(false);
        }

        // 7. Gọi DAO để Lưu xuống Database
        boolean success = false;

        if (building.getId() == null) {
            // Trường hợp: THÊM MỚI
            success = buildingDAO.insertBuilding(building);
        } else {
            // Trường hợp: CẬP NHẬT
            if (isStatusChanged) {
                // Nếu đổi trạng thái -> Gọi hàm Cascade (Cập nhật lan truyền xuống Tầng/Căn hộ)
                buildingDAO.updateBuilding(building); // Lưu tên, địa chỉ...
                success = buildingDAO.updateStatusCascade(building.getId(), newStatus); // Lưu status + lan truyền
            } else {
                // Nếu chỉ sửa tên/địa chỉ bình thường
                success = buildingDAO.updateBuilding(building);
            }
        }

        // 8. Kết thúc
        if (success) {
            confirmed = true;
            dataChanged = false;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Lưu dữ liệu thất bại! Vui lòng thử lại.", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillData() {
        if (building.getId() != null) {
            txtName.setText(building.getName());
            txtAddress.setText(building.getAddress());
            txtDesc.setText(building.getDescription());
            String dbStatus = building.getStatus();

            // Map sang tiếng Việt để ComboBox hiển thị đúng
            if (dbStatus != null) {
                if (dbStatus.equalsIgnoreCase("ACTIVE")) {
                    cbbStatus.setSelectedItem("Đang hoạt động");
                } else if (dbStatus.equalsIgnoreCase("MAINTENANCE") || dbStatus.toLowerCase().contains("bảo trì")) {
                    cbbStatus.setSelectedItem("Đang bảo trì");
                }
            }

            // ✅ Chọn đúng Manager dựa trên ID
            Long mgrId = building.getManagerUserId();

            if (mgrId != null) {
                for (int i = 0; i < cbbManager.getItemCount(); i++) {
                    User u = cbbManager.getItemAt(i);
                    if (u.getId() != null && u.getId().equals(mgrId)) {
                        cbbManager.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Building getBuilding() {
        return building;
    }

    // --- UI Helpers ---
    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(new LineBorder(new Color(220, 220, 220), 1, true), title);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        border.setTitleColor(UIConstants.PRIMARY_COLOR);
        panel.setBorder(new CompoundBorder(border, new EmptyBorder(15, 20, 15, 20)));
        return panel;
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
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
        f.setFont(UIConstants.FONT_REGULAR);
        f.setPreferredSize(new Dimension(100, 35));
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
            g2.setColor(Color.WHITE);
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
            g2.setColor(getModel().isArmed() ? getBackground().darker() : getBackground());
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
            g2.translate(x, y);
            if ("BUILDING_BIG".equals(type)) {
                g2.fillRect(size / 4, size / 4, size / 2, size / 2);
            } else if ("CHECK".equals(type)) {
                g2.setStroke(new BasicStroke(2.5f));
                Path2D p = new Path2D.Float();
                p.moveTo(2, size / 2);
                p.lineTo(size / 2 - 2, size - 3);
                p.lineTo(size - 2, 3);
                g2.draw(p);
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
