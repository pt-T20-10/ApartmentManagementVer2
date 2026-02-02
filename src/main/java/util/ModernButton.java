package util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled button with rounded corners and hover effects
 */
public class ModernButton extends JButton {

    private Color backgroundColor;
    private Color hoverColor;
    private Color pressedColor;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public ModernButton(String text) {
        this(text, UIConstants.PRIMARY_COLOR);
    }

    public ModernButton(String text, Color bgColor) {
        super(text);
        this.backgroundColor = bgColor;
        this.hoverColor = bgColor.darker();
        this.pressedColor = bgColor.darker().darker();

        setupButton();
        addMouseListeners();
    }

    private void setupButton() {
        setFont(UIConstants.FONT_BUTTON);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(getPreferredSize().width + 30, UIConstants.BUTTON_HEIGHT));
    }

    private void addMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Determine button color based on state
        Color buttonColor = backgroundColor;
        if (isPressed) {
            buttonColor = pressedColor;
        } else if (isHovered) {
            buttonColor = hoverColor;
        }

        // Draw rounded rectangle
        g2.setColor(buttonColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(),
                UIConstants.BORDER_RADIUS, UIConstants.BORDER_RADIUS));

        g2.dispose();
        super.paintComponent(g);
    }

    public void setButtonColor(Color color) {
        this.backgroundColor = color;
        this.hoverColor = color.darker();
        this.pressedColor = color.darker().darker();
        repaint();
    }
}
