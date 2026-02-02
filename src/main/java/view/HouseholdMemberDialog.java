package view;

import model.HouseholdMember;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Date;

/**
 * Household Member Dialog — Add / Edit Redesigned: sectioned layout, 2-col
 * grid, styled inputs consistent with ContractFormDialog
 */
public class HouseholdMemberDialog extends JDialog {

    private Long contractId;
    private HouseholdMember member;
    private boolean confirmed = false;

    // --- Form fields ---
    private JTextField txtFullName;
    private JComboBox<String> cmbRelationship;
    private JComboBox<String> cmbGender;
    private JSpinner spnDob;
    private JTextField txtIdentityCard;
    private JTextField txtPhone;
    private JCheckBox chkActive;

    // ===================== CONSTRUCTORS =====================
    /**
     * Add mode
     */
    public HouseholdMemberDialog(JDialog parent, Long contractId) {
        super(parent, "Thêm Thành Viên", true);
        this.contractId = contractId;
        this.member = new HouseholdMember();
        buildUI();
    }

    /**
     * Edit mode
     */
    public HouseholdMemberDialog(JDialog parent, HouseholdMember existing, Long contractId) {
        super(parent, "Sửa Thành Viên", true);
        this.contractId = contractId;
        this.member = existing;
        buildUI();
        populateFields();
    }

    // ===================== BUILD UI =====================
    private void buildUI() {
        setSize(560, 600);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.BACKGROUND_COLOR);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ---------- Header ----------
    private JPanel buildHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        panel.setBackground(UIConstants.PRIMARY_COLOR);
        panel.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel icon = new JLabel(member.getId() == null ? "➕" : "✏️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));

        JLabel title = new JLabel(member.getId() == null ? "Thêm Thành Viên Mới" : "Sửa Thành Viên");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        panel.add(icon);
        panel.add(title);
        return panel;
    }

