package view;

import model.Contract;
import model.Apartment;
import model.Resident;
import model.Floor;
import model.Building;
import model.User;
import dao.ApartmentDAO;
import dao.ResidentDAO;
import dao.FloorDAO;
import dao.BuildingDAO;
import util.UIConstants;
import util.ModernButton;
import util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Contract Dialog - Legacy popup for Add/Edit Contract FIXED: Building filter
 * applied - users only see apartments in their building
 */
public class ContractDialog extends JDialog {

    private ApartmentDAO apartmentDAO;
    private ResidentDAO residentDAO;
    private FloorDAO floorDAO;
    private BuildingDAO buildingDAO;

    private JComboBox<ApartmentDisplay> apartmentCombo;
    private JComboBox<ResidentDisplay> residentCombo;
    private JComboBox<String> contractTypeCombo;
    private JTextField signedDateField;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField priceField;
    private JTextField depositField;
    private JComboBox<String> statusCombo;

    private Contract contract;
    private boolean confirmed = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // Inner classes for display
    private class ApartmentDisplay {

        Apartment apartment;
        String displayText;

        ApartmentDisplay(Apartment apartment, String displayText) {
            this.apartment = apartment;
            this.displayText = displayText;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    private class ResidentDisplay {

        Resident resident;

        ResidentDisplay(Resident resident) {
            this.resident = resident;
        }

        @Override
        public String toString() {
            return resident.getFullName() + " - " + resident.getPhone();
        }
    }

    public ContractDialog(JFrame parent) {
        this(parent, null);
    }

    public ContractDialog(JFrame parent, Contract contract) {
        super(parent, contract == null ? "Thêm Hợp Đồng" : "Sửa Hợp Đồng", true);
        this.contract = contract;
        this.apartmentDAO = new ApartmentDAO();
        this.residentDAO = new ResidentDAO();
        this.floorDAO = new FloorDAO();
        this.buildingDAO = new BuildingDAO();

        initializeDialog();
        createContent();

        if (contract != null) {
            loadContractData();
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeDialog() {
        setSize(550, 750);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);
    }

    private void createContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createForm(), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JLabel iconLabel = new JLabel(contract == null ? "➕" : "✏️");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));

        JLabel titleLabel = new JLabel(contract == null ? "Thêm Hợp Đồng Mới" : "Sửa Thông Tin Hợp Đồng");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(titleLabel);

        return headerPanel;
    }

    private JPanel createForm() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // Apartment dropdown
        formPanel.add(createFieldLabel("Căn Hộ *"));
        apartmentCombo = new JComboBox<>();
        apartmentCombo.setFont(UIConstants.FONT_REGULAR);
        apartmentCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        apartmentCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(apartmentCombo);
        formPanel.add(Box.createVerticalStrut(15));

        // Resident dropdown
        formPanel.add(createFieldLabel("Cư Dân *"));
        residentCombo = new JComboBox<>();
        residentCombo.setFont(UIConstants.FONT_REGULAR);
        residentCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        residentCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(residentCombo);
        formPanel.add(Box.createVerticalStrut(15));

        loadApartments();
        loadResidents();

        // Contract Type
        formPanel.add(createFieldLabel("Loại Hợp Đồng *"));
        contractTypeCombo = new JComboBox<>(new String[]{"Thuê", "Sở hữu"});
        contractTypeCombo.setFont(UIConstants.FONT_REGULAR);
        contractTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        contractTypeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(contractTypeCombo);
        formPanel.add(Box.createVerticalStrut(15));

        // Signed date
        formPanel.add(createFieldLabel("Ngày Ký (yyyy-MM-dd)"));
        signedDateField = createTextField();
        signedDateField.setToolTipText("Ví dụ: 2026-01-28");
        formPanel.add(signedDateField);
        formPanel.add(Box.createVerticalStrut(15));

        // Start date (for RENTAL)
        formPanel.add(createFieldLabel("Ngày Bắt Đầu (yyyy-MM-dd) *"));
        startDateField = createTextField();
        startDateField.setToolTipText("Chỉ áp dụng cho hợp đồng Thuê");
        formPanel.add(startDateField);
        JLabel startHint = createHintLabel("Chỉ áp dụng cho hợp đồng Thuê");
        formPanel.add(startHint);
        formPanel.add(Box.createVerticalStrut(15));

        // End date (for RENTAL)
        formPanel.add(createFieldLabel("Ngày Kết Thúc (yyyy-MM-dd) *"));
        endDateField = createTextField();
        endDateField.setToolTipText("Chỉ áp dụng cho hợp đồng Thuê");
        formPanel.add(endDateField);
        JLabel endHint = createHintLabel("Chỉ áp dụng cho hợp đồng Thuê");
        formPanel.add(endHint);
        formPanel.add(Box.createVerticalStrut(15));

