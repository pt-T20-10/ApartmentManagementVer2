package view;

import dao.ServiceDAO;
import model.Service;
import util.UIConstants;
import util.ModernButton;
import util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

/**
 * Service Management Panel - IMPROVED UI Added statistics cards, better
 * spacing, modern design All original functionality preserved
 */
public class ServiceManagementPanel extends JPanel {

    private ServiceDAO serviceDAO;
    private JTable serviceTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    // Statistics labels
    private JLabel totalServicesLabel;
    private JLabel mandatoryServicesLabel;
    private JLabel optionalServicesLabel;

    public ServiceManagementPanel() {
        this.serviceDAO = new ServiceDAO();

        setLayout(new BorderLayout(0, 20));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // Top container with header + statistics
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(UIConstants.BACKGROUND_COLOR);

        topContainer.add(createHeader());
        topContainer.add(Box.createVerticalStrut(20));
        topContainer.add(createStatisticsPanel());

        add(topContainer, BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createActionPanel(), BorderLayout.SOUTH);

        loadServices();
        updateStatistics();
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Title with custom icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(UIConstants.BACKGROUND_COLOR);

        // Custom lightning icon
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Lightning bolt
                g2d.setColor(new Color(251, 191, 36));
                int[] xPoints = {20, 18, 22, 14, 16, 12};
                int[] yPoints = {8, 16, 16, 24, 18, 18};
                g2d.fillPolygon(xPoints, yPoints, 6);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32);
            }
        };
        iconPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Quản Lý Dịch Vụ");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        titlePanel.add(iconPanel);
        titlePanel.add(Box.createHorizontalStrut(12));
        titlePanel.add(titleLabel);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        searchField = new JTextField(20);
        searchField.setFont(UIConstants.FONT_REGULAR);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        // Placeholder
        final String PLACEHOLDER = "Tìm theo tên dịch vụ...";
        final Color PLACEHOLDER_COLOR = new Color(158, 158, 158);
        final Color TEXT_COLOR = new Color(33, 33, 33);

        searchField.setText(PLACEHOLDER);
        searchField.setForeground(PLACEHOLDER_COLOR);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (searchField.getText().equals(PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_COLOR);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText(PLACEHOLDER);
                    searchField.setForeground(PLACEHOLDER_COLOR);
                }
            }
        });

        searchField.addActionListener(e -> searchServices());

        ModernButton searchButton = new ModernButton("🔍 Tìm Kiếm", UIConstants.INFO_COLOR);
        searchButton.addActionListener(e -> searchServices());

        ModernButton refreshButton = new ModernButton("🔄 Làm Mới", UIConstants.SUCCESS_COLOR);
        refreshButton.addActionListener(e -> {
            searchField.setText(PLACEHOLDER);
            searchField.setForeground(PLACEHOLDER_COLOR);
            loadServices();
            updateStatistics();
        });

        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createStatisticsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        statsPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Card 1: Total Services
        totalServicesLabel = new JLabel("0");
        JPanel card1 = createStatCard("Tổng Dịch Vụ", totalServicesLabel, new Color(99, 102, 241), "📋");

        // Card 2: Mandatory
        mandatoryServicesLabel = new JLabel("0");
        JPanel card2 = createStatCard("Dịch Vụ Bắt Buộc", mandatoryServicesLabel, new Color(34, 197, 94), "✓");

        // Card 3: Optional
        optionalServicesLabel = new JLabel("0");
        JPanel card3 = createStatCard("Dịch Vụ Tùy Chọn", optionalServicesLabel, new Color(156, 163, 175), "○");

        statsPanel.add(card1);
        statsPanel.add(card2);
        statsPanel.add(card3);

        return statsPanel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout(15, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow
                g2d.setColor(new Color(0, 0, 0, 8));
                g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);

                // Background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2d.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        iconLabel.setForeground(color);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(107, 114, 128));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(UIConstants.TEXT_PRIMARY);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(valueLabel);

        card.add(iconLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    private void updateStatistics() {
        List<Service> services = serviceDAO.getAllServices();

        int total = services.size();
        int mandatory = (int) services.stream().filter(Service::isMandatory).count();
        int optional = total - mandatory;

        totalServicesLabel.setText(String.valueOf(total));
        mandatoryServicesLabel.setText(String.valueOf(mandatory));
        optionalServicesLabel.setText(String.valueOf(optional));
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));

        // Table columns - KEEP ORIGINAL
        String[] columns = {"ID", "Tên Dịch Vụ", "Đơn Vị", "Đơn Giá", "Bắt Buộc"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        serviceTable = new JTable(tableModel);
        serviceTable.setFont(UIConstants.FONT_REGULAR);
        serviceTable.setRowHeight(55); // Slightly taller
        serviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceTable.setShowGrid(true);
        serviceTable.setGridColor(new Color(240, 240, 240));
        serviceTable.setSelectionBackground(new Color(232, 240, 254));
        serviceTable.setSelectionForeground(UIConstants.TEXT_PRIMARY);

        // Double-click to edit - KEEP ORIGINAL
        serviceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    editService();
                }
            }
        });

        // Table header styling - IMPROVED
        JTableHeader header = serviceTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(249, 250, 251));
        header.setForeground(new Color(75, 85, 99));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 48));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.BORDER_COLOR));

        // Center align header
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        // Column widths - KEEP ORIGINAL
        serviceTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        serviceTable.getColumnModel().getColumn(0).setMaxWidth(80);
        serviceTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        serviceTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        serviceTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        serviceTable.getColumnModel().getColumn(4).setPreferredWidth(120);

        // Center align ALL columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < serviceTable.getColumnCount(); i++) {
            serviceTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Special renderer for "Bắt Buộc" column - IMPROVED BADGES
        serviceTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

                JLabel badge = new JLabel();
                badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
                badge.setBorder(new EmptyBorder(4, 12, 4, 12));
                badge.setOpaque(true);

                if ("Có".equals(value)) {
                    badge.setText("✓ Bắt buộc");
                    badge.setBackground(new Color(220, 252, 231));
                    badge.setForeground(new Color(22, 163, 74));
                } else {
                    badge.setText("○ Tùy chọn");
                    badge.setBackground(new Color(243, 244, 246));
                    badge.setForeground(new Color(107, 114, 128));
                }

                panel.add(badge);
                return panel;
            }
        });

        JScrollPane scrollPane = new JScrollPane(serviceTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        actionPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        actionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        ModernButton addButton = new ModernButton("➕ Thêm Dịch Vụ", UIConstants.SUCCESS_COLOR);
        addButton.setPreferredSize(new Dimension(160, 45));
        addButton.addActionListener(e -> addService());

        ModernButton editButton = new ModernButton("✏ Sửa", UIConstants.WARNING_COLOR);
        editButton.setPreferredSize(new Dimension(120, 45));
        editButton.addActionListener(e -> editService());

        ModernButton deleteButton = new ModernButton("🗑 Xóa", UIConstants.DANGER_COLOR);
        deleteButton.setPreferredSize(new Dimension(120, 45));
        deleteButton.addActionListener(e -> deleteService());

        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        return actionPanel;
    }

    // ===== ALL ORIGINAL METHODS BELOW - UNCHANGED =====
    private void loadServices() {
        tableModel.setRowCount(0);
        List<Service> services = serviceDAO.getAllServices();

        for (Service service : services) {
            Object[] row = {
                service.getId(),
                service.getName(),
                service.getUnit(),
                MoneyFormatter.formatMoney(service.getUnitPrice()) + " đ",
                service.isMandatory() ? "Có" : "Không"
            };
            tableModel.addRow(row);
        }
    }

    private void addService() {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        ServiceDialog dialog = new ServiceDialog(parentFrame);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Service service = dialog.getService();

            if (serviceDAO.insertService(service)) {
                JOptionPane.showMessageDialog(this,
                        "Thêm dịch vụ thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadServices();
                updateStatistics(); // Update stats
            } else {
                JOptionPane.showMessageDialog(this,
                        "Thêm dịch vụ thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editService() {
        int selectedRow = serviceTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn dịch vụ cần sửa!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        Service service = serviceDAO.getServiceById(id);

        if (service == null) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy dịch vụ!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        ServiceDialog dialog = new ServiceDialog(parentFrame, service);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Service updatedService = dialog.getService();

            if (serviceDAO.updateService(updatedService)) {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật dịch vụ thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadServices();
                updateStatistics(); // Update stats
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật dịch vụ thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteService() {
        int selectedRow = serviceTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn dịch vụ cần xóa!",
                    "Cảnh Báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        String serviceName = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa dịch vụ '" + serviceName + "'?",
                "Xác Nhận Xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (serviceDAO.deleteService(id)) {
                JOptionPane.showMessageDialog(this,
                        "Xóa dịch vụ thành công!",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadServices();
                updateStatistics(); // Update stats
            } else {
                JOptionPane.showMessageDialog(this,
                        "Xóa dịch vụ thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void searchServices() {
        String keyword = searchField.getText().trim().toLowerCase();

        // Ignore placeholder
        if (keyword.isEmpty() || keyword.equals("tìm theo tên dịch vụ...")) {
            loadServices();
            return;
        }

        tableModel.setRowCount(0);
        List<Service> services = serviceDAO.getAllServices();

        for (Service service : services) {
            if (service.getName().toLowerCase().contains(keyword)
                    || service.getUnit().toLowerCase().contains(keyword)) {

                Object[] row = {
                    service.getId(),
                    service.getName(),
                    service.getUnit(),
                    MoneyFormatter.formatMoney(service.getUnitPrice()) + " đ",
                    service.isMandatory() ? "Có" : "Không"
                };
                tableModel.addRow(row);
            }
        }

        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy dịch vụ nào!",
                    "Thông Báo",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
