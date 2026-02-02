package view;

import dao.UserDAO;
import model.User;
import util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Beautiful Centered Login Frame Fixed alignment and spacing issues
 */
public class LoginFrame extends JFrame {

    private UserDAO userDAO;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel usernameErrorLabel;
    private JLabel passwordErrorLabel;
    private JCheckBox rememberCheckbox;
    private JLabel eyeIcon;
    private boolean passwordVisible = false;

    // Colors
    private final Color PRIMARY_COLOR = new Color(99, 102, 241);
    private final Color PRIMARY_HOVER = new Color(79, 70, 229);
    private final Color ERROR_COLOR = new Color(239, 68, 68);
    private final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private final Color BORDER_COLOR = new Color(229, 231, 235);

    public LoginFrame() {
        this.userDAO = new UserDAO();

        initializeFrame();
        createLoginUI();

        setVisible(true);
    }

    private void initializeFrame() {
        setTitle("Đăng Nhập - Hệ Thống Quản Lý Chung Cư");
        setSize(500, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    }

    private void createLoginUI() {
        // Main container with gradient - USE BorderLayout
        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Subtle gradient
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(240, 242, 255),
                        0, getHeight(), new Color(248, 250, 252)
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // Wrapper panel to center the card
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        // Login card
        JPanel loginCard = createLoginCard();
        loginCard.setPreferredSize(new Dimension(400, 620));

        centerWrapper.add(loginCard);

        mainContainer.add(centerWrapper, BorderLayout.CENTER);
        add(mainContainer);
    }

    private JPanel createLoginCard() {
        // Card with shadow
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow layers
                g2d.setColor(new Color(0, 0, 0, 12));
                g2d.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 24, 24);
                g2d.setColor(new Color(0, 0, 0, 8));
                g2d.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 24, 24);

                // White background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(45, 45, 45, 45));

        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;

                // Draw building
                g2d.setColor(PRIMARY_COLOR);
                g2d.fillRoundRect(centerX - 30, centerY - 25, 60, 50, 4, 4);

