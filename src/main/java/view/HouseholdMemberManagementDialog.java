package view;

import dao.HouseholdMemberDAO;
import model.HouseholdMember;
import util.UIConstants;
import util.ModernButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Household Member Management Dialog Popup để quản lý danh sách thành viên hộ
 * gia đình của một hợp đồng
 */
public class HouseholdMemberManagementDialog extends JDialog {

    private HouseholdMemberDAO memberDAO;
    private Long contractId;

    private JTable memberTable;
    private DefaultTableModel tableModel;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public HouseholdMemberManagementDialog(JDialog parent, Long contractId) {
        super(parent, "Quản Lý Thành Viên Hộ Gia Đình", true);

        this.contractId = contractId;
        this.memberDAO = new HouseholdMemberDAO();

        initializeDialog();
        createContent();
        loadMembers();

        setLocationRelativeTo(parent);
    }

    private void initializeDialog() {
        setSize(900, 600);
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);
    }

    private void createContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JLabel iconLabel = new JLabel("👥");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 32));

        JLabel titleLabel = new JLabel("Danh Sách Thành Viên");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel infoLabel = new JLabel("(Hợp đồng ID: " + contractId + ")");
        infoLabel.setFont(UIConstants.FONT_REGULAR);
        infoLabel.setForeground(UIConstants.TEXT_SECONDARY);

        titlePanel.add(iconLabel);
        titlePanel.add(Box.createHorizontalStrut(10));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createHorizontalStrut(5));
        titlePanel.add(infoLabel);

        headerPanel.add(titlePanel, BorderLayout.WEST);

        return headerPanel;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));

        String[] columns = {"ID", "Họ tên", "Quan hệ", "Giới tính", "Ngày sinh", "CCCD", "SĐT", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        memberTable = new JTable(tableModel);
        memberTable.setFont(UIConstants.FONT_REGULAR);
        memberTable.setRowHeight(45);
        memberTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberTable.setShowGrid(true);
        memberTable.setGridColor(UIConstants.BORDER_COLOR);

        // Double-click to edit
        memberTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    editMember();
                }
            }
        });

        JTableHeader header = memberTable.getTableHeader();
        header.setFont(UIConstants.FONT_HEADING);
        header.setBackground(UIConstants.BACKGROUND_COLOR);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.BORDER_COLOR));

        // Column widths
        memberTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        memberTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        memberTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        memberTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        memberTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        memberTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        memberTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        memberTable.getColumnModel().getColumn(7).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(memberTable);
        scrollPane.setBorder(null);

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        // Left: Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        ModernButton addButton = new ModernButton("➕ Thêm Thành Viên", UIConstants.SUCCESS_COLOR);
        addButton.setPreferredSize(new Dimension(170, 45));
        addButton.addActionListener(e -> addMember());

        ModernButton editButton = new ModernButton("✏️ Sửa", UIConstants.WARNING_COLOR);
        editButton.setPreferredSize(new Dimension(120, 45));
        editButton.addActionListener(e -> editMember());

        ModernButton deleteButton = new ModernButton("🗑️ Xóa", UIConstants.DANGER_COLOR);
        deleteButton.setPreferredSize(new Dimension(120, 45));
        deleteButton.addActionListener(e -> deleteMember());

        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        // Right: Close button
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setBackground(UIConstants.BACKGROUND_COLOR);

        ModernButton closeButton = new ModernButton("Đóng", UIConstants.TEXT_SECONDARY);
        closeButton.setPreferredSize(new Dimension(120, 45));
        closeButton.addActionListener(e -> dispose());

        closePanel.add(closeButton);

        buttonPanel.add(actionPanel, BorderLayout.WEST);
        buttonPanel.add(closePanel, BorderLayout.EAST);

        return buttonPanel;
    }

    private void loadMembers() {
        tableModel.setRowCount(0);
        List<HouseholdMember> members = memberDAO.getByContractId(contractId);

        for (HouseholdMember member : members) {
            String dobStr = member.getDob() != null ? dateFormat.format(member.getDob()) : "";
            String status = member.isActive() ? "Đang ở" : "Đã rời đi";

            Object[] row = {
                member.getId(),
                member.getFullName(),
                member.getRelationship(),
                member.getGender(),
                dobStr,
                member.getIdentityCard() != null ? member.getIdentityCard() : "",
                member.getPhone() != null ? member.getPhone() : "",
                status
            };
            tableModel.addRow(row);
        }
    }

    private void addMember() {
        HouseholdMemberDialog dialog = new HouseholdMemberDialog(this, contractId);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            HouseholdMember member = dialog.getMember();

            if (memberDAO.insert(member)) {
                JOptionPane.showMessageDialog(this,
                        "Thêm thành viên thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadMembers();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Thêm thành viên thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editMember() {
        int selectedRow = memberTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn thành viên cần sửa!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        HouseholdMember member = memberDAO.getById(id);

        if (member == null) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy thành viên!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        HouseholdMemberDialog dialog = new HouseholdMemberDialog(this, member, contractId);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            HouseholdMember updatedMember = dialog.getMember();

            if (memberDAO.update(updatedMember)) {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thành viên thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadMembers();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thành viên thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteMember() {
        int selectedRow = memberTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn thành viên cần xóa!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        String name = (String) tableModel.getValueAt(selectedRow, 1);
        String relationship = (String) tableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa thành viên:\n"
                + "Họ tên: " + name + "\n"
                + "Quan hệ: " + relationship + "?",
                "Xác Nhận Xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (memberDAO.delete(id)) {
                JOptionPane.showMessageDialog(this,
                        "Xóa thành viên thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadMembers();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Xóa thành viên thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
