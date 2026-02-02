package util;

import java.awt.Color;
import java.awt.Font;

/**
 * Modern Color Scheme and Design Constants
 */
public class UIConstants {

    // Modern Color Palette
    public static final Color PRIMARY_COLOR = new Color(99, 102, 241);        // Indigo
    public static final Color PRIMARY_DARK = new Color(79, 70, 229);          // Darker Indigo
    public static final Color PRIMARY_LIGHT = new Color(129, 140, 248);       // Light Indigo

    public static final Color SECONDARY_COLOR = new Color(236, 72, 153);      // Pink
    public static final Color SUCCESS_COLOR = new Color(34, 197, 94);         // Green
    public static final Color WARNING_COLOR = new Color(251, 146, 60);        // Orange
    public static final Color DANGER_COLOR = new Color(239, 68, 68);          // Red
    public static final Color INFO_COLOR = new Color(59, 130, 246);           // Blue

    // Neutral Colors
    public static final Color BACKGROUND_COLOR = new Color(249, 250, 251);    // Very light gray
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color SIDEBAR_COLOR = new Color(31, 41, 55);          // Dark gray
    public static final Color SIDEBAR_HOVER = new Color(55, 65, 81);          // Medium dark gray

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(17, 24, 39);           // Almost black
    public static final Color TEXT_SECONDARY = new Color(107, 114, 128);      // Gray
    public static final Color TEXT_WHITE = Color.WHITE;

    // Border Colors
    public static final Color BORDER_COLOR = new Color(229, 231, 235);        // Light gray
    public static final Color BORDER_FOCUS = PRIMARY_COLOR;

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MENU = new Font("Segoe UI", Font.PLAIN, 15);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 14);

    // Dimensions
    public static final int SIDEBAR_WIDTH = 250;
    public static final int BUTTON_HEIGHT = 40;
    public static final int CARD_PADDING = 20;
    public static final int BORDER_RADIUS = 8;

    // Shadows (for manual implementation)
    public static final Color SHADOW_COLOR = new Color(0, 0, 0, 10);
}
