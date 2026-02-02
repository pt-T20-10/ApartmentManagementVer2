package view;

import dao.ApartmentDAO;
import dao.FloorDAO;
import model.Apartment;
import model.Floor;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class ApartmentDialog extends JDialog {

    private FloorDAO floorDAO;
    private ApartmentDAO apartmentDAO;

    private JComboBox<Floor> cbbFloor;
    private JTextField txtRoomNumber;
    private JTextField txtArea;

    private JComboBox<String> cbbType;
    private JSpinner spnBedrooms;
    private JSpinner spnBathrooms;

    private JComboBox<String> cbbStatus; // Tiếng Việt
    private JTextArea txtDesc;

    private boolean confirmed = false;
    private Apartment apartment;
    private Long buildingId;
    private Long preSelectedFloorId;
    private boolean dataChanged = false;

    public ApartmentDialog(Frame owner, Apartment apartment, Long buildingId) {
        super(owner, apartment.getId() == null ? "Thêm Căn Hộ Mới" : "Cập Nhật Căn Hộ", true);
        this.apartment = apartment;
        this.buildingId = buildingId;
        this.preSelectedFloorId = apartment.getFloorId();

        this.floorDAO = new FloorDAO();
        this.apartmentDAO = new ApartmentDAO();

        initUI();
        fillData(); // Nạp dữ liệu và xử lý logic hiển thị trạng thái

        // Logic khóa field nếu cần
        if (apartment.getId() == null) {
            if (preSelectedFloorId != null) {
                autoSelectFloorAndGenerateRoom();
                cbbFloor.setEnabled(false);
            }
        } else {
            cbbFloor.setEnabled(false);
            txtRoomNumber.setEditable(false);
            txtRoomNumber.setBackground(new Color(240, 240, 240));
        }

        dataChanged = false;
        setSize(850, 600);
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 250));

        // HEADER
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        JLabel titleLabel = new JLabel(apartment.getId() == null ? "THIẾT LẬP CĂN HỘ MỚI" : "THÔNG TIN CHI TIẾT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(new Color(245, 245, 250));
        bodyPanel.setBorder(new EmptyBorder(15, 25, 15, 25));

        // SECTION 1 - Vị Trí & Loại Hình
        JPanel pnlLocation = createSectionPanel("Vị Trí & Loại Hình");
        cbbFloor = new JComboBox<>();
        List<Floor> floors = floorDAO.getFloorsByBuildingId(buildingId);
        for (Floor f : floors) {
            cbbFloor.addItem(f);
        }
        styleComboBox(cbbFloor);
        cbbFloor.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Floor) {
                    setText(((Floor) value).getName());
                }
                return this;
            }
        });
        cbbFloor.addActionListener(e -> {
            if (cbbFloor.isEnabled() && apartment.getId() == null) {
                generateRoomNumber();
            }
        });

        txtRoomNumber = createRoundedField();
        txtRoomNumber.setEditable(false);
        txtRoomNumber.setBackground(new Color(240, 240, 240));

        cbbType = new JComboBox<>(new String[]{"Standard", "Studio", "Mini", "Duplex", "Penthouse", "Shophouse"});
        styleComboBox(cbbType);

        JPanel row1 = new JPanel(new GridLayout(1, 3, 20, 0));
        row1.setOpaque(false);
        row1.add(createFieldGroup("Thuộc Tầng", cbbFloor));
        row1.add(createFieldGroup("Số Phòng", txtRoomNumber));
        row1.add(createFieldGroup("Loại Căn Hộ", cbbType));
        pnlLocation.add(row1);

        // SECTION 2 - Thông Số Chi Tiết
        JPanel pnlSpecs = createSectionPanel("Thông Số Chi Tiết");
        txtArea = createRoundedField();
        ((AbstractDocument) txtArea.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String s, AttributeSet a) throws BadLocationException {
                if (s.matches("[0-9.]*")) {
                    super.insertString(fb, offset, s, a);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int l, String t, AttributeSet a) throws BadLocationException {
                if (t.matches("[0-9.]*")) {
                    super.replace(fb, offset, l, t, a);
                }
            }
        });

        spnBedrooms = createSpinner();
        spnBathrooms = createSpinner();

        JPanel row2 = new JPanel(new GridLayout(1, 3, 20, 0));
        row2.setOpaque(false);
        row2.add(createFieldGroup("Diện Tích (m²)", txtArea));
        row2.add(createFieldGroup("Phòng Ngủ", spnBedrooms));
        row2.add(createFieldGroup("Phòng Tắm", spnBathrooms));
        pnlSpecs.add(row2);

        // SECTION 3 - Trạng Thái (ĐÃ BỎ GIÁ THUÊ)
        JPanel pnlStatus = createSectionPanel("Trạng Thái");

        // Mặc định chỉ có Trống và Bảo trì. Không có "Đã thuê" để tránh chọn nhầm.
        cbbStatus = new JComboBox<>(new String[]{"Trống", "Bảo trì"});
        styleComboBox(cbbStatus);

        JPanel statusRow = new JPanel(new GridLayout(1, 1, 20, 0));
        statusRow.setOpaque(false);
        statusRow.add(createFieldGroup("Trạng Thái Căn Hộ", cbbStatus));
        pnlStatus.add(statusRow);

        // SECTION 4 - Ghi Chú
        JPanel pnlDesc = createSectionPanel("Thông Tin Bổ Sung");
        txtDesc = new JTextArea(4, 20);
        txtDesc.setFont(UIConstants.FONT_REGULAR);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDesc);
        scrollDesc.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        pnlDesc.add(createFieldGroup("Ghi Chú", scrollDesc));

        // Listeners
        SimpleDocumentListener docListener = new SimpleDocumentListener(() -> dataChanged = true);
        txtArea.getDocument().addDocumentListener(docListener);
        txtDesc.getDocument().addDocumentListener(docListener);

        bodyPanel.add(pnlLocation);
        bodyPanel.add(Box.createVerticalStrut(10));
        bodyPanel.add(pnlSpecs);
        bodyPanel.add(Box.createVerticalStrut(10));
        bodyPanel.add(pnlStatus);
        bodyPanel.add(Box.createVerticalStrut(10));
        bodyPanel.add(pnlDesc);

        JScrollPane mainScroll = new JScrollPane(bodyPanel);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        // FOOTER BUTTONS
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new MatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JButton btnCancel = new RoundedButton("Hủy Bỏ", 10);
        btnCancel.setBackground(new Color(245, 245, 245));
        btnCancel.addActionListener(e -> handleCancel());

        JButton btnSave = new RoundedButton("Lưu Dữ Liệu", 10);
        btnSave.setBackground(UIConstants.PRIMARY_COLOR);
        btnSave.setForeground(Color.WHITE);
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

    private void fillData() {
        if (apartment.getId() != null) {
            txtRoomNumber.setText(apartment.getRoomNumber());
            if (apartment.getArea() != null) {
                txtArea.setText(String.valueOf(apartment.getArea()));
            }

            if (apartment.getApartmentType() != null) {
                cbbType.setSelectedItem(apartment.getApartmentType());
            }
            if (apartment.getBedroomCount() != null) {
                spnBedrooms.setValue(apartment.getBedroomCount());
            }
            if (apartment.getBathroomCount() != null) {
                spnBathrooms.setValue(apartment.getBathroomCount());
            }
            txtDesc.setText(apartment.getDescription());

            // Xử lý trạng thái từ Database
            String dbStatus = (apartment.getStatus() == null) ? "AVAILABLE" : apartment.getStatus();

            if (dbStatus.equalsIgnoreCase("RENTED") || dbStatus.equalsIgnoreCase("OCCUPIED") || dbStatus.equalsIgnoreCase("Đã thuê")) {
                // Nếu đang thuê: Thêm option "Đã thuê" vào list và KHÓA LẠI (Disable)
                if (((DefaultComboBoxModel) cbbStatus.getModel()).getIndexOf("Đã thuê") == -1) {
                    cbbStatus.addItem("Đã thuê");
                }
                cbbStatus.setSelectedItem("Đã thuê");
                cbbStatus.setEnabled(false);
            } else if (dbStatus.equalsIgnoreCase("MAINTENANCE") || dbStatus.contains("Bảo trì")) {
                cbbStatus.setSelectedItem("Bảo trì");
                cbbStatus.setEnabled(true);
            } else {
                cbbStatus.setSelectedItem("Trống");
                cbbStatus.setEnabled(true);
            }

            // Chọn tầng
            if (apartment.getFloorId() != null) {
                for (int i = 0; i < cbbFloor.getItemCount(); i++) {
                    if (cbbFloor.getItemAt(i).getId().equals(apartment.getFloorId())) {
                        cbbFloor.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void onSave() {
        StringBuilder sb = new StringBuilder();
        if (txtArea.getText().trim().isEmpty()) {
            sb.append("- Thiếu Diện tích\n");
        }

        if (sb.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ:\n" + sb.toString(),
                    "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Map trạng thái Tiếng Việt -> Tiếng Anh
        String uiStatus = (String) cbbStatus.getSelectedItem();
        String dbStatus = "AVAILABLE"; // Mặc định là TRỐNG

        if ("Bảo trì".equals(uiStatus) || "Đang bảo trì".equals(uiStatus)) {
            dbStatus = "MAINTENANCE";
        } else if ("Đã thuê".equals(uiStatus)) {
            dbStatus = "RENTED";
        } else {
            dbStatus = "AVAILABLE";
        }

        // Logic bảo vệ: Nếu căn hộ đang RENTED trong DB nhưng UI lại chọn cái khác, thì chặn lại
        String currentDbStatus = (apartment.getStatus() == null) ? "AVAILABLE" : apartment.getStatus();
        boolean isCurrentlyRented = "RENTED".equalsIgnoreCase(currentDbStatus)
                || "OCCUPIED".equalsIgnoreCase(currentDbStatus);

        if (isCurrentlyRented && !"RENTED".equals(dbStatus)) {
            // Nếu đang thuê mà người dùng cố tình chuyển sang trạng thái khác
            // Ta giữ nguyên trạng thái RENTED
            dbStatus = "RENTED";
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Xác nhận lưu thông tin?",
                "Lưu",
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Double area = Double.parseDouble(txtArea.getText().trim());

            apartment.setFloorId(((Floor) cbbFloor.getSelectedItem()).getId());
            apartment.setRoomNumber(txtRoomNumber.getText().trim());
            apartment.setArea(area);
            apartment.setDescription(txtDesc.getText().trim());

            // Chỉ cập nhật trạng thái nếu không bị khóa (tức là không phải đang thuê)
            if (cbbStatus.isEnabled()) {
                apartment.setStatus(dbStatus);
            }

            apartment.setApartmentType((String) cbbType.getSelectedItem());
            apartment.setBedroomCount((Integer) spnBedrooms.getValue());
            apartment.setBathroomCount((Integer) spnBathrooms.getValue());

            confirmed = true;
            dispose();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Số liệu không hợp lệ! Vui lòng kiểm tra diện tích.",
                    "Lỗi định dạng",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configureShortcuts(JButton defaultButton) {
        getRootPane().setDefaultButton(defaultButton);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancel();
            }
        });
    }

    private void handleCancel() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn thoát? Dữ liệu chưa lưu sẽ bị mất.",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    private void autoSelectFloorAndGenerateRoom() {
        if (preSelectedFloorId != null) {
            for (int i = 0; i < cbbFloor.getItemCount(); i++) {
                Floor f = cbbFloor.getItemAt(i);
                if (f.getId().equals(preSelectedFloorId)) {
                    cbbFloor.setSelectedIndex(i);
                    break;
                }
            }
        }
        generateRoomNumber();
    }

    private void generateRoomNumber() {
        Floor selectedFloor = (Floor) cbbFloor.getSelectedItem();
        if (selectedFloor == null) {
            return;
        }

        int floorNum = selectedFloor.getFloorNumber();
        List<Apartment> existingApts = apartmentDAO.getApartmentsByFloorId(selectedFloor.getId());

        int maxSuffix = 0;
        for (Apartment a : existingApts) {
            try {
                String roomNum = a.getRoomNumber();
                if (roomNum.length() >= 2) {
                    String suffixStr = roomNum.substring(roomNum.length() - 2);
                    int suffix = Integer.parseInt(suffixStr);
                    if (suffix > maxSuffix) {
                        maxSuffix = suffix;
                    }
                }
            } catch (Exception e) {
            }
        }

        int nextSuffix = maxSuffix + 1;
        String newRoomNumber = floorNum + String.format("%02d", nextSuffix);
        txtRoomNumber.setText(newRoomNumber);
    }

    // Helper classes
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

    private JPanel createSectionPanel(String t) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);

        TitledBorder b = BorderFactory.createTitledBorder(
                new LineBorder(new Color(220, 220, 220), 1, true), t);
        b.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setTitleColor(UIConstants.PRIMARY_COLOR);
        p.setBorder(new CompoundBorder(b, new EmptyBorder(10, 15, 10, 15)));

        return p;
    }

    private JPanel createFieldGroup(String l, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);

        JLabel lbl = new JLabel(l);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(80, 80, 80));

        p.add(lbl, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);

        return p;
    }

    private void styleComboBox(JComboBox box) {
        box.setPreferredSize(new Dimension(100, 35));
        box.setBackground(Color.WHITE);
        box.setFont(UIConstants.FONT_REGULAR);
    }

    private JSpinner createSpinner() {
        JSpinner s = new JSpinner(new SpinnerNumberModel(1, 0, 20, 1));
        s.setPreferredSize(new Dimension(100, 35));
        s.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        return s;
    }

    private JTextField createRoundedField() {
        JTextField f = new RoundedTextField(8);
        f.setPreferredSize(new Dimension(100, 35));
        f.setFont(UIConstants.FONT_REGULAR);
        return f;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Apartment getApartment() {
        return apartment;
    }

    // Custom UI Components
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
