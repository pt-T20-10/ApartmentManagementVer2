package view;

import dao.*;
import model.*;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Invoice Detail Dialog - View details and payment
 */
public class InvoiceDetailDialog extends JDialog {

    private InvoiceDAO invoiceDAO;
    private ContractDAO contractDAO;
    private ApartmentDAO apartmentDAO;
    private ResidentDAO residentDAO;

    private Invoice invoice;
    private Contract contract;

    private DecimalFormat moneyFormat = new DecimalFormat("#,##0");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private JLabel lblStatus;
    private JButton btnPay;
    private JButton btnEdit;

    public InvoiceDetailDialog(JFrame parent, Long invoiceId) {
        super(parent, "Chi Tiết Hóa Đơn", true);

        this.invoiceDAO = new InvoiceDAO();
        this.contractDAO = new ContractDAO();
        this.apartmentDAO = new ApartmentDAO();
        this.residentDAO = new ResidentDAO();

        this.invoice = invoiceDAO.getInvoiceById(invoiceId);
        if (invoice != null) {
            this.contract = contractDAO.getContractById(invoice.getContractId());
        }

        initializeDialog();
        createContent();

        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeDialog() {
        setSize(700, 750);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);
    }

    private void createContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);

        // Content
        JScrollPane scrollPane = new JScrollPane(createContentPanel());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        // Left: Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JLabel icon = new JLabel("📄");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JLabel title = new JLabel("Chi Tiết Hóa Đơn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UIConstants.TEXT_PRIMARY);

        leftPanel.add(icon);
        leftPanel.add(Box.createHorizontalStrut(10));
        leftPanel.add(title);

        // Right: Status
        lblStatus = createStatusBadge(invoice.getStatus());

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(lblStatus, BorderLayout.EAST);

        return panel;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Invoice Info
        panel.add(createInvoiceInfoSection());
        panel.add(Box.createVerticalStrut(20));

        // Contract Info
        panel.add(createContractInfoSection());
        panel.add(Box.createVerticalStrut(20));

        // Invoice Details (services)
        panel.add(createInvoiceDetailsSection());
        panel.add(Box.createVerticalStrut(20));

        // Total
        panel.add(createTotalSection());

        // Payment Info (if paid)
        if ("PAID".equals(invoice.getStatus()) && invoice.getPaymentDate() != null) {
            panel.add(Box.createVerticalStrut(20));
            panel.add(createPaymentInfoSection());
        }

        return panel;
    }

    private JPanel createInvoiceInfoSection() {
        JPanel section = createSection("📋 Thông Tin Hóa Đơn");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        int row = 0;

        // Invoice ID
        addInfoRow(section, gbc, row++, "Mã hóa đơn:", "#" + invoice.getId());

        // Period
        String period = String.format("Tháng %d/%d", invoice.getMonth(), invoice.getYear());
        addInfoRow(section, gbc, row++, "Kỳ hóa đơn:", period);

        // Created date
        if (invoice.getCreatedAt() != null) {
            addInfoRow(section, gbc, row++, "Ngày tạo:", dateFormat.format(invoice.getCreatedAt()));
        }

        return section;
    }

    private JPanel createContractInfoSection() {
        JPanel section = createSection("📝 Thông Tin Hợp Đồng");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        int row = 0;

        if (contract != null) {
            // Contract number
            addInfoRow(section, gbc, row++, "Số hợp đồng:", contract.getContractNumber());

            // Apartment
            Apartment apt = apartmentDAO.getApartmentById(contract.getApartmentId());
            if (apt != null) {
                addInfoRow(section, gbc, row++, "Căn hộ:", apt.getRoomNumber());
            }

            // Resident
            Resident res = residentDAO.getResidentById(contract.getResidentId());
            if (res != null) {
                addInfoRow(section, gbc, row++, "Chủ hộ:", res.getFullName());
                addInfoRow(section, gbc, row++, "SĐT:", res.getPhone());
            }

            // Contract type
            addInfoRow(section, gbc, row++, "Loại hợp đồng:", contract.getContractTypeDisplay());
        }

        return section;
    }

    private JPanel createInvoiceDetailsSection() {
        JPanel section = createSection("📊 Chi Tiết Thanh Toán");
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        // Table header
        JPanel headerPanel = new JPanel(new GridLayout(1, 4, 5, 0));
        headerPanel.setBackground(new Color(245, 245, 245));
        headerPanel.setBorder(new EmptyBorder(10, 12, 10, 12));
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        headerPanel.add(createHeaderLabel("Hạng mục"));
        headerPanel.add(createHeaderLabel("Đơn giá"));
        headerPanel.add(createHeaderLabel("Số lượng"));
        headerPanel.add(createHeaderLabel("Thành tiền"));

        section.add(headerPanel);

        // Get invoice details
        List<InvoiceDetail> details = invoiceDAO.getInvoiceDetails(invoice.getId());

        if (details == null || details.isEmpty()) {
            JLabel empty = new JLabel("Không có chi tiết thanh toán");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(UIConstants.TEXT_SECONDARY);
            empty.setBorder(new EmptyBorder(15, 12, 15, 12));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            section.add(empty);
        } else {
            for (InvoiceDetail detail : details) {
                addDetailRow(
                        section,
                        detail.getServiceName(),
                        detail.getUnitPrice(),
                        detail.getQuantity(),
                        detail.getAmount()
                );
            }
        }

        return section;
    }

    private void addDetailRow(JPanel parent, String name,
            java.math.BigDecimal unitPrice,
            Double quantity,
            java.math.BigDecimal amount) {
        JPanel row = new JPanel(new GridLayout(1, 4, 5, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(10, 12, 10, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        row.add(createDetailLabel(name));
        row.add(createDetailLabel(moneyFormat.format(unitPrice) + " VNĐ"));
        row.add(createDetailLabel(String.format("%.2f", quantity)));
        row.add(createDetailLabel(moneyFormat.format(amount) + " VNĐ", true));

        parent.add(row);
    }

    private JPanel createTotalSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(new Color(240, 248, 255));
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 2),
                new EmptyBorder(20, 25, 20, 25)
        ));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel lblLabel = new JLabel("TỔNG THANH TOÁN:");
        lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblLabel.setForeground(new Color(33, 33, 33));

        JLabel lblAmount = new JLabel(moneyFormat.format(invoice.getTotalAmount()) + " VNĐ");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblAmount.setForeground(new Color(46, 125, 50));

        section.add(lblLabel, BorderLayout.WEST);
        section.add(lblAmount, BorderLayout.EAST);

        return section;
    }

    private JPanel createPaymentInfoSection() {
        JPanel section = createSection("✅ Thông Tin Thanh Toán");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        int row = 0;

        addInfoRow(section, gbc, row++, "Ngày thanh toán:",
                dateFormat.format(invoice.getPaymentDate()));
        addInfoRow(section, gbc, row++, "Trạng thái:", "Đã thanh toán");

        return section;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        // Left: Edit button (only for unpaid invoices)
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        if ("UNPAID".equals(invoice.getStatus())) {
            btnEdit = createButton("✏️ Sửa", new Color(255, 152, 0));
            btnEdit.addActionListener(e -> editInvoice());
            leftPanel.add(btnEdit);
        }

        // Right: Pay and Close buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JButton btnClose = createButton("Đóng", new Color(158, 158, 158));
        btnClose.addActionListener(e -> dispose());

        rightPanel.add(btnClose);

        // Pay button (only for unpaid invoices)
        if ("UNPAID".equals(invoice.getStatus())) {
            btnPay = createButton("💰 Thanh Toán", new Color(46, 125, 50));
            btnPay.addActionListener(e -> confirmPayment());
            rightPanel.add(btnPay, 0); // Add at beginning
        }

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    // ===== HELPER METHODS =====
    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                UIConstants.TEXT_PRIMARY
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2000));
        return panel;
    }

    private void addInfoRow(JPanel parent, GridBagConstraints gbc, int row,
            String label, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.4;
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblLabel.setForeground(UIConstants.TEXT_SECONDARY);
        parent.add(lblLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);
        parent.add(lblValue, gbc);
    }

    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(66, 66, 66));
        return label;
    }

    private JLabel createDetailLabel(String text) {
        return createDetailLabel(text, false);
    }

    private JLabel createDetailLabel(String text, boolean bold) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 13));
        label.setForeground(UIConstants.TEXT_PRIMARY);
        return label;
    }

    private JLabel createStatusBadge(String status) {
        JLabel badge = new JLabel();
        badge.setFont(new Font("Segoe UI", Font.BOLD, 14));
        badge.setBorder(new EmptyBorder(8, 16, 8, 16));
        badge.setOpaque(true);

        if ("PAID".equals(status)) {
            badge.setText("✓ Đã thanh toán");
            badge.setBackground(new Color(232, 245, 233));
            badge.setForeground(new Color(46, 125, 50));
        } else {
            badge.setText("⏳ Chưa thanh toán");
            badge.setBackground(new Color(255, 243, 224));
            badge.setForeground(new Color(230, 126, 34));
        }

        return badge;
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(150, 42));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ===== BUSINESS LOGIC =====
    private void confirmPayment() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận đã thanh toán hóa đơn này?\n\n"
                + "Số tiền: " + moneyFormat.format(invoice.getTotalAmount()) + " VNĐ",
                "Xác Nhận Thanh Toán",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            processPayment();
        }
    }

    private void processPayment() {
        try {
            invoice.setStatus("PAID");
            invoice.setPaymentDate(new java.util.Date());

            boolean success = invoiceDAO.updateInvoice(invoice);

            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Thanh toán thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);

                // Reload dialog
                dispose();
                new InvoiceDetailDialog((JFrame) getOwner(), invoice.getId()).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật trạng thái thanh toán thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editInvoice() {
        JFrame parent = (JFrame) getOwner();
        InvoiceFormDialog dialog = new InvoiceFormDialog(parent, invoice);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            // Reload dialog
            dispose();
            new InvoiceDetailDialog(parent, invoice.getId()).setVisible(true);
        }
    }
}