                // Draw windows (3x3 grid)
                g2d.setColor(Color.WHITE);
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int x = centerX - 20 + (col * 15);
                        int y = centerY - 15 + (row * 15);
                        g2d.fillRect(x, y, 8, 8);
                    }
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(80, 80);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(80, 80);
            }
        };
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logoPanel);
        card.add(Box.createVerticalStrut(22));

        // Title
        JLabel titleLabel = new JLabel("QUẢN LÝ CHUNG CƯ");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 27));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));

        // Subtitle
        JLabel subtitleLabel = new JLabel("Đăng nhập vào hệ thống");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(35));

        // Form container - ensures all inputs same width
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        formContainer.setMaximumSize(new Dimension(310, 500));

        // Username
        formContainer.add(createInputGroup("Tên đăng nhập",
                usernameField = createTextField(),
                usernameErrorLabel = createErrorLabel()));
        formContainer.add(Box.createVerticalStrut(18));

        // Password
        formContainer.add(createPasswordGroup());
        formContainer.add(Box.createVerticalStrut(18));

        // Remember checkbox
        rememberCheckbox = new JCheckBox("Ghi nhớ đăng nhập");
        rememberCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rememberCheckbox.setForeground(TEXT_SECONDARY);
        rememberCheckbox.setOpaque(false);
        rememberCheckbox.setFocusPainted(false);
        rememberCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(rememberCheckbox);
        formContainer.add(Box.createVerticalStrut(28));

        // Login button
        JButton loginButton = createModernButton("Đăng Nhập");
        loginButton.addActionListener(e -> performLogin());
        formContainer.add(loginButton);

        card.add(formContainer);
        card.add(Box.createVerticalStrut(22));

        // Info text
        JLabel infoLabel = new JLabel("<html><div style='text-align: center;'>"
                + "<span style='color: rgb(156, 163, 175); font-size: 11px;'><i>Tài khoản mặc định:<br>"
                + "admin / admin123 hoặc user / user123</i></span></div></html>");
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(infoLabel);

        return card;
    }

    private JPanel createInputGroup(String label, JTextField field, JLabel errorLabel) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(310, 100));

        // Label
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelComponent.setForeground(TEXT_PRIMARY);
        labelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(labelComponent);
        group.add(Box.createVerticalStrut(7));

        // Field
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(field);

        // Error
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(errorLabel);

        return group;
    }

    private JPanel createPasswordGroup() {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(310, 100));

        // Label
        JLabel labelComponent = new JLabel("Mật khẩu");
        labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelComponent.setForeground(TEXT_PRIMARY);
        labelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(labelComponent);
        group.add(Box.createVerticalStrut(7));

        // Field container with eye icon
        JPanel fieldContainer = new JPanel(new BorderLayout(0, 0));
        fieldContainer.setOpaque(false);
        fieldContainer.setMaximumSize(new Dimension(310, 46));
        fieldContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 2),
                new EmptyBorder(11, 14, 11, 44)
        ));

        // Focus effects
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (passwordErrorLabel.isVisible()) {
                    return;
                }
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                        new EmptyBorder(11, 14, 11, 44)
                ));
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (passwordErrorLabel.isVisible()) {
                    return;
                }
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 2),
                        new EmptyBorder(11, 14, 11, 44)
                ));
            }
        });

        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        });

        // Eye icon
        eyeIcon = new JLabel("👁");
        eyeIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        eyeIcon.setForeground(TEXT_SECONDARY);
        eyeIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeIcon.setBorder(new EmptyBorder(0, 8, 0, 12));
        eyeIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                togglePasswordVisibility();
            }

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                eyeIcon.setForeground(PRIMARY_COLOR);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                eyeIcon.setForeground(TEXT_SECONDARY);
            }
        });

        fieldContainer.add(passwordField, BorderLayout.CENTER);
        fieldContainer.add(eyeIcon, BorderLayout.EAST);

        group.add(fieldContainer);

        // Error
        passwordErrorLabel = createErrorLabel();
        passwordErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(passwordErrorLabel);

        return group;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 2),
                new EmptyBorder(11, 14, 11, 14)
        ));
        field.setMaximumSize(new Dimension(310, 46));
        field.setPreferredSize(new Dimension(310, 46));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (usernameErrorLabel != null && usernameErrorLabel.isVisible()) {
                    return;
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                        new EmptyBorder(11, 14, 11, 14)
                ));
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (usernameErrorLabel != null && usernameErrorLabel.isVisible()) {
                    return;
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 2),
                        new EmptyBorder(11, 14, 11, 14)
                ));
            }
        });

        return field;
    }

    private JLabel createErrorLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(ERROR_COLOR);
        label.setBorder(new EmptyBorder(5, 3, 0, 0));
        label.setVisible(false);
        return label;
    }

    private JButton createModernButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bgColor;
                if (getModel().isPressed()) {
                    bgColor = new Color(67, 56, 202);
                } else if (getModel().isRollover()) {
                    bgColor = PRIMARY_HOVER;
                } else {
                    bgColor = PRIMARY_COLOR;
                }

                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.dispose();

                super.paintComponent(g);
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(310, 46));
        button.setPreferredSize(new Dimension(310, 46));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        return button;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordField.setEchoChar((char) 0);
            eyeIcon.setText("🙈");
        } else {
            passwordField.setEchoChar('●');
            eyeIcon.setText("👁");
        }
    }

    private void performLogin() {
        clearErrors();

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        boolean hasError = false;

        if (username.isEmpty()) {
            showError(usernameField, usernameErrorLabel, "⚠ Vui lòng nhập tên đăng nhập");
            hasError = true;
        }

        if (password.isEmpty()) {
            showError(passwordField, passwordErrorLabel, "⚠ Vui lòng nhập mật khẩu");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        User user = userDAO.authenticate(username, password);
        setCursor(Cursor.getDefaultCursor());

        if (user != null) {
            if (!user.isActive()) {
                showError(usernameField, usernameErrorLabel, "⚠ Tài khoản đã bị vô hiệu hóa");
                return;
            }

            SessionManager.getInstance().setCurrentUser(user);

            SwingUtilities.invokeLater(() -> {
                MainDashboard dashboard = new MainDashboard();
                dashboard.setVisible(true);
                dispose();
            });
        } else {
            showError(usernameField, usernameErrorLabel, "⚠ Tên đăng nhập hoặc mật khẩu không đúng");
            showError(passwordField, passwordErrorLabel, "");
            passwordField.setText("");
            usernameField.selectAll();
            usernameField.requestFocus();
        }
    }

    private void showError(JComponent field, JLabel errorLabel, String message) {
        if (field instanceof JTextField) {
            int rightPadding = (field == passwordField) ? 44 : 14;
            ((JTextField) field).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ERROR_COLOR, 2),
                    new EmptyBorder(11, 14, 11, rightPadding)
            ));
        }

        if (!message.isEmpty()) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    private void clearErrors() {
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 2),
                new EmptyBorder(11, 14, 11, 14)
        ));
        usernameErrorLabel.setVisible(false);

        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 2),
                new EmptyBorder(11, 14, 11, 44)
        ));
        passwordErrorLabel.setVisible(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}
