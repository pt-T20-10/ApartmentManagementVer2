package view;

import dao.*;
import model.*;
import util.UIConstants;
import util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import javax.swing.DefaultListCellRenderer;

/**
 * Contract Form Dialog with DYNAMIC UI based on contract type Fixed: Binding
 * data correctly in Edit Mode & Service Update Logic
 */
public class ContractFormDialog extends JDialog {

    private ContractDAO contractDAO;
    private ApartmentDAO apartmentDAO;
    private BuildingDAO buildingDAO;
    private FloorDAO floorDAO;
    private ResidentDAO residentDAO;
    private ServiceDAO serviceDAO;
    private ContractServiceDAO contractServiceDAO;

    private Contract contract;
    private boolean isEditMode;
    private boolean isConfirmed = false;

    // Form components - Contract Info
    private JTextField txtContractNumber;
    private JComboBox<String> cmbContractType;

    // Cascade filters
    private JComboBox<BuildingDisplay> cmbBuilding;
    private JComboBox<FloorDisplay> cmbFloor;
    private JComboBox<ApartmentDisplay> cmbApartment;

    // Resident fields
    private JTextField txtResidentName;
    private JTextField txtResidentPhone;
    private JTextField txtResidentIdentityCard;
    private JComboBox<String> cmbResidentGender;
    private JSpinner spnResidentDob;
    private JTextField txtResidentEmail;

    // Contract dates (visibility depends on type)
    private JLabel lblSignedDate;
    private JSpinner spnSignedDate;
    private JLabel lblStartDate;
    private JSpinner spnStartDate;
    private JLabel lblEndDate;
    private JSpinner spnEndDate;
    private JCheckBox chkIndefinite;
    private JPanel datesSection;

    // Financial (dynamic label)
    private JLabel lblPriceField;
    private JTextField txtPriceAmount;
    private JTextField txtDepositAmount;

    // Services
    private JPanel servicesPanel;
    private List<JCheckBox> serviceCheckboxes = new ArrayList<>();

    // Notes
    private JTextArea txtNotes;

    private boolean isUpdatingCombos = false;

    public ContractFormDialog(JFrame parent, Contract contract) {
        super(parent, contract == null ? "Tạo Hợp Đồng Mới" : "Chỉnh Sửa Hợp Đồng", true);

        this.contractDAO = new ContractDAO();
        this.apartmentDAO = new ApartmentDAO();
        this.buildingDAO = new BuildingDAO();
        this.floorDAO = new FloorDAO();
        this.residentDAO = new ResidentDAO();
        this.serviceDAO = new ServiceDAO();
        this.contractServiceDAO = new ContractServiceDAO();

        this.contract = contract != null ? contract : new Contract();
        this.isEditMode = contract != null && contract.getId() != null;

        initComponents();
        loadData(); // Load buildings & services

        if (isEditMode) {
            loadContractData();
        } else {
            // Set defaults for new contract
            txtContractNumber.setText(contractDAO.generateContractNumber());
            // Try to auto-select if only 1 building
            if (cmbBuilding.getItemCount() > 0) {
                // logic handled in loadData->loadBuildingData
            }
        }

        setSize(950, 800);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        mainPanel.add(createHeader());
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(createContractInfoSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createLocationSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createResidentSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createDatesSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createFinancialSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createServicesSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createNotesSection());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel iconLabel = new JLabel(isEditMode ? "✏️" : "➕");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel titleLabel = new JLabel(isEditMode ? "Chỉnh Sửa Hợp Đồng" : "Tạo Hợp Đồng Mới");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(33, 33, 33));

        panel.add(iconLabel);
        panel.add(titleLabel);

