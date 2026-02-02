package util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.Component; // ✅ Đã thêm import này
import java.awt.Desktop;   // ✅ Đã thêm import này
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExcelExporter {

    public static void exportTable(JTable table, String sheetName, String title, Component parent) {
        // 1. Chọn nơi lưu file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu file Excel");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));

        // Tên file mặc định: TenSheet_NgayThangNam.xlsx
        String defaultName = sheetName + "_" + new SimpleDateFormat("ddMMyyyy").format(new Date()) + ".xlsx";
        fileChooser.setSelectedFile(new File(defaultName));

        int userSelection = fileChooser.showSaveDialog(parent);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Đảm bảo đuôi .xlsx
            if (!fileToSave.getAbsolutePath().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
            }

            try {
                writeExcel(table, sheetName, title, fileToSave);

                int open = JOptionPane.showConfirmDialog(parent,
                        "Xuất file thành công!\nĐường dẫn: " + fileToSave.getAbsolutePath() + "\n\nBạn có muốn mở file ngay không?",
                        "Thành công", JOptionPane.YES_NO_OPTION);

                // Mở file sau khi xuất nếu user chọn Yes
                if (open == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(fileToSave);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(parent, "Không thể mở file tự động.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Lỗi khi lưu file: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void writeExcel(JTable table, String sheetName, String title, File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        // --- STYLES ---
        // 1. Title Style
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 18);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // 2. Header Style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 3. Data Style (Normal)
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        TableModel model = table.getModel();
        int colCount = model.getColumnCount();

        // Bỏ cột cuối cùng nếu là "Thao tác" hoặc "Action" (thường là nút bấm)
        String lastColName = model.getColumnName(colCount - 1);
        if (lastColName.toLowerCase().contains("thao tác") || lastColName.toLowerCase().contains("action") || lastColName.equals("")) {
            colCount--;
        }

        int rowIndex = 0;

        // --- ROW 0: TITLE ---
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title.toUpperCase());
        titleCell.setCellStyle(titleStyle);
        // Merge cells cho title
        if (colCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));
        }
        rowIndex++; // Dòng trống

        // --- ROW 2: HEADER ---
        Row headerRow = sheet.createRow(rowIndex++);
        headerRow.setHeightInPoints(30);
        for (int i = 0; i < colCount; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(model.getColumnName(i));
            cell.setCellStyle(headerStyle);
        }

        // --- DATA ROWS ---
        for (int i = 0; i < model.getRowCount(); i++) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(20);
            for (int j = 0; j < colCount; j++) {
                Cell cell = row.createCell(j);
                Object value = model.getValueAt(i, j);
                String text = (value == null) ? "" : value.toString();

                // Xử lý html tag nếu có (ví dụ <html>...</html> trong label)
                text = text.replaceAll("\\<.*?\\>", "").trim();

                cell.setCellValue(text);
                cell.setCellStyle(dataStyle);
            }
        }

        // Auto-size columns
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
        }

        // Save
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
        workbook.close();
    }
}