        // Price (dynamic label)
        formPanel.add(createFieldLabel("Tiền Thuê/Tháng hoặc Giá Mua (VNĐ) *"));
        priceField = createTextField();
        formPanel.add(priceField);
        formPanel.add(Box.createVerticalStrut(15));

        // Deposit
        formPanel.add(createFieldLabel("Tiền Đặt Cọc (VNĐ) *"));
        depositField = createTextField();
        formPanel.add(depositField);
        formPanel.add(Box.createVerticalStrut(15));

        // Status
        formPanel.add(createFieldLabel("Trạng Thái *"));
        statusCombo = new JComboBox<>(new String[]{"ACTIVE", "EXPIRED", "TERMINATED"});
        statusCombo.setFont(UIConstants.FONT_REGULAR);
        statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        statusCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(statusCombo);

        return formPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        if (contract != null && contract.getId() != null) {
            ModernButton manageMembersButton = new ModernButton("👥 Quản lý Thành viên", new Color(103, 58, 183));
            manageMembersButton.setPreferredSize(new Dimension(200, 40));
            manageMembersButton.addActionListener(e -> manageHouseholdMembers());
            leftPanel.add(manageMembersButton);
        }

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        ModernButton cancelButton = new ModernButton("Hủy", UIConstants.TEXT_SECONDARY);
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        ModernButton saveButton = new ModernButton(
                contract == null ? "Thêm" : "Lưu",
                UIConstants.SUCCESS_COLOR
        );
        saveButton.setPreferredSize(new Dimension(100, 40));
        saveButton.addActionListener(e -> saveContract());

        rightPanel.add(cancelButton);
        rightPanel.add(saveButton);

        buttonPanel.add(leftPanel, BorderLayout.WEST);
        buttonPanel.add(rightPanel, BorderLayout.EAST);

