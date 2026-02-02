package util;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility class for formatting money fields Formats: 2000000 -> 2,000,000 (Sử
 * dụng dấu phẩy)
 */
public class MoneyFormatter {

    // --- KHỞI TẠO COMPONENT ---
    /**
     * Create a formatted text field for money format Example: 2,000,000
     */
    public static JTextField createMoneyField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newText = currentText.substring(0, offset) + text + currentText.substring(offset + length);
                updateField(fb, newText, attrs);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newText = currentText.substring(0, offset) + currentText.substring(offset + length);
                updateField(fb, newText, null);
            }

            private void updateField(FilterBypass fb, String text, AttributeSet attrs) throws BadLocationException {
                // Chỉ giữ lại số
                String digitsOnly = text.replaceAll("[^0-9]", "");

                if (digitsOnly.isEmpty()) {
                    super.replace(fb, 0, fb.getDocument().getLength(), "", attrs);
                    return;
                }

                // Format với dấu phẩy
                String formatted = formatWithCommas(digitsOnly);
                super.replace(fb, 0, fb.getDocument().getLength(), formatted, attrs);
            }
        });

        return field;
    }

    /**
     * Create a formatted text field with custom size
     */
    public static JTextField createMoneyField(int height) {
        JTextField field = createMoneyField();
        field.setPreferredSize(new Dimension(0, height));
        return field;
    }

    // --- HELPER FORMAT ---
    /**
     * Format digits with commas Example: "2000000" -> "2,000,000"
     */
    private static String formatWithCommas(String digitsOnly) {
        try {
            long val = Long.parseLong(digitsOnly);
            return String.format(Locale.US, "%,d", val);
        } catch (NumberFormatException e) {
            return digitsOnly; // Fallback nếu số quá lớn
        }
    }

    // --- CÁC HÀM GET/SET (Giữ nguyên của bạn, cập nhật logic dấu phẩy) ---
    /**
     * Get numeric value from formatted field as Long
     */
    public static Long getValue(JTextField field) {
        // Xóa dấu phẩy để lấy số
        String text = field.getText().replace(",", "").trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Set value to formatted field
     */
    public static void setValue(JTextField field, Long value) {
        if (value == null) {
            field.setText("");
        } else {
            field.setText(formatWithCommas(String.valueOf(value)));
        }
    }

    // --- CÁC HÀM FORMAT/PARSE ---
    /**
     * Format long value to money string Example: 2000000 -> "2,000,000"
     */
    public static String formatMoney(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    /**
     * Format BigDecimal to money string
     */
    public static String formatMoney(java.math.BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return formatMoney(value.longValue());
    }

    /**
     * Parse formatted money string to long Example: "2,000,000" -> 2000000
     */
    public static long parseMoney(String formattedValue) {
        if (formattedValue == null || formattedValue.trim().isEmpty()) {
            return 0;
        }
        // Xóa dấu phẩy và chấm
        String cleaned = formattedValue.replace(",", "").replace(".", "").trim();
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * [MỚI] Hàm parse trả về BigDecimal để dùng cho ApartmentDialog Giúp tránh
     * lỗi biên dịch "incompatible types"
     */
    public static BigDecimal parseToBigDecimal(String formattedValue) {
        if (formattedValue == null || formattedValue.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String cleaned = formattedValue.replace(",", "").replace(".", "").trim();
        try {
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