        return panel;
    }

    private JPanel createContractInfoSection() {
        JPanel section = createSection("Thông Tin Hợp Đồng");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Số hợp đồng:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtContractNumber = new JTextField();
        txtContractNumber.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtContractNumber.setPreferredSize(new Dimension(0, 32));
        txtContractNumber.setEnabled(false);
        txtContractNumber.setBackground(new Color(245, 245, 245));
        section.add(txtContractNumber, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        section.add(createLabel("Loại hợp đồng:", true), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        cmbContractType = new JComboBox<>(new String[]{"Thuê", "Sở hữu"});
        cmbContractType.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbContractType.setPreferredSize(new Dimension(0, 32));
        cmbContractType.addActionListener(e -> onContractTypeChanged());
        section.add(cmbContractType, gbc);

        return section;
    }

    private void onContractTypeChanged() {
        String selectedType = (String) cmbContractType.getSelectedItem();
        boolean isRental = "Thuê".equals(selectedType);

        if (lblPriceField != null) {
            lblPriceField.setText(isRental
                    ? "<html>Tiền thuê/tháng: <font color='red'>*</font></html>"
                    : "<html>Giá mua: <font color='red'>*</font></html>");
        }

        if (lblStartDate != null && lblEndDate != null) {
            lblStartDate.setVisible(isRental);
            spnStartDate.setVisible(isRental);
            lblEndDate.setVisible(isRental);
            spnEndDate.setVisible(isRental);
            if (chkIndefinite != null) {
                chkIndefinite.setVisible(isRental);
            }
        }

        if (datesSection != null) {
            datesSection.revalidate();
            datesSection.repaint();
        }
    }

    private JPanel createLocationSection() {
        JPanel section = createSection("Vị Trí Căn Hộ");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Tòa nhà:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        cmbBuilding = new JComboBox<>();
        cmbBuilding.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbBuilding.setPreferredSize(new Dimension(0, 32));
        cmbBuilding.addActionListener(e -> {
            if (!isUpdatingCombos) {
                onBuildingChanged();
            }
        });
        section.add(cmbBuilding, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        section.add(createLabel("Tầng:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        cmbFloor = new JComboBox<>();
        cmbFloor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbFloor.setPreferredSize(new Dimension(0, 32));
        cmbFloor.addActionListener(e -> {
            if (!isUpdatingCombos) {
                onFloorChanged();
            }
        });
        section.add(cmbFloor, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        section.add(createLabel("Căn hộ:", true), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        cmbApartment = new JComboBox<>();
        cmbApartment.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbApartment.setPreferredSize(new Dimension(0, 32));
        section.add(cmbApartment, gbc);

        cmbBuilding.setRenderer(new PlaceholderRenderer("-- Chọn tòa nhà --"));
        cmbFloor.setRenderer(new PlaceholderRenderer("-- Chọn tầng --"));
        cmbApartment.setRenderer(new PlaceholderRenderer("-- Chọn căn hộ --"));

        return section;
    }

    private JPanel createResidentSection() {
        JPanel section = createSection("Thông Tin Khách Thuê / Chủ Hộ");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Họ và tên:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        txtResidentName = new JTextField();
        txtResidentName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtResidentName.setPreferredSize(new Dimension(0, 32));
        section.add(txtResidentName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        section.add(createLabel("Số điện thoại:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtResidentPhone = new JTextField();
        txtResidentPhone.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtResidentPhone.setPreferredSize(new Dimension(0, 32));
        section.add(txtResidentPhone, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        section.add(createLabel("CCCD/CMND:", true), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        txtResidentIdentityCard = new JTextField();
        txtResidentIdentityCard.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtResidentIdentityCard.setPreferredSize(new Dimension(0, 32));
        section.add(txtResidentIdentityCard, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        section.add(createLabel("Giới tính:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        cmbResidentGender = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cmbResidentGender.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbResidentGender.setPreferredSize(new Dimension(0, 32));
        section.add(cmbResidentGender, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        section.add(createLabel("Ngày sinh:", false), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        spnResidentDob = createDateSpinner();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -30);
        spnResidentDob.setValue(cal.getTime());
        section.add(spnResidentDob, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        section.add(createLabel("Email:", false), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        txtResidentEmail = new JTextField();
        txtResidentEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtResidentEmail.setPreferredSize(new Dimension(0, 32));
        section.add(txtResidentEmail, gbc);

        return section;
    }

    private JPanel createDatesSection() {
        datesSection = createSection("Thời Hạn Hợp Đồng");
        datesSection.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        lblSignedDate = createLabel("Ngày ký:", true);
        datesSection.add(lblSignedDate, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        spnSignedDate = createDateSpinner();
        datesSection.add(spnSignedDate, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        lblStartDate = createLabel("Ngày bắt đầu:", true);
        datesSection.add(lblStartDate, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        spnStartDate = createDateSpinner();
        datesSection.add(spnStartDate, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        lblEndDate = createLabel("Ngày kết thúc:", true);
        datesSection.add(lblEndDate, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        spnEndDate = createDateSpinner();
        datesSection.add(spnEndDate, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 2;
        chkIndefinite = new JCheckBox("Vô thời hạn");
        chkIndefinite.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chkIndefinite.setBackground(Color.WHITE);
        chkIndefinite.addActionListener(e -> spnEndDate.setEnabled(!chkIndefinite.isSelected()));
        datesSection.add(chkIndefinite, gbc);

        onContractTypeChanged();

        return datesSection;
    }

    private JPanel createFinancialSection() {
        JPanel section = createSection("Thông Tin Tài Chính");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        section.add(createLabel("Tiền cọc:", true), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtDepositAmount = MoneyFormatter.createMoneyField(32);
        section.add(txtDepositAmount, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblDepositCurrency = new JLabel("VNĐ");
        lblDepositCurrency.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDepositCurrency.setForeground(UIConstants.PRIMARY_COLOR);
        section.add(lblDepositCurrency, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        lblPriceField = createLabel("Tiền thuê/tháng:", true);
        section.add(lblPriceField, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtPriceAmount = MoneyFormatter.createMoneyField(32);
        section.add(txtPriceAmount, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblPriceCurrency = new JLabel("VNĐ");
        lblPriceCurrency.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPriceCurrency.setForeground(UIConstants.PRIMARY_COLOR);
        section.add(lblPriceCurrency, gbc);

        return section;
    }

    private JPanel createServicesSection() {
        JPanel section = createSection("Dịch Vụ Áp Dụng");
        section.setLayout(new BorderLayout());

        servicesPanel = new JPanel();
        servicesPanel.setLayout(new BoxLayout(servicesPanel, BoxLayout.Y_AXIS));
        servicesPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(servicesPanel);
        scroll.setPreferredSize(new Dimension(0, 100));
        scroll.setBorder(null);

        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    private JPanel createNotesSection() {
        JPanel section = createSection("Ghi Chú");
        section.setLayout(new BorderLayout());

        txtNotes = new JTextArea(3, 20);
        txtNotes.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(txtNotes);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(224, 224, 224)));

        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_COLOR));

        JButton btnCancel = createButton("Hủy", new Color(158, 158, 158));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = createButton(isEditMode ? "Lưu" : "Tạo Hợp Đồng", UIConstants.PRIMARY_COLOR);
        btnSave.addActionListener(e -> saveContract());

        panel.add(btnCancel);
        panel.add(btnSave);

        return panel;
    }

    // ===== LOGIC & DATA LOADING =====
    private void onBuildingChanged() {
        BuildingDisplay selected = (BuildingDisplay) cmbBuilding.getSelectedItem();
        if (selected == null) {
            cmbFloor.removeAllItems();
            cmbFloor.addItem(null);
            cmbApartment.removeAllItems();
            cmbApartment.addItem(null);
            return;
        }
        loadFloorData(selected.building.getId());
    }

    private void onFloorChanged() {
        FloorDisplay selected = (FloorDisplay) cmbFloor.getSelectedItem();
        if (selected == null) {
            cmbApartment.removeAllItems();
            cmbApartment.addItem(null);
            return;
        }
        loadApartmentData(selected.floor.getId());
    }

    // Load initial data
    private void loadData() {
        isUpdatingCombos = true;
        try {
            if (!isEditMode) {
                cmbBuilding.addItem(null);
            }
            List<Building> buildings = buildingDAO.getAllBuildings();
            for (Building building : buildings) {
                cmbBuilding.addItem(new BuildingDisplay(building));
            }

            // Auto select if only 1 building (Staff/Manager)
            if (buildings.size() == 1) {
                cmbBuilding.setSelectedIndex(isEditMode ? 0 : 1);
                cmbBuilding.setEnabled(false);
                loadFloorData(buildings.get(0).getId());
            }
        } finally {
            isUpdatingCombos = false;
        }

        loadServices();
    }

    private void loadFloorData(Long buildingId) {
        boolean oldState = isUpdatingCombos;
        isUpdatingCombos = true;
        try {
            cmbFloor.removeAllItems();
            cmbFloor.addItem(null);
            List<Floor> floors = floorDAO.getFloorsByBuildingId(buildingId);
            for (Floor f : floors) {
                cmbFloor.addItem(new FloorDisplay(f));
            }
            cmbFloor.setSelectedIndex(0);
            cmbApartment.removeAllItems();
            cmbApartment.addItem(null);
        } finally {
            isUpdatingCombos = oldState;
        }
    }

    private void loadApartmentData(Long floorId) {
        boolean oldState = isUpdatingCombos;
        isUpdatingCombos = true;
        try {
            cmbApartment.removeAllItems();
            cmbApartment.addItem(null);
            List<Apartment> apartments = apartmentDAO.getApartmentsByFloorId(floorId);
            for (Apartment a : apartments) {
                // Edit Mode: Show current apt + available ones
                // Create Mode: Show only available
                if (isEditMode || "AVAILABLE".equals(a.getStatus())) {
                    cmbApartment.addItem(new ApartmentDisplay(a));
                }
            }
            cmbApartment.setSelectedIndex(0);
        } finally {
            isUpdatingCombos = oldState;
        }
    }

    private void loadServices() {
        List<Service> services = serviceDAO.getAllServices();
        serviceCheckboxes.clear();
        servicesPanel.removeAll();
        for (Service service : services) {
            JCheckBox cb = new JCheckBox(service.getServiceName() + " ("
                    + service.getUnitPrice().toPlainString() + " VNĐ/" + service.getUnit() + ")");
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cb.setBackground(Color.WHITE);
            cb.putClientProperty("service", service);
            if (service.isMandatory()) {
                cb.setSelected(true);
                cb.setEnabled(false);
            }
            serviceCheckboxes.add(cb);
            servicesPanel.add(cb);
        }
        servicesPanel.revalidate();
        servicesPanel.repaint();
    }

    // ✅ FIX: Load data for Edit Mode with sequential updates
    private void loadContractData() {
        // 1. Basic Info
        txtContractNumber.setText(contract.getContractNumber());
        cmbContractType.setSelectedItem("RENTAL".equals(contract.getContractType()) ? "Thuê" : "Sở hữu");

        // Disable fixed fields
        cmbContractType.setEnabled(false);
        cmbBuilding.setEnabled(false);
        cmbFloor.setEnabled(false);
        cmbApartment.setEnabled(false);

        // Dates & Money
        if (contract.getSignedDate() != null) {
            spnSignedDate.setValue(contract.getSignedDate());
        }
        if (contract.isRental()) {
            if (contract.getStartDate() != null) {
                spnStartDate.setValue(contract.getStartDate());
            }
            if (contract.getEndDate() != null) {
                spnEndDate.setValue(contract.getEndDate());
            } else {
                chkIndefinite.setSelected(true);
                spnEndDate.setEnabled(false);
            }
        }

        MoneyFormatter.setValue(txtDepositAmount, contract.getDepositAmount().longValue());
        if (contract.getMonthlyRent() != null) {
            MoneyFormatter.setValue(txtPriceAmount, contract.getMonthlyRent().longValue());
        }
        if (contract.getNotes() != null) {
            txtNotes.setText(contract.getNotes());
        }

        // 2. Cascade Selection (FIXED HERE)
        Apartment apartment = apartmentDAO.getApartmentById(contract.getApartmentId());
        if (apartment != null) {
            Floor floor = floorDAO.getFloorById(apartment.getFloorId());
            if (floor != null) {
                // Call explicit select to trigger loading
                selectBuildingAndFloor(floor.getBuildingId(), floor.getId(), apartment.getId());
            }
        }

        // 3. Resident
        Resident r = residentDAO.getResidentById(contract.getResidentId());
        if (r != null) {
            txtResidentName.setText(r.getFullName());
            txtResidentPhone.setText(r.getPhone());
            txtResidentIdentityCard.setText(r.getIdentityCard());
            cmbResidentGender.setSelectedItem(r.getGender());
            if (r.getDob() != null) {
                spnResidentDob.setValue(r.getDob());
            }
            txtResidentEmail.setText(r.getEmail());
        }

        // 4. Services
        List<ContractService> css = contractServiceDAO.getServicesByContract(contract.getId());
        for (JCheckBox cb : serviceCheckboxes) {
            Service s = (Service) cb.getClientProperty("service");
            for (ContractService cs : css) {
                if (cs.getServiceId().equals(s.getId())) {
                    cb.setSelected(true);
                    break;
                }
            }
        }
    }

    // ✅ FIX: Force data loading when selecting items via code
    private void selectBuildingAndFloor(Long buildingId, Long floorId, Long apartmentId) {
        boolean oldState = isUpdatingCombos;
        isUpdatingCombos = true; // Prevent events during batch update
        try {
            // 1. Select Building
            for (int i = 0; i < cmbBuilding.getItemCount(); i++) {
                BuildingDisplay bd = cmbBuilding.getItemAt(i);
                if (bd != null && bd.building.getId().equals(buildingId)) {
                    cmbBuilding.setSelectedIndex(i);
                    break;
                }
            }

            // 2. FORCE Load Floors (since events are disabled)
            loadFloorData(buildingId);
            isUpdatingCombos = true;

            // 3. Select Floor
            for (int i = 0; i < cmbFloor.getItemCount(); i++) {
                FloorDisplay fd = cmbFloor.getItemAt(i);
                if (fd != null && fd.floor.getId().equals(floorId)) {
                    cmbFloor.setSelectedIndex(i);
                    break;
                }
            }

            // 4. FORCE Load Apartments
            loadApartmentData(floorId);
            isUpdatingCombos = true;

            // 5. Select Apartment
            for (int i = 0; i < cmbApartment.getItemCount(); i++) {
                ApartmentDisplay ad = cmbApartment.getItemAt(i);
                if (ad != null && ad.apartment.getId().equals(apartmentId)) {
                    cmbApartment.setSelectedIndex(i);
                    break;
                }
            }

        } finally {
            isUpdatingCombos = false; // Re-enable events
        }
    }

    private void saveContract() {
        if (!validateForm()) {
            return;
        }
        try {
            // 1. Resident
            Resident resident = new Resident();
            if (isEditMode) {
                resident = residentDAO.getResidentById(contract.getResidentId());
            }

            resident.setFullName(txtResidentName.getText().trim());
            resident.setPhone(txtResidentPhone.getText().trim());
            resident.setIdentityCard(txtResidentIdentityCard.getText().trim());
            resident.setGender((String) cmbResidentGender.getSelectedItem());
            resident.setDob((Date) spnResidentDob.getValue());
            resident.setEmail(txtResidentEmail.getText().trim());

            if (isEditMode) {
                residentDAO.updateResident(resident);
            } else {
                if (!residentDAO.insertResident(resident)) {
                    JOptionPane.showMessageDialog(this, "Lỗi tạo cư dân!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                contract.setResidentId(resident.getId());
            }

            // 2. Contract
            if (!isEditMode) {
                ApartmentDisplay ad = (ApartmentDisplay) cmbApartment.getSelectedItem();
                contract.setApartmentId(ad.apartment.getId());
                contract.setContractNumber(contractDAO.generateContractNumber());
            }

            contract.setContractType("Thuê".equals(cmbContractType.getSelectedItem()) ? "RENTAL" : "OWNERSHIP");
            contract.setSignedDate((Date) spnSignedDate.getValue());
            if (contract.isRental()) {
                contract.setStartDate((Date) spnStartDate.getValue());
                contract.setEndDate(chkIndefinite.isSelected() ? null : (Date) spnEndDate.getValue());
            } else {
                contract.setStartDate(null);
                contract.setEndDate(null);
            }
            contract.setDepositAmount(BigDecimal.valueOf(MoneyFormatter.getValue(txtDepositAmount)));
            contract.setMonthlyRent(BigDecimal.valueOf(MoneyFormatter.getValue(txtPriceAmount)));
            contract.setNotes(txtNotes.getText().trim());
            contract.setStatus("ACTIVE");

            boolean success = isEditMode ? contractDAO.updateContract(contract) : contractDAO.insertContract(contract);
            if (success) {
                // ✅ FIX: LOGIC UPDATE DỊCH VỤ (XÓA CŨ -> THÊM MỚI)
                if (contract.getId() != null) {
                    contractServiceDAO.deleteServicesByContract(contract.getId()); // Clean old

                    List<Long> sIds = new ArrayList<>();
                    for (JCheckBox cb : serviceCheckboxes) {
                        if (cb.isSelected()) {
                            sIds.add(((Service) cb.getClientProperty("service")).getId());
                        }
                    }
                    if (!sIds.isEmpty()) {
                        contractServiceDAO.insertServicesForContract(contract.getId(), sIds,
                                contract.getStartDate() != null ? contract.getStartDate() : new Date());
                    }
                }

                // Update Apartment Status if new
                if (!isEditMode) {
                    ApartmentDisplay ad = (ApartmentDisplay) cmbApartment.getSelectedItem();
                    Apartment apt = ad.apartment;
                    apt.setStatus(contract.isRental() ? "RENTED" : "OWNED");
                    apartmentDAO.updateApartment(apt);
                }

                isConfirmed = true;
                JOptionPane.showMessageDialog(this, "Thành công!");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi lưu hợp đồng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        if (cmbApartment.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Chọn căn hộ!");
            return false;
        }
        if (txtResidentName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập tên cư dân!");
            return false;
        }
        if (MoneyFormatter.getValue(txtPriceAmount) <= 0) {
            JOptionPane.showMessageDialog(this, "Nhập giá!");
            return false;
        }
        return true;
    }

    // Components helpers
    private JPanel createSection(String title) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR), title, TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), new Color(66, 66, 66)));
        return p;
    }

    private JLabel createLabel(String text, boolean req) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (req) {
            l.setText("<html>" + text + " <font color='red'>*</font></html>");
        }
        return l;
    }

    private JSpinner createDateSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        s.setPreferredSize(new Dimension(0, 32));
        return s;
    }

    private JButton createButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(140, 38));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // Display wrappers
    private class BuildingDisplay {

        Building building;

        BuildingDisplay(Building b) {
            this.building = b;
        }

        @Override
        public String toString() {
            return building.getName();
        }
    }

    private class FloorDisplay {

        Floor floor;

        FloorDisplay(Floor f) {
            this.floor = f;
        }

        @Override
        public String toString() {
            return floor.getName();
        }
    }

    private class ApartmentDisplay {

        Apartment apartment;

        ApartmentDisplay(Apartment a) {
            this.apartment = a;
        }

        @Override
        public String toString() {
            return apartment.getRoomNumber() + " - " + apartment.getArea() + "m²";
        }
    }

    private class PlaceholderRenderer extends DefaultListCellRenderer {

        String ph;

        PlaceholderRenderer(String p) {
            this.ph = p;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) {
                setText(ph);
                setForeground(Color.GRAY);
            }
            return this;
        }
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public Contract getContract() {
        return contract;
    }
}