    // ---------- Body ----------
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UIConstants.BACKGROUND_COLOR);
        body.setBorder(new EmptyBorder(16, 20, 8, 20));

        body.add(buildInfoSection());
        body.add(Box.createVerticalStrut(12));
        body.add(buildContactSection());
        body.add(Box.createVerticalStrut(12));
        body.add(buildStatusSection());

        JScrollPane scroll = new JScrollPane(body,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setBackground(UIConstants.BACKGROUND_COLOR);
        scroll.getViewport().setBackground(UIConstants.BACKGROUND_COLOR);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UIConstants.BACKGROUND_COLOR);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ---------- Section: Thông tin cơ bản ----------
    private JPanel buildInfoSection() {
        JPanel section = createSection("👤  Thông Tin Cơ Bản");
        section.setLayout(new GridBagLayout());
        GridBagConstraints g = defaultGBC();

        // Row 0-1: Họ tên (full width)
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        g.weightx = 1;
        section.add(label("Họ và tên", true), g);

        g.gridy = 1;
        txtFullName = styledTextField();
        section.add(txtFullName, g);

        // Row 2-3: Quan hệ | Giới tính (2 cột)
        g.gridwidth = 1;

        g.gridx = 0;
        g.gridy = 2;
        g.weightx = 1;
        section.add(label("Mối quan hệ", true), g);

        g.gridx = 1;
        section.add(label("Giới tính", true), g);

        g.gridy = 3;
        g.gridx = 0;
        cmbRelationship = styledComboBox(new String[]{
            "Vợ", "Chồng", "Con", "Cha", "Mẹ", "Anh/Chị", "Em", "Khác"
        });
        section.add(cmbRelationship, g);

        g.gridx = 1;
        cmbGender = styledComboBox(new String[]{"Nam", "Nữ", "Khác"});
        section.add(cmbGender, g);

        // Row 4-5: Ngày sinh (full width)
        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        section.add(label("Ngày sinh", false), g);

        g.gridy = 5;
        spnDob = createDateSpinner();
        section.add(spnDob, g);

        return section;
    }

    // ---------- Section: Liên lạc ----------
    private JPanel buildContactSection() {
        JPanel section = createSection("📞  Thông Tin Liên Lạc");
        section.setLayout(new GridBagLayout());
        GridBagConstraints g = defaultGBC();

        // Labels
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        section.add(label("CMND / CCCD", false), g);

        g.gridx = 1;
        section.add(label("Số điện thoại", false), g);

        // Fields
        g.gridy = 1;
        g.gridx = 0;
        txtIdentityCard = styledTextField();
        txtIdentityCard.setToolTipText("9 hoặc 12 chữ số");
        section.add(txtIdentityCard, g);

        g.gridx = 1;
        txtPhone = styledTextField();
        txtPhone.setToolTipText("10 hoặc 11 chữ số");
        section.add(txtPhone, g);

        return section;
    }

    // ---------- Section: Trạng thái ----------
    private JPanel buildStatusSection() {
        JPanel section = createSection("📋  Trạng Thái");
        section.setLayout(new GridBagLayout());
        GridBagConstraints g = defaultGBC();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.gridwidth = 2;

        chkActive = new JCheckBox("  Đang cư trú tại căn hộ");
        chkActive.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chkActive.setBackground(Color.WHITE);
        chkActive.setForeground(new Color(44, 44, 44));
        chkActive.setSelected(true);
        chkActive.setFocusPainted(false);
        section.add(chkActive, g);

        return section;
    }

    // ---------- Footer ----------
    private JPanel buildFooter() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        panel.setBackground(UIConstants.BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_COLOR));

        JButton btnCancel = footerButton("Hủy", new Color(158, 158, 158));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = footerButton(
                member.getId() == null ? "✅  Thêm" : "💾  Lưu",
                UIConstants.SUCCESS_COLOR
        );
        btnSave.addActionListener(e -> onSave());

        panel.add(btnCancel);
        panel.add(btnSave);
        return panel;
    }

    // ===================== POPULATE (Edit mode) =====================
    private void populateFields() {
        if (member == null) {
            return;
        }

        txtFullName.setText(member.getFullName());
        if (member.getRelationship() != null) {
            cmbRelationship.setSelectedItem(member.getRelationship());
        }
        if (member.getGender() != null) {
            cmbGender.setSelectedItem(member.getGender());
        }
        if (member.getDob() != null) {
            spnDob.setValue(member.getDob());
        }
        if (member.getIdentityCard() != null) {
            txtIdentityCard.setText(member.getIdentityCard());
        }
        if (member.getPhone() != null) {
            txtPhone.setText(member.getPhone());
        }
        chkActive.setSelected(member.isActive());
    }

    // ===================== SAVE =====================
    private void onSave() {
        // Validation
        String name = txtFullName.getText().trim();
        if (name.isEmpty()) {
            focus(txtFullName, "Họ và tên không được để trống!");
            return;
        }

        String idCard = txtIdentityCard.getText().trim();
        if (!idCard.isEmpty() && !idCard.matches("^[0-9]{9}$") && !idCard.matches("^[0-9]{12}$")) {
            focus(txtIdentityCard, "CMND/CCCD phải là 9 hoặc 12 chữ số!");
            return;
        }

        String phone = txtPhone.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[0-9]{10,11}$")) {
            focus(txtPhone, "SĐT phải là 10 hoặc 11 chữ số!");
            return;
        }

        // Map to model
        member.setFullName(name);
        member.setRelationship((String) cmbRelationship.getSelectedItem());
        member.setGender((String) cmbGender.getSelectedItem());
        member.setDob((Date) spnDob.getValue());
        member.setIdentityCard(idCard.isEmpty() ? null : idCard);
        member.setPhone(phone.isEmpty() ? null : phone);
        member.setActive(chkActive.isSelected());
        member.setContractId(contractId);

        confirmed = true;
        dispose();
    }

    // ===================== COMPONENT FACTORIES =====================
    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                new Color(66, 66, 66)
        );
        panel.setBorder(BorderFactory.createCompoundBorder(tb, new EmptyBorder(10, 14, 14, 14)));
        return panel;
    }

    private JLabel label(String text, boolean required) {
        JLabel lbl = new JLabel();
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setText(required
                ? "<html>" + text + " <font color='#e53935'>*</font></html>"
                : text);
        return lbl;
    }

    private JTextField styledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setPreferredSize(new Dimension(0, 34));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 2),
                        new EmptyBorder(5, 9, 5, 9)
                ));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                        new EmptyBorder(6, 10, 6, 10)
                ));
            }
        });
        return tf;
    }

    private JComboBox<String> styledComboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.setPreferredSize(new Dimension(0, 34));
        return cb;
    }

    private JSpinner createDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner sp = new JSpinner(model);
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        sp.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sp.setPreferredSize(new Dimension(0, 34));

        // Default: 30 năm trước
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.YEAR, -30);
        sp.setValue(cal.getTime());
        return sp;
    }

    private JButton footerButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(130, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bg.darker());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private GridBagConstraints defaultGBC() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 0, 4, 10);
        g.anchor = GridBagConstraints.NORTHWEST;
        return g;
    }

    private void focus(JTextField field, String msg) {
        field.requestFocus();
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.DANGER_COLOR, 2),
                new EmptyBorder(5, 9, 5, 9)
        ));
        JOptionPane.showMessageDialog(this, msg, "Kiểm tra lại", JOptionPane.WARNING_MESSAGE);
        new Timer(2000, e -> {
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                    new EmptyBorder(6, 10, 6, 10)
            ));
            ((Timer) e.getSource()).stop();
        }).start();
    }

    // ===================== GETTERS =====================
    public boolean isConfirmed() {
        return confirmed;
    }

    public HouseholdMember getMember() {
        return member;
    }
}
