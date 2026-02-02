package view;

import dao.*;
import model.*;
import util.UIConstants;
import util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * Invoice Form Dialog - Create/Edit Invoice Supports: 1. Rental + Services
 * (Thuê nhà + dịch vụ) 2. Services Only for Ownership (Chỉ dịch vụ cho nhà mua)
 * 3. Individual Services (Dịch vụ đơn lẻ)
 */
public class InvoiceFormDialog extends JDialog {

    private JTextField txtYear;
    // DAOs
    private InvoiceDAO invoiceDAO;
    private ContractDAO contractDAO;
    private ApartmentDAO apartmentDAO;
    private ResidentDAO residentDAO;
    private ServiceDAO serviceDAO;
    private ContractServiceDAO contractServiceDAO;

    private JRadioButton rbServicesOnly;
    private JRadioButton rbXXX;
    private JComboBox<?> contractCombo;

    // Components
    private JComboBox<ContractDisplay> cmbContract;
    private JSpinner spnMonth;
    private JSpinner spnYear;

    // Invoice type
    private JRadioButton rbRentalWithServices;

    private JRadioButton rbIndividual;

    // Rental section
    private JPanel rentalSection;
    private JTextField txtRentalAmount;

    // Services section
    private JPanel servicesSection;
    private java.util.List<ServiceRow> serviceRows;

    // Debt section
    private JCheckBox chkHasDebt;
    private JTextField txtDebtAmount;
    private JTextArea txtDebtNote;

    // Total section
    private JLabel lblSubtotal;
    private JLabel lblDebt;
    private JLabel lblGrandTotal;

    // Data
    private Invoice invoice;
    private Contract selectedContract;
    private boolean isEditMode;
    private boolean confirmed = false;

    // Formatters
    private DecimalFormat moneyFormat = new DecimalFormat("#,##0");
    private boolean uiReady = false;

    /**
     * Inner class for service row
     */
    private class ServiceRow {

        JCheckBox chkEnabled;
        JLabel lblServiceName;
        JTextField txtUnitPrice;
        JTextField txtQuantity;
        JLabel lblAmount;
        Service service;

