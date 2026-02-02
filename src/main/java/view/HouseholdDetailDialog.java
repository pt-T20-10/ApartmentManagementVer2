package view;

import dao.HouseholdMemberDAO;
import dao.ResidentDAO;
import dao.ContractHouseholdViewDAO;
import model.ContractHouseholdViewModel;
import model.HouseholdMember;
import model.Resident;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Household Detail Dialog Hiển thị thông tin Chủ hộ + Danh sách thành viên
 */
public class HouseholdDetailDialog extends JDialog {

    private ContractHouseholdViewModel household;
    private HouseholdMemberDAO memberDAO;
    private ResidentDAO residentDAO;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private JFrame parentFrame;

    public HouseholdDetailDialog(JFrame parent, ContractHouseholdViewModel household) {
        super(parent, "Chi Tiết Hộ Gia Đình", true);

        this.household = household;
        this.memberDAO = new HouseholdMemberDAO();
        this.residentDAO = new ResidentDAO();
        this.parentFrame = parent;

        initializeDialog();
        createContent();

        setLocationRelativeTo(parent);
    }

    private void initializeDialog() {
        setSize(1200, 700); // Increased to 1200px for better layout
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);
    }

    private void createContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        mainPanel.add(createHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        centerPanel.add(createHouseholderCard());
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(createMembersTable());

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JLabel iconLabel = new JLabel("🏠");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));

        JLabel titleLabel = new JLabel("Chi Tiết Hộ Gia Đình");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("Căn hộ " + household.getApartmentNumber()
                + " - " + household.getFloorName());
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(UIConstants.TEXT_SECONDARY);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(subtitleLabel);

        return headerPanel;
    }

    private JPanel createHouseholderCard() {
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(15, 20, 15, 20) // Reduced padding
        ));

        // Header with Edit button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel headerLabel = new JLabel("👤 THÔNG TIN CHỦ HỘ");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerLabel.setForeground(new Color(25, 118, 210));

        JButton editHouseholderBtn = new JButton("✏️ Sửa thông tin");
        editHouseholderBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        editHouseholderBtn.setBackground(new Color(255, 152, 0));
        editHouseholderBtn.setForeground(Color.WHITE);
        editHouseholderBtn.setFocusPainted(false);
        editHouseholderBtn.setBorderPainted(false);
        editHouseholderBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editHouseholderBtn.setPreferredSize(new Dimension(130, 32));
        editHouseholderBtn.addActionListener(e -> editHouseholder());

        headerPanel.add(headerLabel, BorderLayout.WEST);
        headerPanel.add(editHouseholderBtn, BorderLayout.EAST);

        // Info grid - more compact
        JPanel infoPanel = new JPanel(new GridLayout(3, 3, 40, 12)); // Reduced vertical gap
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(12, 0, 0, 0)); // Reduced top margin

        // Row 1
        addInfoField(infoPanel, "Họ tên:", household.getResidentFullName());
        addInfoField(infoPanel, "Giới tính:", household.getResidentGender());
        addInfoField(infoPanel, "Điện thoại:", household.getResidentPhone());

        // Row 2
        addInfoField(infoPanel, "CCCD:", household.getResidentIdentityCard());
        addInfoField(infoPanel, "Email:", household.getResidentEmail() != null ? household.getResidentEmail() : "-");
        addInfoField(infoPanel, "Ngày sinh:",
                household.getResidentDob() != null ? dateFormat.format(household.getResidentDob()) : "-");

        // Row 3
        addInfoField(infoPanel, "Trạng thái:", household.getResidencyStatus());
        addInfoField(infoPanel, "Tổng số người:", household.getTotalPeopleDisplay());
        // Empty cell for alignment
        infoPanel.add(new JPanel() {
            {
                setBackground(Color.WHITE);
            }
        });

        // Use BorderLayout to force full width
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        cardPanel.add(infoPanel, BorderLayout.CENTER);

        return cardPanel;
    }

    private void addInfoField(JPanel parent, String label, String value) {
        JPanel fieldPanel = new JPanel();
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        fieldPanel.setBackground(Color.WHITE);

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        labelComp.setForeground(UIConstants.TEXT_SECONDARY);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueComp = new JLabel(value != null && !value.isEmpty() ? value : "-");
        valueComp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueComp.setForeground(UIConstants.TEXT_PRIMARY);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        fieldPanel.add(labelComp);
        fieldPanel.add(Box.createVerticalStrut(4)); // Reduced gap
        fieldPanel.add(valueComp);

        parent.add(fieldPanel);
    }

    private JPanel createMembersTable() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Header with Add Member button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        // Show total members including householder
        int totalMembers = household.getHouseholdMemberCount(); // Members only
        String memberCountText = totalMembers > 0
                ? household.getHouseholdMemberCount() + " người"
                : "Không có thành viên";

        JLabel headerLabel = new JLabel("👥 DANH SÁCH THÀNH VIÊN (" + memberCountText + ")");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerLabel.setForeground(new Color(103, 58, 183));

        JButton addMemberBtn = new JButton("➕ Thêm thành viên");
        addMemberBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addMemberBtn.setBackground(new Color(76, 175, 80));
        addMemberBtn.setForeground(Color.WHITE);
        addMemberBtn.setFocusPainted(false);
        addMemberBtn.setBorderPainted(false);
        addMemberBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMemberBtn.setPreferredSize(new Dimension(160, 35));
        addMemberBtn.addActionListener(e -> addMember());

        headerPanel.add(headerLabel, BorderLayout.WEST);
        headerPanel.add(addMemberBtn, BorderLayout.EAST);

        tablePanel.add(headerPanel, BorderLayout.NORTH);

        // Table with Actions column
        String[] columns = {"STT", "Họ tên", "Quan hệ", "Giới tính", "Ngày sinh", "CCCD", "SĐT", "Trạng thái", "Thao tác"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8; // Only Actions column
            }
        };

        JTable memberTable = new JTable(tableModel);
        memberTable.setFont(UIConstants.FONT_REGULAR);
        memberTable.setRowHeight(45);
        memberTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberTable.setShowGrid(true);
        memberTable.setGridColor(UIConstants.BORDER_COLOR);

        JTableHeader header = memberTable.getTableHeader();
        header.setFont(UIConstants.FONT_HEADING);
        header.setBackground(new Color(250, 250, 250));
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.BORDER_COLOR));

        // Column widths (optimized for 1200px dialog)
        memberTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // STT
        memberTable.getColumnModel().getColumn(1).setPreferredWidth(160);  // Họ tên
        memberTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Quan hệ
        memberTable.getColumnModel().getColumn(3).setPreferredWidth(90);   // Giới tính
        memberTable.getColumnModel().getColumn(4).setPreferredWidth(110);  // Ngày sinh
        memberTable.getColumnModel().getColumn(5).setPreferredWidth(120);  // CCCD
        memberTable.getColumnModel().getColumn(6).setPreferredWidth(120);  // SĐT
        memberTable.getColumnModel().getColumn(7).setPreferredWidth(110);  // Trạng thái
        memberTable.getColumnModel().getColumn(8).setPreferredWidth(210);  // Thao tác (70 + 110 + spacing)

        // Load data
        List<HouseholdMember> members = memberDAO.getByContractId(household.getContractId());
        int stt = 1;
        for (HouseholdMember member : members) {
            String dobStr = member.getDob() != null ? dateFormat.format(member.getDob()) : "";
            String status = member.isActive() ? "● Đang ở" : "○ Đã rời";

            Object[] row = {
                stt++,
                member.getFullName(),
                member.getRelationship(),
                member.getGender(),
                dobStr,
                member.getIdentityCard() != null ? member.getIdentityCard() : "",
                member.getPhone() != null ? member.getPhone() : "",
                status,
                member.getId() // Store ID for actions
            };
            tableModel.addRow(row);
        }

        // Button renderer and editor
        memberTable.getColumnModel().getColumn(8).setCellRenderer(new MemberActionRenderer());
        memberTable.getColumnModel().getColumn(8).setCellEditor(new MemberActionEditor(new JCheckBox(), members));

        JScrollPane scrollPane = new JScrollPane(memberTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scrollPane.setPreferredSize(new Dimension(0, 300)); // Increased from 250 to 300

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setPreferredSize(new Dimension(120, 45));
        closeButton.setBackground(UIConstants.TEXT_SECONDARY);
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(closeButton);

        return buttonPanel;
    }

    /**
     * Edit householder information
     */
    private void editHouseholder() {
        Resident resident = residentDAO.getResidentById(household.getResidentId());
        if (resident == null) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy thông tin cư dân!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ResidentDialog dialog = new ResidentDialog(parentFrame, resident);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Resident updatedResident = dialog.getResident();
            if (residentDAO.updateResident(updatedResident)) {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thông tin chủ hộ thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);

                // Refresh dialog
                dispose();
                // Notify parent to reload
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thông tin chủ hộ thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Add new household member
     */
    private void addMember() {
        HouseholdMemberDialog dialog = new HouseholdMemberDialog(this, household.getContractId());
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            HouseholdMember member = dialog.getMember();
            if (memberDAO.insert(member)) {
                JOptionPane.showMessageDialog(this,
                        "Thêm thành viên thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);

                // Reload household data to update member count
                ContractHouseholdViewDAO householdDAO = new ContractHouseholdViewDAO();
                ContractHouseholdViewModel updatedHousehold = householdDAO.getContractsByBuilding(
                        household.getBuildingId()).stream()
                        .filter(h -> h.getContractId().equals(household.getContractId()))
                        .findFirst()
                        .orElse(household);

                // Refresh dialog with updated data
                dispose();
                HouseholdDetailDialog newDialog = new HouseholdDetailDialog(parentFrame, updatedHousehold);
                newDialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Thêm thành viên thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Edit household member
     */
    private void editMember(Long memberId) {
        HouseholdMember member = memberDAO.getById(memberId);
        if (member == null) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy thành viên!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        HouseholdMemberDialog dialog = new HouseholdMemberDialog(this, member, household.getContractId());
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            HouseholdMember updatedMember = dialog.getMember();
            if (memberDAO.update(updatedMember)) {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thành viên thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);

                // Reload household data
                reloadDialog();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thành viên thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Toggle member active status
     */
    private void toggleMemberStatus(Long memberId) {
        HouseholdMember member = memberDAO.getById(memberId);
        if (member == null) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy thành viên!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String action = member.isActive() ? "đã rời đi" : "đang ở";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Đánh dấu " + member.getFullName() + " " + action + "?",
                "Xác Nhận",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            member.setActive(!member.isActive());
            if (memberDAO.update(member)) {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật trạng thái thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);

                // Reload household data
                reloadDialog();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật trạng thái thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Reload dialog with updated household data
     */
    private void reloadDialog() {
        ContractHouseholdViewDAO householdDAO = new ContractHouseholdViewDAO();
        ContractHouseholdViewModel updatedHousehold = householdDAO.getContractsByBuilding(
                household.getBuildingId()).stream()
                .filter(h -> h.getContractId().equals(household.getContractId()))
                .findFirst()
                .orElse(household);

        dispose();
        HouseholdDetailDialog newDialog = new HouseholdDetailDialog(parentFrame, updatedHousehold);
        newDialog.setVisible(true);
    }

    /**
     * Member Action Button Renderer
     */
    class MemberActionRenderer extends JPanel implements TableCellRenderer {

        private JButton editBtn;
        private JButton statusBtn;

        public MemberActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 8, 3));
            setOpaque(true);

            editBtn = new JButton("✏️ Sửa");
            editBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            editBtn.setPreferredSize(new Dimension(70, 32));
            editBtn.setBackground(new Color(255, 152, 0));
            editBtn.setForeground(Color.WHITE);
            editBtn.setFocusPainted(false);
            editBtn.setBorderPainted(false);
            editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            statusBtn = new JButton("🏠 Trạng thái");
            statusBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            statusBtn.setPreferredSize(new Dimension(110, 32));
            statusBtn.setBackground(new Color(33, 150, 243));
            statusBtn.setForeground(Color.WHITE);
            statusBtn.setFocusPainted(false);
            statusBtn.setBorderPainted(false);
            statusBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            add(editBtn);
            add(statusBtn);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            // Always use white background to avoid selection color
            setBackground(Color.WHITE);
            return this;
        }
    }

    /**
     * Member Action Button Editor
     */
    class MemberActionEditor extends DefaultCellEditor {

        private JPanel panel;
        private JButton editBtn;
        private JButton statusBtn;
        private Long currentMemberId;
        private List<HouseholdMember> members;

        public MemberActionEditor(JCheckBox checkBox, List<HouseholdMember> members) {
            super(checkBox);
            this.members = members;

            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 3));
            panel.setBackground(Color.WHITE);

            editBtn = new JButton("✏️ Sửa");
            editBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            editBtn.setPreferredSize(new Dimension(70, 32));
            editBtn.setBackground(new Color(255, 152, 0));
            editBtn.setForeground(Color.WHITE);
            editBtn.setFocusPainted(false);
            editBtn.setBorderPainted(false);
            editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            editBtn.addActionListener(e -> {
                fireEditingStopped();
                editMember(currentMemberId);
            });

            statusBtn = new JButton("🏠 Trạng thái");
            statusBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            statusBtn.setPreferredSize(new Dimension(110, 32));
            statusBtn.setBackground(new Color(33, 150, 243));
            statusBtn.setForeground(Color.WHITE);
            statusBtn.setFocusPainted(false);
            statusBtn.setBorderPainted(false);
            statusBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            statusBtn.addActionListener(e -> {
                fireEditingStopped();
                toggleMemberStatus(currentMemberId);
            });

            panel.add(editBtn);
            panel.add(statusBtn);
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentMemberId = (Long) value;
            return panel;
        }

        public Object getCellEditorValue() {
            return currentMemberId;
        }
    }
}