        return buttonPanel;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(UIConstants.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createHintLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        label.setForeground(UIConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(UIConstants.FONT_REGULAR);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 2),
                        new EmptyBorder(9, 11, 9, 11)
                ));
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                        new EmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        return field;
    }

    // ✅ FIXED: Apply building filter
    private void loadApartments() {
        DefaultComboBoxModel<ApartmentDisplay> model = new DefaultComboBoxModel<>();

        // ✅ Get current user for building filter
        User currentUser = SessionManager.getInstance().getCurrentUser();

        // ✅ Load apartments with building filter
        List<Apartment> apartments = apartmentDAO.getAllApartments();

        for (Apartment apartment : apartments) {
            Floor floor = floorDAO.getFloorById(apartment.getFloorId());
            if (floor != null) {
                Building building = buildingDAO.getBuildingById(floor.getBuildingId());

                // ✅ FILTER: Only show apartments in user's building (non-ADMIN)
                if (currentUser != null && !currentUser.isAdmin()) {
                    if (building == null || !currentUser.canAccessBuilding(building.getId())) {
                        continue; // Skip apartments not in user's building
                    }
                }

                String displayText = (building != null ? building.getName() : "N/A")
                        + " - Tầng " + floor.getFloorNumber()
                        + " - " + apartment.getRoomNumber();
                model.addElement(new ApartmentDisplay(apartment, displayText));
            }
        }

        apartmentCombo.setModel(model);
    }

    private void loadResidents() {
        DefaultComboBoxModel<ResidentDisplay> model = new DefaultComboBoxModel<>();
        List<Resident> residents = residentDAO.getAllResidents();

        for (Resident resident : residents) {
            model.addElement(new ResidentDisplay(resident));
        }

        residentCombo.setModel(model);
    }

    private void loadContractData() {
        if (contract != null) {
            // Select apartment
            for (int i = 0; i < apartmentCombo.getItemCount(); i++) {
                ApartmentDisplay ad = apartmentCombo.getItemAt(i);
                if (ad.apartment.getId().equals(contract.getApartmentId())) {
                    apartmentCombo.setSelectedIndex(i);
                    break;
                }
            }

            // Select resident
            for (int i = 0; i < residentCombo.getItemCount(); i++) {
                ResidentDisplay rd = residentCombo.getItemAt(i);
                if (rd.resident.getId().equals(contract.getResidentId())) {
                    residentCombo.setSelectedIndex(i);
                    break;
                }
            }

            // Contract type
            contractTypeCombo.setSelectedItem(contract.getContractTypeDisplay());

            // Dates
            signedDateField.setText(contract.getSignedDate() != null ? dateFormat.format(contract.getSignedDate()) : "");
            startDateField.setText(contract.getStartDate() != null ? dateFormat.format(contract.getStartDate()) : "");
            endDateField.setText(contract.getEndDate() != null ? dateFormat.format(contract.getEndDate()) : "");

            // Price
            priceField.setText(contract.getMonthlyRent() != null ? contract.getMonthlyRent().toString() : "");
            depositField.setText(contract.getDeposit() != null ? contract.getDeposit().toString() : "");
            statusCombo.setSelectedItem(contract.getStatus() != null ? contract.getStatus() : "ACTIVE");
        }
    }

    private void saveContract() {
        if (!validateForm()) {
            return;
        }

        try {
            ApartmentDisplay selectedApt = (ApartmentDisplay) apartmentCombo.getSelectedItem();
            ResidentDisplay selectedRes = (ResidentDisplay) residentCombo.getSelectedItem();

            String typeDisplay = (String) contractTypeCombo.getSelectedItem();
            String contractType = "Thuê".equals(typeDisplay) ? "RENTAL" : "OWNERSHIP";

            // Parse dates based on contract type
            Date signedDate = null;
            if (!signedDateField.getText().trim().isEmpty()) {
                signedDate = dateFormat.parse(signedDateField.getText().trim());
            }

            Date startDate = null;
            Date endDate = null;

            if ("RENTAL".equals(contractType)) {
                if (!startDateField.getText().trim().isEmpty()) {
                    startDate = dateFormat.parse(startDateField.getText().trim());
                }
                if (!endDateField.getText().trim().isEmpty()) {
                    endDate = dateFormat.parse(endDateField.getText().trim());
                }
            }

            BigDecimal price = new BigDecimal(priceField.getText().trim());
            BigDecimal deposit = new BigDecimal(depositField.getText().trim());
            String status = (String) statusCombo.getSelectedItem();

            if (contract == null) {
                contract = new Contract();
            }

            contract.setApartmentId(selectedApt.apartment.getId());
            contract.setResidentId(selectedRes.resident.getId());
            contract.setContractType(contractType);
            contract.setSignedDate(signedDate);
            contract.setStartDate(startDate);
            contract.setEndDate(endDate);
            contract.setMonthlyRent(price);
            contract.setDeposit(deposit);
            contract.setStatus(status);

            confirmed = true;
            dispose();
        } catch (ParseException e) {
            showError("Định dạng ngày không đúng! Vui lòng nhập theo định dạng yyyy-MM-dd");
        } catch (NumberFormatException e) {
            showError("Giá tiền không hợp lệ!");
        }
    }

    private boolean validateForm() {
        if (apartmentCombo.getSelectedItem() == null) {
            showError("Vui lòng chọn căn hộ!");
            return false;
        }

        if (residentCombo.getSelectedItem() == null) {
            showError("Vui lòng chọn cư dân!");
            return false;
        }

        String typeDisplay = (String) contractTypeCombo.getSelectedItem();
        boolean isRental = "Thuê".equals(typeDisplay);

        // Validate dates for RENTAL
        if (isRental) {
            if (startDateField.getText().trim().isEmpty()) {
                showError("Hợp đồng thuê phải có ngày bắt đầu!");
                startDateField.requestFocus();
                return false;
            }

            if (endDateField.getText().trim().isEmpty()) {
                showError("Hợp đồng thuê phải có ngày kết thúc!");
                endDateField.requestFocus();
                return false;
            }

            try {
                Date start = dateFormat.parse(startDateField.getText().trim());
                Date end = dateFormat.parse(endDateField.getText().trim());

                if (end.before(start) || end.equals(start)) {
                    showError("Ngày kết thúc phải sau ngày bắt đầu!");
                    return false;
                }
            } catch (ParseException e) {
                showError("Định dạng ngày không đúng!");
                return false;
            }
        }

        // Price
        if (priceField.getText().trim().isEmpty()) {
            showError("Vui lòng nhập giá tiền!");
            priceField.requestFocus();
            return false;
        }

        try {
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Giá tiền phải lớn hơn 0!");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Giá tiền không hợp lệ!");
            return false;
        }

        // Deposit
        if (depositField.getText().trim().isEmpty()) {
            showError("Vui lòng nhập tiền cọc!");
            depositField.requestFocus();
            return false;
        }

        try {
            BigDecimal deposit = new BigDecimal(depositField.getText().trim());
            if (deposit.compareTo(BigDecimal.ZERO) < 0) {
                showError("Tiền cọc không được âm!");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Tiền cọc không hợp lệ!");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Cảnh Báo",
                JOptionPane.WARNING_MESSAGE
        );
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Contract getContract() {
        return contract;
    }

    private void manageHouseholdMembers() {
        if (contract == null || contract.getId() == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng lưu hợp đồng trước khi quản lý thành viên!",
                    "Thông Báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        HouseholdMemberManagementDialog dialog = new HouseholdMemberManagementDialog(this, contract.getId());
        dialog.setVisible(true);
    }
}