        ServiceRow(Service service) {
            this.service = service;
            this.chkEnabled = new JCheckBox();
            this.lblServiceName = new JLabel(service.getName());

            // ✅ FIX: Use MoneyFormatter properly
            this.txtUnitPrice = MoneyFormatter.createMoneyField(35);
            MoneyFormatter.setValue(this.txtUnitPrice, service.getUnitPrice().longValue());

            this.txtQuantity = new JTextField("1.0");
            this.txtQuantity.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            this.txtQuantity.setHorizontalAlignment(JTextField.CENTER);

            this.lblAmount = new JLabel("0");
            this.lblAmount.setHorizontalAlignment(SwingConstants.CENTER);

            // Listeners
            chkEnabled.addActionListener(e -> updateServiceRow());
            txtUnitPrice.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    calculateAmount();
                }

                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    calculateAmount();
                }

                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    calculateAmount();
                }
            });
            txtQuantity.addActionListener(e -> calculateAmount());
        }

        void updateServiceRow() {
            boolean enabled = chkEnabled.isSelected();
            txtUnitPrice.setEnabled(enabled);
            txtQuantity.setEnabled(enabled);
            calculateAmount();
        }

        void calculateAmount() {
            if (!chkEnabled.isSelected()) {
                lblAmount.setText("0");
                calculateTotal();
                return;
            }

            try {
                Long unitPrice = MoneyFormatter.getValue(txtUnitPrice);
                if (unitPrice == null) {
                    unitPrice = 0L;
                }

                double quantity = Double.parseDouble(txtQuantity.getText().trim());
                long amount = (long) (unitPrice * quantity);

                lblAmount.setText(moneyFormat.format(amount));
            } catch (Exception e) {
                lblAmount.setText("0");
            }
            calculateTotal();
        }

        BigDecimal getAmount() {
            if (!chkEnabled.isSelected()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(lblAmount.getText().replace(",", ""));
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * Inner class for contract display
     */
    private class ContractDisplay {

        Contract contract;
        String displayText;

        ContractDisplay(Contract contract, String displayText) {
            this.contract = contract;
            this.displayText = displayText;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    /**
     * Constructor for Create mode
     */
    public InvoiceFormDialog(JFrame parent) {
        this(parent, null);
    }

    /**
     * Constructor for Edit mode
     */
    public InvoiceFormDialog(JFrame parent, Invoice invoice) {
        super(parent, invoice == null ? "Tạo Hóa Đơn" : "Sửa Hóa Đơn", true);

        this.invoice = invoice;
        this.isEditMode = (invoice != null);

        initializeDAOs();
        initializeDialog();
        createContent();

        pack();

        setMinimumSize(new Dimension(800, 600));
        setMaximumSize(new Dimension(1200, 900));

        setLocationRelativeTo(parent);

        // ✅ UI đã sẵn sàng
        uiReady = true;

        if (isEditMode) {
            loadInvoiceData();
        } else {
            Calendar cal = Calendar.getInstance();
            spnMonth.setValue(cal.get(Calendar.MONTH) + 1);
            txtYear.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }
    }

    private void initializeDAOs() {
        this.invoiceDAO = new InvoiceDAO();
        this.contractDAO = new ContractDAO();
        this.apartmentDAO = new ApartmentDAO();
        this.residentDAO = new ResidentDAO();
        this.serviceDAO = new ServiceDAO();
        this.contractServiceDAO = new ContractServiceDAO();
        this.serviceRows = new ArrayList<>();
    }

    private void initializeDialog() {
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);
    }

    private void createContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);

        // Content (scrollable)
        JScrollPane scrollPane = new JScrollPane(createFormPanel());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        JLabel icon = new JLabel(isEditMode ? "✏️" : "📄");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JLabel title = new JLabel(isEditMode ? "Sửa Hóa Đơn" : "Tạo Hóa Đơn Mới");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UIConstants.TEXT_PRIMARY);

        panel.add(icon);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(title);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 1. Invoice type (radio buttons) – PHẢI TẠO ĐẦU TIÊN
        panel.add(createInvoiceTypeSection());
        panel.add(Box.createVerticalStrut(15));

        // 2. Rental section – KHỞI TẠO TRƯỚC
        rentalSection = createRentalSection();
        panel.add(rentalSection);
        panel.add(Box.createVerticalStrut(15));

        // 3. Services section – KHỞI TẠO TRƯỚC
        servicesSection = createServicesSection();
        panel.add(servicesSection);
        panel.add(Box.createVerticalStrut(15));

        // 4. Contract & Period section – TẠO CUỐI (vì loadContracts() fire event)
        panel.add(createContractSection());
        panel.add(Box.createVerticalStrut(15));

        // 5. Debt section
        panel.add(createDebtSection());
        panel.add(Box.createVerticalStrut(15));

        // 6. Total section
        panel.add(createTotalSection());

        return panel;
    }

    private JPanel createContractSection() {
        JPanel section = createSection("📋 Hợp Đồng & Kỳ Hóa Đơn");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Contract
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Hợp đồng:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        cmbContract = new JComboBox<>();
        cmbContract.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbContract.setPreferredSize(new Dimension(0, 35));
        cmbContract.addActionListener(e -> onContractChanged());
        loadContracts();
        section.add(cmbContract, gbc);

        // Month
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        section.add(createLabel("Tháng:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        SpinnerNumberModel monthModel = new SpinnerNumberModel(1, 1, 12, 1);
        spnMonth = new JSpinner(monthModel);
        spnMonth.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) spnMonth.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
        section.add(spnMonth, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        section.add(createLabel("Năm:", true), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.3;

        txtYear = new JTextField("2026");
        txtYear.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtYear.setHorizontalAlignment(JTextField.CENTER);
        txtYear.setEditable(false);
        txtYear.setBackground(new Color(245, 245, 245));

        section.add(txtYear, gbc);

        return section;
    }

    private JPanel createInvoiceTypeSection() {
        JPanel section = createSection("🏷️ Loại Hóa Đơn");
        section.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));

        ButtonGroup group = new ButtonGroup();

        rbRentalWithServices = new JRadioButton("Thuê nhà + Dịch vụ");
        rbRentalWithServices.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rbRentalWithServices.setBackground(Color.WHITE);
        rbRentalWithServices.setSelected(true);
        rbRentalWithServices.addActionListener(e -> onInvoiceTypeChanged());
        group.add(rbRentalWithServices);

        rbServicesOnly = new JRadioButton("Chỉ dịch vụ (Nhà mua)");
        rbServicesOnly.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rbServicesOnly.setBackground(Color.WHITE);
        rbServicesOnly.addActionListener(e -> onInvoiceTypeChanged());
        group.add(rbServicesOnly);

        rbIndividual = new JRadioButton("Dịch vụ đơn lẻ");
        rbIndividual.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rbIndividual.setBackground(Color.WHITE);
        rbIndividual.addActionListener(e -> onInvoiceTypeChanged());
        group.add(rbIndividual);

        section.add(rbRentalWithServices);
        section.add(rbServicesOnly);
        section.add(rbIndividual);

        return section;
    }

    private JPanel createRentalSection() {
        JPanel section = createSection("🏠 Tiền Thuê Nhà");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Tiền thuê/tháng:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtRentalAmount = MoneyFormatter.createMoneyField(35);
        txtRentalAmount.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                calculateTotal();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                calculateTotal();
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                calculateTotal();
            }
        });
        section.add(txtRentalAmount, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblVND = new JLabel("VNĐ");
        lblVND.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblVND.setForeground(new Color(46, 125, 50));
        section.add(lblVND, gbc);

        return section;
    }

    private JPanel createServicesSection() {
        JPanel section = createSection("🔧 Dịch Vụ");
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        // Header row
        JPanel headerRow = new JPanel(new GridLayout(1, 5, 5, 0));
        headerRow.setBackground(new Color(245, 245, 245));
        headerRow.setBorder(new EmptyBorder(8, 8, 8, 8));
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        headerRow.add(createHeaderLabel("Chọn"));
        headerRow.add(createHeaderLabel("Tên dịch vụ"));
        headerRow.add(createHeaderLabel("Đơn giá"));
        headerRow.add(createHeaderLabel("Số lượng"));
        headerRow.add(createHeaderLabel("Thành tiền"));

        section.add(headerRow);
        section.add(Box.createVerticalStrut(5));

        // Service rows will be added here
        JLabel placeholder = new JLabel("Chọn hợp đồng để hiển thị dịch vụ");
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        placeholder.setForeground(UIConstants.TEXT_SECONDARY);
        placeholder.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(placeholder);

        return section;
    }

    private JPanel createDebtSection() {
        JPanel section = createSection("💳 Nợ Cũ");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Has debt checkbox
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 4;
        chkHasDebt = new JCheckBox("Có nợ cũ");
        chkHasDebt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chkHasDebt.setBackground(Color.WHITE);
        chkHasDebt.addActionListener(e -> onDebtCheckChanged());
        section.add(chkHasDebt, gbc);

        // Debt amount
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        JLabel lblDebtAmount = createLabel("Số tiền nợ:", false);
        section.add(lblDebtAmount, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 2;
        txtDebtAmount = MoneyFormatter.createMoneyField(35);
        txtDebtAmount.setEnabled(false);
        txtDebtAmount.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                calculateTotal();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                calculateTotal();
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                calculateTotal();
            }
        });
        section.add(txtDebtAmount, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        JLabel lblVND = new JLabel("VNĐ");
        lblVND.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblVND.setForeground(new Color(211, 47, 47));
        section.add(lblVND, gbc);

        // Debt note
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        section.add(createLabel("Ghi chú:", false), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        txtDebtNote = new JTextArea(2, 20);
        txtDebtNote.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDebtNote.setLineWrap(true);
        txtDebtNote.setWrapStyleWord(true);
        txtDebtNote.setEnabled(false);
        JScrollPane scrollNote = new JScrollPane(txtDebtNote);
        scrollNote.setPreferredSize(new Dimension(0, 60));
        section.add(scrollNote, gbc);

        return section;
    }

    private JPanel createTotalSection() {
        JPanel section = createSection("💰 Tổng Thanh Toán");
        section.setBackground(new Color(245, 250, 255));
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 15, 5, 15);

        // Subtotal
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        JLabel lblSubtotalLabel = new JLabel("Tạm tính:");
        lblSubtotalLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        section.add(lblSubtotalLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        lblSubtotal = new JLabel("0 VNĐ");
        lblSubtotal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblSubtotal.setForeground(new Color(33, 150, 243));
        section.add(lblSubtotal, gbc);

        // Debt
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        JLabel lblDebtLabel = new JLabel("Nợ cũ:");
        lblDebtLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        section.add(lblDebtLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        lblDebt = new JLabel("0 VNĐ");
        lblDebt.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblDebt.setForeground(new Color(211, 47, 47));
        section.add(lblDebt, gbc);

        // Separator
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JSeparator separator = new JSeparator();
        separator.setPreferredSize(new Dimension(0, 2));
        section.add(separator, gbc);

        // Grand Total
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        JLabel lblGrandLabel = new JLabel("TỔNG CỘNG:");
        lblGrandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        section.add(lblGrandLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        lblGrandTotal = new JLabel("0 VNĐ");
        lblGrandTotal.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblGrandTotal.setForeground(new Color(46, 125, 50));
        section.add(lblGrandTotal, gbc);

        return section;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        JButton btnCancel = createButton("Hủy", new Color(158, 158, 158));
        btnCancel.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton btnSave = createButton(isEditMode ? "Cập Nhật" : "Tạo Hóa Đơn", new Color(46, 125, 50));
        btnSave.addActionListener(e -> saveInvoice());

        panel.add(btnCancel);
        panel.add(btnSave);

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
        return panel;
    }

    private JLabel createLabel(String text, boolean required) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(UIConstants.TEXT_PRIMARY);
        if (required) {
            label.setText("<html>" + text + " <font color='red'>*</font></html>");
        }
        return label;
    }

    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(66, 66, 66));
        return label;
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(140, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ===== BUSINESS LOGIC =====
    private void loadContracts() {
        List<Contract> contracts = contractDAO.getAllContracts();
        cmbContract.removeAllItems();

        for (Contract contract : contracts) {
            if ("ACTIVE".equals(contract.getStatus())) {
                Apartment apt = apartmentDAO.getApartmentById(contract.getApartmentId());
                Resident res = residentDAO.getResidentById(contract.getResidentId());

                String display = String.format("%s - %s - %s (%s)",
                        contract.getContractNumber(),
                        apt != null ? apt.getRoomNumber() : "N/A",
                        res != null ? res.getFullName() : "N/A",
                        contract.getContractTypeDisplay()
                );

                cmbContract.addItem(new ContractDisplay(contract, display));
            }
        }
    }

    private void onContractChanged() {
        if (!uiReady) {
            return;
        }

        ContractDisplay selected = (ContractDisplay) cmbContract.getSelectedItem();
        if (selected == null) {
            return;
        }

        selectedContract = selected.contract;

        if (selectedContract.isRental()) {
            rbRentalWithServices.setSelected(true);
        } else {
            rbServicesOnly.setSelected(true);
        }

        onInvoiceTypeChanged();
    }

    private void onInvoiceTypeChanged() {
        if (!uiReady) {
            return;
        }

        boolean showRental = rbRentalWithServices.isSelected();
        rentalSection.setVisible(showRental);

        if (selectedContract != null) {
            loadContractData();
        }

        revalidate();
        repaint();
    }

    private void loadContractData() {
        if (!uiReady || selectedContract == null) {
            return;
        }

        if (selectedContract.isRental() && rbRentalWithServices.isSelected()) {
            BigDecimal rent = selectedContract.getMonthlyRent();
            MoneyFormatter.setValue(
                    txtRentalAmount,
                    rent != null ? rent.longValue() : 0
            );
        }

        loadServices();
        calculateTotal();
    }

    private void loadServices() {
        // Clear existing service rows
        Component[] components = servicesSection.getComponents();
        for (int i = 2; i < components.length; i++) {
            servicesSection.remove(components[i]);
        }
        serviceRows.clear();

        if (selectedContract == null) {
            return;
        }

        // Get contract services
        List<ContractService> contractServices = contractServiceDAO.getActiveServicesByContract(selectedContract.getId());

        for (ContractService cs : contractServices) {
            Service service = serviceDAO.getServiceById(cs.getServiceId());
            if (service != null) {
                addServiceRow(service);
            }
        }

        servicesSection.revalidate();
        servicesSection.repaint();
    }

    private void addServiceRow(Service service) {
        ServiceRow row = new ServiceRow(service);

        JPanel rowPanel = new JPanel(new GridLayout(1, 5, 5, 0));
        rowPanel.setBackground(Color.WHITE);
        rowPanel.setBorder(new EmptyBorder(5, 8, 5, 8));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        rowPanel.add(row.chkEnabled);
        rowPanel.add(row.lblServiceName);
        rowPanel.add(row.txtUnitPrice);
        rowPanel.add(row.txtQuantity);
        rowPanel.add(row.lblAmount);

        serviceRows.add(row);
        servicesSection.add(rowPanel);
    }

    private void onDebtCheckChanged() {
        boolean hasDebt = chkHasDebt.isSelected();
        txtDebtAmount.setEnabled(hasDebt);
        txtDebtNote.setEnabled(hasDebt);

        if (!hasDebt) {
            MoneyFormatter.setValue(txtDebtAmount, (long) 0);
            txtDebtNote.setText("");
        }

        calculateTotal();
    }

    private void calculateTotal() {
        if (!uiReady) {
            return;
        }

        long subtotal = 0;

        // Rental
        if (rentalSection.isVisible()) {
            Long rental = MoneyFormatter.getValue(txtRentalAmount);
            if (rental != null) {
                subtotal += rental;
            }
        }

        // Services
        for (ServiceRow row : serviceRows) {
            subtotal += row.getAmount().longValue();
        }

        // Debt (AN TOÀN)
        long debt = 0;
        if (chkHasDebt != null && chkHasDebt.isSelected()) {
            Long debtValue = MoneyFormatter.getValue(txtDebtAmount);
            if (debtValue != null) {
                debt = debtValue;
            }
        }

        long grandTotal = subtotal + debt;

        lblSubtotal.setText(moneyFormat.format(subtotal) + " VNĐ");
        lblDebt.setText(moneyFormat.format(debt) + " VNĐ");
        lblGrandTotal.setText(moneyFormat.format(grandTotal) + " VNĐ");
    }

    private void loadInvoiceData() {
        // TODO: Load existing invoice data for edit mode
        // This will be implemented when needed
    }

    private List<InvoiceDetail> buildInvoiceDetails(Long invoiceId) {
        List<InvoiceDetail> details = new ArrayList<>();

        // ✅ TRƯỜNG HỢP: thuê nhà + dịch vụ
        if (rentalSection.isVisible()) {
            Long rent = MoneyFormatter.getValue(txtRentalAmount);
            if (rent != null && rent > 0) {
                InvoiceDetail d = new InvoiceDetail();
                d.setInvoiceId(invoiceId);
                d.setServiceName("Tiền thuê nhà");
                d.setUnitPrice(BigDecimal.valueOf(rent));
                d.setQuantity(1.0);
                d.setAmount(BigDecimal.valueOf(rent));
                details.add(d);
            }
        }

        // ✅ DỊCH VỤ
        for (ServiceRow row : serviceRows) {
            if (!row.chkEnabled.isSelected()) {
                continue;
            }

            BigDecimal amount = row.getAmount();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            InvoiceDetail d = new InvoiceDetail();
            d.setInvoiceId(invoiceId);
            d.setServiceName(row.service.getName());
            d.setUnitPrice(
                    BigDecimal.valueOf(
                            MoneyFormatter.getValue(row.txtUnitPrice)
                    )
            );
            d.setQuantity(Double.parseDouble(row.txtQuantity.getText()));
            d.setAmount(amount);

            details.add(d);
        }

        return details;
    }

    private void saveInvoice() {
        if (!validateForm()) {
            return;
        }

        try {
            if (invoice == null) {
                invoice = new Invoice();
            }

            ContractDisplay selected = (ContractDisplay) cmbContract.getSelectedItem();
            invoice.setContractId(selected.contract.getId());
            invoice.setMonth((Integer) spnMonth.getValue());
            invoice.setYear(Integer.parseInt(txtYear.getText()));
            invoice.setStatus("UNPAID");

            long total = 0;

            if (rentalSection.isVisible()) {
                Long rental = MoneyFormatter.getValue(txtRentalAmount);
                if (rental != null) {
                    total += rental;
                }
            }

            for (ServiceRow row : serviceRows) {
                total += row.getAmount().longValue();
            }

            if (chkHasDebt.isSelected()) {
                Long debt = MoneyFormatter.getValue(txtDebtAmount);
                if (debt != null) {
                    total += debt;
                }
            }

            invoice.setTotalAmount(BigDecimal.valueOf(total));

            Long invoiceId;

            if (isEditMode) {
                boolean ok = invoiceDAO.updateInvoice(invoice);
                if (!ok) {
                    showError("Cập nhật hóa đơn thất bại!");
                    return;
                }
                invoiceId = invoice.getId();
            } else {
                invoiceId = invoiceDAO.insertInvoiceAndReturnId(invoice);
                if (invoiceId == null) {
                    showError("Tạo hóa đơn thất bại!");
                    return;
                }
                invoice.setId(invoiceId);
            }

            // 👉 INSERT DETAILS
            List<InvoiceDetail> details = buildInvoiceDetails(invoiceId);
            boolean detailOk = invoiceDAO.insertInvoiceDetails(invoiceId, details);
            if (!detailOk) {
                showError("Lưu chi tiết hóa đơn thất bại!");
                return;
            }

            // ✅ THÀNH CÔNG
            confirmed = true;
            JOptionPane.showMessageDialog(this,
                    "Lưu hóa đơn thành công!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        if (cmbContract.getSelectedItem() == null) {
            showError("Vui lòng chọn hợp đồng!");
            return false;
        }

        if (rentalSection.isVisible()) {
            Long rental = MoneyFormatter.getValue(txtRentalAmount);
            if (rental == null || rental <= 0) {
                showError("Tiền thuê phải lớn hơn 0!");
                txtRentalAmount.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Invoice getInvoice() {
        return invoice;
    }
}
