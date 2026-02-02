package view;

import dao.UserDAO;
import model.User;
import util.SessionManager;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

public class MyStaffPanel extends JPanel {

    private final UserDAO userDAO = new UserDAO();
    private JPanel cardsPanel;
    private JLabel lblTotalStaff, lblActiveStaff;
    private final User currentManager;

    public MyStaffPanel() {
        this.currentManager = SessionManager.getInstance().getCurrentUser();

        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND_COLOR);

        add(createModernHeader(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);

        loadStaff();
    }

    // ================= MODERN HEADER =================
    private JPanel createModernHeader() {
        JPanel headerContainer = new JPanel(new BorderLayout());
        headerContainer.setBackground(Color.WHITE);
        headerContainer.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(230, 230, 235)),
                new EmptyBorder(25, 35, 25, 35)
        ));

        // Left side - Title & Stats
        JPanel leftSide = new JPanel();
        leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.Y_AXIS));
        leftSide.setOpaque(false);

        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel("👥");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel title = new JLabel("Nhân Viên Tòa Nhà");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(30, 30, 45));

        titlePanel.add(iconLabel);
        titlePanel.add(title);

        leftSide.add(titlePanel);
        leftSide.add(Box.createVerticalStrut(12));

        // Stats Panel với cards nhỏ
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        statsPanel.setOpaque(false);

        lblTotalStaff = new JLabel("0");
        lblActiveStaff = new JLabel("0");

        statsPanel.add(createStatBadge("Tổng số", lblTotalStaff, new Color(99, 102, 241)));
        statsPanel.add(createStatBadge("Hoạt động", lblActiveStaff, new Color(16, 185, 129)));

        leftSide.add(statsPanel);

        // Right side - Action button
        JButton btnAdd = new ModernButton("+ Thêm nhân viên", new Color(99, 102, 241));
        btnAdd.setPreferredSize(new Dimension(160, 42));
        btnAdd.addActionListener(e -> addStaff());

        headerContainer.add(leftSide, BorderLayout.WEST);
        headerContainer.add(btnAdd, BorderLayout.EAST);

        return headerContainer;
    }

    // Stat badge component
    private JPanel createStatBadge(String label, JLabel valueLabel, Color color) {
        JPanel badge = new JPanel();
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
        badge.setBorder(new CompoundBorder(
                new LineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60), 1, true),
                new EmptyBorder(10, 18, 10, 18)
        ));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblLabel.setForeground(new Color(100, 100, 115));
        lblLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        badge.add(lblLabel);
        badge.add(Box.createVerticalStrut(3));
        badge.add(valueLabel);

        return badge;
    }

    // ================= CONTENT =================
    private JScrollPane createContent() {
        // ✅ FlowLayout tự động wrap khi không đủ chỗ
        cardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(new EmptyBorder(25, 35, 25, 35));

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BACKGROUND_COLOR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ================= DATA =================
    private void loadStaff() {
        cardsPanel.removeAll();

        List<User> users = userDAO.getAllUsers(currentManager);

        // Filter out current manager
        users = users.stream()
                .filter(u -> !u.getId().equals(currentManager.getId()))
                .collect(java.util.stream.Collectors.toList());

        long active = users.stream().filter(User::isActive).count();

        lblTotalStaff.setText(String.valueOf(users.size()));
        lblActiveStaff.setText(String.valueOf(active));

        if (users.isEmpty()) {
            cardsPanel.setLayout(new GridBagLayout());
            cardsPanel.add(createEmptyState());
        } else {
            // ✅ Reset về FlowLayout khi có data
            cardsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
            for (User u : users) {
                cardsPanel.add(new CompactStaffCard(u));
            }
        }

        revalidate();
        repaint();
    }

    // Empty state
    private JPanel createEmptyState() {
        JPanel empty = new JPanel();
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setOpaque(false);

        JLabel icon = new JLabel("📋");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel text = new JLabel("Chưa có nhân viên nào");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        text.setForeground(new Color(150, 150, 160));
        text.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Nhấn \"Thêm nhân viên\" để bắt đầu");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hint.setForeground(new Color(180, 180, 190));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        empty.add(icon);
        empty.add(Box.createVerticalStrut(15));
        empty.add(text);
        empty.add(Box.createVerticalStrut(8));
        empty.add(hint);

        return empty;
    }

    // ================= ACTIONS =================
    private void addStaff() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        // ✅ SỬA: Truyền 'currentManager' thay vì buildingId đơn lẻ
        MyStaffDialog dialog = new MyStaffDialog(parent, currentManager);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            loadStaff();
        }
    }

    // ================= COMPACT STAFF CARD (FIXED SIZE) =================
    private class CompactStaffCard extends JPanel {

        private final User user;

        CompactStaffCard(User user) {
            this.user = user;

            setLayout(new BorderLayout(12, 12));
            setBackground(Color.WHITE);
            setBorder(new CompoundBorder(
                    new LineBorder(new Color(230, 230, 235), 1, true),
                    new EmptyBorder(16, 16, 16, 16)
            ));

            // ✅ FIXED SIZE - không co giãn
            setPreferredSize(new Dimension(300, 140));
            setMinimumSize(new Dimension(300, 140));
            setMaximumSize(new Dimension(300, 140));

            add(createCardContent(), BorderLayout.CENTER);
            add(createCardActions(), BorderLayout.SOUTH);

            // Hover effect
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    setBackground(new Color(250, 250, 252));
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    setBackground(Color.WHITE);
                }
            });
        }

        private JPanel createCardContent() {
            JPanel content = new JPanel(new BorderLayout(10, 0));
            content.setOpaque(false);

            // Avatar
            JPanel avatarPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Color avatarColor = user.isStaff()
                            ? new Color(139, 92, 246) : new Color(236, 72, 153);
                    g2d.setColor(avatarColor);
                    g2d.fillOval(0, 0, 48, 48);

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    String initials = getInitials(user.getFullName());
                    FontMetrics fm = g2d.getFontMetrics();
                    int x = (48 - fm.stringWidth(initials)) / 2;
                    int y = ((48 - fm.getHeight()) / 2) + fm.getAscent();
                    g2d.drawString(initials, x, y);

                    g2d.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(48, 48);
                }
            };
            avatarPanel.setOpaque(false);

            // Info
            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setOpaque(false);

            JLabel name = new JLabel(user.getFullName());
            name.setFont(new Font("Segoe UI", Font.BOLD, 15));
            name.setForeground(new Color(30, 30, 45));
            name.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel username = new JLabel("@" + user.getUsername());
            username.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            username.setForeground(new Color(140, 140, 155));
            username.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Status & Role in one line
            JPanel metaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            metaPanel.setOpaque(false);
            metaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel status = new JLabel(user.isActive() ? "● Hoạt động" : "● Ngưng");
            status.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            status.setForeground(user.isActive()
                    ? new Color(16, 185, 129) : new Color(239, 68, 68));

            JLabel role = new JLabel(getRoleDisplay(user.getRole()));
            role.setFont(new Font("Segoe UI", Font.BOLD, 10));
            Color roleColor = user.isStaff() ? new Color(139, 92, 246) : new Color(236, 72, 153);
            role.setForeground(roleColor);
            role.setOpaque(true);
            role.setBackground(new Color(roleColor.getRed(), roleColor.getGreen(), roleColor.getBlue(), 25));
            role.setBorder(new EmptyBorder(3, 8, 3, 8));

            metaPanel.add(status);
            metaPanel.add(role);

            info.add(name);
            info.add(Box.createVerticalStrut(3));
            info.add(username);
            info.add(Box.createVerticalStrut(6));
            info.add(metaPanel);

            content.add(avatarPanel, BorderLayout.WEST);
            content.add(info, BorderLayout.CENTER);

            return content;
        }

        private JPanel createCardActions() {
            JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));
            actions.setOpaque(false);

            actions.add(createIconButton("✏️", "Sửa", new Color(59, 130, 246), e -> edit()));
            actions.add(createIconButton(user.isActive() ? "🔒" : "🔓",
                    user.isActive() ? "Khóa" : "Mở", new Color(245, 158, 11), e -> toggle()));
            actions.add(createIconButton("🗑️", "Xóa", new Color(239, 68, 68), e -> delete()));

            return actions;
        }

        private JButton createIconButton(String icon, String tooltip, Color color, java.awt.event.ActionListener action) {
            JButton btn = new JButton(icon);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            btn.setToolTipText(tooltip);
            btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
            btn.setForeground(color);
            btn.setBorder(new EmptyBorder(6, 6, 6, 6));
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(action);

            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
                }
            });

            return btn;
        }

        private String getInitials(String name) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
            }
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }

        private String getRoleDisplay(String role) {
            switch (role) {
                case "STAFF":
                    return "NHÂN VIÊN";
                case "ACCOUNTANT":
                    return "KẾ TOÁN";
                default:
                    return role;
            }
        }

        private void edit() {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(MyStaffPanel.this);
            // ✅ SỬA: Truyền 'currentManager' vào constructor
            MyStaffDialog dialog = new MyStaffDialog(parent, user, currentManager);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                loadStaff();
            }
        }

        private void toggle() {
            int confirm = JOptionPane.showConfirmDialog(
                    MyStaffPanel.this,
                    user.isActive() ? "Khóa tài khoản này?" : "Mở khóa tài khoản này?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                user.setActive(!user.isActive());
                userDAO.updateUser(user);
                loadStaff();
            }
        }

        private void delete() {
            int confirm = JOptionPane.showConfirmDialog(
                    MyStaffPanel.this,
                    "<html><b>Xóa nhân viên \"" + user.getFullName() + "\"?</b><br>"
                    + "<span style='color: red;'>Hành động này không thể hoàn tác!</span></html>",
                    "Xác nhận xóa",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                userDAO.deleteUser(user.getId());
                JOptionPane.showMessageDialog(
                        MyStaffPanel.this,
                        "Đã xóa nhân viên thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadStaff();
            }
        }
    }

    // ================= MODERN BUTTON COMPONENT =================
    private static class ModernButton extends JButton {

        private Color bgColor;
        private Color hoverColor;

        public ModernButton(String text, Color bgColor) {
            super(text);
            this.bgColor = bgColor;
            this.hoverColor = bgColor.darker();

            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    repaint();
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color color = getModel().isRollover() ? hoverColor : bgColor;
            g2d.setColor(color);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            g2d.dispose();
            super.paintComponent(g);
        }
    }
}
