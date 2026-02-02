package util;

import dao.*;
import model.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.util.*;

// iText PDF
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

// Java core
import java.io.*;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.List;

/**
 * Report Export Service Handles exporting reports to Excel (XLSX) and PDF
 * formats
 *
 * Dependencies: - Apache POI 5.2.3+ for Excel - iText 5.5.13+ for PDF
 */
public class ReportExportService {

    // DAOs
    private InvoiceDAO invoiceDAO;
    private ApartmentDAO apartmentDAO;
    private ContractDAO contractDAO;
    private ResidentDAO residentDAO;
    private BuildingDAO buildingDAO;

    // Formatters
    private DecimalFormat moneyFormat;
    private SimpleDateFormat dateFormat;

    // Colors for Excel
    private static final short COLOR_HEADER = IndexedColors.LIGHT_BLUE.getIndex();
    private static final short COLOR_TOTAL = IndexedColors.LIGHT_GREEN.getIndex();

    public ReportExportService() {
        initializeDAOs();
        initializeFormatters();
    }

    private void initializeDAOs() {
        this.invoiceDAO = new InvoiceDAO();
        this.apartmentDAO = new ApartmentDAO();
        this.contractDAO = new ContractDAO();
        this.residentDAO = new ResidentDAO();
        this.buildingDAO = new BuildingDAO();
    }

    private void initializeFormatters() {
        this.moneyFormat = new DecimalFormat("#,##0");
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    }

    /**
     * ===== EXCEL EXPORT =====
     */
    public boolean exportToExcel(String filepath, int year, int fromMonth, int toMonth) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // Create sheets
            createOverviewSheet(workbook, year, fromMonth, toMonth);
            createRevenueSheet(workbook, year, fromMonth, toMonth);
            createInvoiceSheet(workbook, year, fromMonth, toMonth);
            createServiceSheet(workbook, year);
            createApartmentSheet(workbook);

            // Write to file
            try (FileOutputStream out = new FileOutputStream(filepath)) {
                workbook.write(out);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * SHEET 1: Overview (Tổng quan)
     */
    private void createOverviewSheet(XSSFWorkbook workbook, int year, int fromMonth, int toMonth) {
        Sheet sheet = workbook.createSheet("📊 Tổng Quan");

        // Styles
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);
        CellStyle moneyStyle = createMoneyStyle(workbook);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO TỔNG QUAN HỆ THỐNG QUẢN LÝ CHUNG CƯ");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

        rowNum++; // Blank row

        // Period
        Row periodRow = sheet.createRow(rowNum++);
        periodRow.createCell(0).setCellValue("Kỳ báo cáo:");
        periodRow.createCell(1).setCellValue(String.format("Từ tháng %d đến tháng %d năm %d",
                fromMonth, toMonth, year));

        // Export date
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Ngày xuất:");
        dateRow.createCell(1).setCellValue(dateFormat.format(new Date()));

        rowNum++; // Blank row

        // Statistics header
        Row statsHeaderRow = sheet.createRow(rowNum++);
        statsHeaderRow.createCell(0).setCellValue("CHỈ SỐ THỐNG KÊ");
        statsHeaderRow.getCell(0).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

        // Statistics data
        addStatRow(sheet, rowNum++, "🏢 Tổng số tòa nhà", buildingDAO.countBuildings(), normalStyle);
        addStatRow(sheet, rowNum++, "🏠 Tổng số căn hộ", apartmentDAO.countApartments(), normalStyle);
        addStatRow(sheet, rowNum++, "🔑 Căn hộ đang thuê", apartmentDAO.countRentedApartments(), normalStyle);
        addStatRow(sheet, rowNum++, "👥 Tổng số cư dân", residentDAO.countResidents(), normalStyle);
        addStatRow(sheet, rowNum++, "📝 Hợp đồng đang hiệu lực", contractDAO.countActiveContracts(), normalStyle);

        rowNum++; // Blank row

        // Revenue header
        Row revenueHeaderRow = sheet.createRow(rowNum++);
        revenueHeaderRow.createCell(0).setCellValue("DOANH THU");
        revenueHeaderRow.getCell(0).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

        // Revenue data
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (int month = fromMonth; month <= toMonth; month++) {
            BigDecimal monthRevenue = invoiceDAO.getMonthlyRevenue(month, year);
            totalRevenue = totalRevenue.add(monthRevenue);
        }

        Row totalRevenueRow = sheet.createRow(rowNum++);
        totalRevenueRow.createCell(0).setCellValue("💰 Tổng doanh thu");
        Cell revenueCell = totalRevenueRow.createCell(1);
        revenueCell.setCellValue(totalRevenue.doubleValue());
        revenueCell.setCellStyle(moneyStyle);
        totalRevenueRow.createCell(2).setCellValue("VNĐ");

        // Set column widths
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 3000);
    }

    /**
     * SHEET 2: Revenue Report (Báo cáo doanh thu)
     */
    private void createRevenueSheet(XSSFWorkbook workbook, int year, int fromMonth, int toMonth) {
        Sheet sheet = workbook.createSheet("📈 Doanh Thu");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);
        CellStyle moneyStyle = createMoneyStyle(workbook);
        CellStyle totalStyle = createTotalStyle(workbook);

        int rowNum = 0;

        // Header row
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Tháng/Năm", "Tổng HĐ", "Đã Thu", "Chưa Thu", "Doanh Thu (VNĐ)", "Tỷ Lệ (%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalInvoices = 0;
        int totalPaid = 0;
        int totalUnpaid = 0;

        for (int month = fromMonth; month <= toMonth; month++) {
            Row row = sheet.createRow(rowNum++);

            List<Invoice> invoices = invoiceDAO.getInvoicesByMonth(month, year);
            int monthInvoices = invoices.size();
            long paidCount = invoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();
            long unpaidCount = monthInvoices - paidCount;
            BigDecimal revenue = invoiceDAO.getMonthlyRevenue(month, year);

            totalInvoices += monthInvoices;
            totalPaid += paidCount;
            totalUnpaid += unpaidCount;
            totalRevenue = totalRevenue.add(revenue);

            row.createCell(0).setCellValue("Tháng " + month + "/" + year);
            row.createCell(1).setCellValue(monthInvoices);
            row.createCell(2).setCellValue(paidCount);
            row.createCell(3).setCellValue(unpaidCount);

            Cell revenueCell = row.createCell(4);
            revenueCell.setCellValue(revenue.doubleValue());
            revenueCell.setCellStyle(moneyStyle);

            // Formula for percentage
            String formula = String.format("E%d/$E$%d*100", rowNum, rowNum + (toMonth - fromMonth) + 1);
            Cell percentCell = row.createCell(5);
            percentCell.setCellFormula(formula);
        }

        // Total row
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("TỔNG CỘNG");
        totalRow.createCell(1).setCellValue(totalInvoices);
        totalRow.createCell(2).setCellValue(totalPaid);
        totalRow.createCell(3).setCellValue(totalUnpaid);

        Cell totalRevenueCell = totalRow.createCell(4);
        totalRevenueCell.setCellValue(totalRevenue.doubleValue());
        totalRevenueCell.setCellStyle(totalStyle);

        totalRow.createCell(5).setCellValue(100.0);

        // Apply total style to all cells in total row
        for (int i = 0; i <= 5; i++) {
            totalRow.getCell(i).setCellStyle(totalStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * SHEET 3: Invoice Report (Báo cáo hóa đơn)
     */
    private void createInvoiceSheet(XSSFWorkbook workbook, int year, int fromMonth, int toMonth) {
        Sheet sheet = workbook.createSheet("💰 Hóa Đơn");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle moneyStyle = createMoneyStyle(workbook);

        int rowNum = 0;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Số HĐ", "Căn Hộ", "Cư Dân", "Tháng/Năm", "Tổng Tiền (VNĐ)", "Trạng Thái", "Ngày TT"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        for (int month = fromMonth; month <= toMonth; month++) {
            List<Invoice> invoices = invoiceDAO.getInvoicesByMonth(month, year);

            for (Invoice invoice : invoices) {
                Row row = sheet.createRow(rowNum++);

                Contract contract = contractDAO.getContractById(invoice.getContractId());

                String contractNumber = "N/A";
                String apartmentNumber = "N/A";
                String residentName = "N/A";

                if (contract != null) {
                    contractNumber = contract.getContractNumber() != null ? contract.getContractNumber() : "N/A";

                    Apartment apt = apartmentDAO.getApartmentById(contract.getApartmentId());
                    if (apt != null) {
                        apartmentNumber = apt.getRoomNumber();
                    }

                    Resident res = residentDAO.getResidentById(contract.getResidentId());
                    if (res != null) {
                        residentName = res.getFullName();
                    }
                }

                row.createCell(0).setCellValue(contractNumber);
                row.createCell(1).setCellValue(apartmentNumber);
                row.createCell(2).setCellValue(residentName);
                row.createCell(3).setCellValue(String.format("%d/%d", month, year));

                Cell amountCell = row.createCell(4);
                amountCell.setCellValue(invoice.getTotalAmount().doubleValue());
                amountCell.setCellStyle(moneyStyle);

                row.createCell(5).setCellValue("PAID".equals(invoice.getStatus()) ? "Đã thanh toán" : "Chưa thanh toán");

                String paymentDate = invoice.getPaymentDate() != null
                        ? dateFormat.format(invoice.getPaymentDate()) : "";
                row.createCell(6).setCellValue(paymentDate);
            }
        }

        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * SHEET 4: Service Report (Báo cáo dịch vụ)
     */
    private void createServiceSheet(XSSFWorkbook workbook, int year) {
        Sheet sheet = workbook.createSheet("🔧 Dịch Vụ");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle moneyStyle = createMoneyStyle(workbook);
        CellStyle totalStyle = createTotalStyle(workbook);

        int rowNum = 0;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Tên Dịch Vụ", "Doanh Thu (VNĐ)", "Tỷ Trọng (%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Get service revenue data
        Map<String, BigDecimal> serviceRevenue = getServiceRevenue(year);
        BigDecimal totalRevenue = serviceRevenue.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Data rows
        for (Map.Entry<String, BigDecimal> entry : serviceRevenue.entrySet()) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(entry.getKey());

            Cell revenueCell = row.createCell(1);
            revenueCell.setCellValue(entry.getValue().doubleValue());
            revenueCell.setCellStyle(moneyStyle);

            double percent = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? (entry.getValue().doubleValue() / totalRevenue.doubleValue() * 100) : 0;
            row.createCell(2).setCellValue(percent);
        }

        // Total row
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("TỔNG CỘNG");

        Cell totalCell = totalRow.createCell(1);
        totalCell.setCellValue(totalRevenue.doubleValue());
        totalCell.setCellStyle(totalStyle);

        totalRow.createCell(2).setCellValue(100.0);
        totalRow.getCell(0).setCellStyle(totalStyle);
        totalRow.getCell(2).setCellStyle(totalStyle);

        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * SHEET 5: Apartment Report (Báo cáo căn hộ)
     */
    private void createApartmentSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("🏢 Căn Hộ");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);

        int rowNum = 0;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Tòa Nhà", "Tổng Căn", "Đang Thuê", "Còn Trống", "Tỷ Lệ Lấp Đầy (%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        List<Building> buildings = buildingDAO.getAllBuildings();

        for (Building building : buildings) {
            Row row = sheet.createRow(rowNum++);

            List<Apartment> apartments = apartmentDAO.getApartmentsByBuildingId(building.getId());
            int total = apartments.size();
            int rented = (int) apartments.stream().filter(a -> "RENTED".equals(a.getStatus())).count();
            int available = total - rented;
            double occupancyRate = total > 0 ? (rented * 100.0 / total) : 0;

            row.createCell(0).setCellValue(building.getName());
            row.createCell(1).setCellValue(total);
            row.createCell(2).setCellValue(rented);
            row.createCell(3).setCellValue(available);
            row.createCell(4).setCellValue(occupancyRate);
        }

        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * ===== PDF EXPORT =====
     */
    public boolean exportToPDF(String filepath, int year, int fromMonth, int toMonth) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filepath));

            document.open();

            // Add content
            addPDFCoverPage(document, year, fromMonth, toMonth);
            document.newPage();

            addPDFRevenueReport(document, year, fromMonth, toMonth);
            document.newPage();

            addPDFInvoiceReport(document, year, fromMonth, toMonth);
            document.newPage();

            addPDFApartmentReport(document);

            document.close();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addPDFCoverPage(Document document, int year, int fromMonth, int toMonth) throws DocumentException {
        // Title
        com.itextpdf.text.Font titleFont
                = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,
                        24,
                        com.itextpdf.text.Font.BOLD
                );

        Paragraph title = new Paragraph("BÁO CÁO TỔNG QUAN", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        com.itextpdf.text.Font subtitleFont
                = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,
                        24,
                        com.itextpdf.text.Font.BOLD
                );

        Paragraph subtitle = new Paragraph("HỆ THỐNG QUẢN LÝ CHUNG CƯ", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(30);
        document.add(subtitle);

        // Period
        com.itextpdf.text.Font normalFont
                = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,
                        12
                );

        Paragraph period = new Paragraph(
                String.format("Kỳ báo cáo: Từ tháng %d đến tháng %d năm %d", fromMonth, toMonth, year),
                normalFont
        );
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(10);
        document.add(period);

        // Date
        Paragraph date = new Paragraph(
                "Ngày xuất: " + dateFormat.format(new Date()),
                normalFont
        );
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(40);
        document.add(date);

        // Statistics table
        PdfPTable statsTable = new PdfPTable(2);
        statsTable.setWidthPercentage(70);
        statsTable.setSpacingBefore(20);

        addPDFStatRow(statsTable, "Tổng số tòa nhà", String.valueOf(buildingDAO.countBuildings()));
        addPDFStatRow(statsTable, "Tổng số căn hộ", String.valueOf(apartmentDAO.countApartments()));
        addPDFStatRow(statsTable, "Căn hộ đang thuê", String.valueOf(apartmentDAO.countRentedApartments()));
        addPDFStatRow(statsTable, "Tổng số cư dân", String.valueOf(residentDAO.countResidents()));
        addPDFStatRow(statsTable, "Hợp đồng đang hiệu lực", String.valueOf((char) contractDAO.countActiveContracts()));

        document.add(statsTable);
    }

    private void addPDFRevenueReport(Document document, int year, int fromMonth, int toMonth) throws DocumentException {
        // Section title
        com.itextpdf.text.Font sectionFont
                = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,
                        16,
                        com.itextpdf.text.Font.BOLD
                );

        Paragraph sectionTitle = new Paragraph("BÁO CÁO DOANH THU", sectionFont);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        // Table
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        // Header
        String[] headers = {"Tháng", "Tổng HĐ", "Đã Thu", "Chưa Thu", "Doanh Thu", "Tỷ Lệ"};
        for (String header : headers) {

            PdfPCell cell = new PdfPCell(new Phrase(header, new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 12
            )));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Data
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (int month = fromMonth; month <= toMonth; month++) {
            List<Invoice> invoices = invoiceDAO.getInvoicesByMonth(month, year);
            BigDecimal revenue = invoiceDAO.getMonthlyRevenue(month, year);
            totalRevenue = totalRevenue.add(revenue);

            int totalInv = invoices.size();
            long paid = invoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();

            table.addCell(String.format("T%d/%d", month, year));
            table.addCell(String.valueOf(totalInv));
            table.addCell(String.valueOf(paid));
            table.addCell(String.valueOf(totalInv - paid));
            table.addCell(formatMoney(revenue));
            table.addCell("-");
        }

        document.add(table);

        // Total
        Paragraph total = new Paragraph(
                String.format("Tổng doanh thu: %s VNĐ", formatMoney(totalRevenue)),
                new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 14
                )
        );
        total.setSpacingBefore(15);
        document.add(total);
    }

    private void addPDFInvoiceReport(Document document, int year, int fromMonth, int toMonth) throws DocumentException {
        com.itextpdf.text.Font sectionFont
                = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,
                        16,
                        com.itextpdf.text.Font.BOLD
                );

        Paragraph sectionTitle = new Paragraph("BÁO CÁO HÓA ĐƠN", sectionFont);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        // Summary
        List<Invoice> allInvoices = new ArrayList<>();
        for (int month = fromMonth; month <= toMonth; month++) {
            allInvoices.addAll(invoiceDAO.getInvoicesByMonth(month, year));
        }

        long paid = allInvoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        long unpaid = allInvoices.size() - paid;

        Paragraph summary = new Paragraph(
                String.format("Tổng hóa đơn: %d | Đã thanh toán: %d | Chưa thanh toán: %d",
                        allInvoices.size(), paid, unpaid),
                new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 12
                )
        );
        summary.setSpacingAfter(15);
        document.add(summary);
    }

    private void addPDFApartmentReport(Document document) throws DocumentException {
        com.itextpdf.text.Font sectionFont
                = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,
                        16,
                        com.itextpdf.text.Font.BOLD
                );
        Paragraph sectionTitle = new Paragraph("BÁO CÁO CĂN HỘ", sectionFont);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);

        // Header
        String[] headers = {"Tòa Nhà", "Tổng", "Đang Thuê", "Trống", "Tỷ Lệ %"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 10
            )));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }

        // Data
        List<Building> buildings = buildingDAO.getAllBuildings();
        for (Building building : buildings) {
            List<Apartment> apts = apartmentDAO.getApartmentsByBuildingId(building.getId());
            int total = apts.size();
            int rented = (int) apts.stream().filter(a -> "RENTED".equals(a.getStatus())).count();
            double rate = total > 0 ? (rented * 100.0 / total) : 0;

            table.addCell(building.getName());
            table.addCell(String.valueOf(total));
            table.addCell(String.valueOf(rented));
            table.addCell(String.valueOf(total - rented));
            table.addCell(String.format("%.1f%%", rate));
        }

        document.add(table);
    }

    /**
     * ===== HELPER METHODS =====
     */
    private Map<String, BigDecimal> getServiceRevenue(int year) {
        Map<String, BigDecimal> result = new HashMap<>();

        List<Invoice> invoices = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            invoices.addAll(invoiceDAO.getInvoicesByMonth(month, year));
        }

        for (Invoice invoice : invoices) {
            if (!"PAID".equals(invoice.getStatus())) {
                continue;
            }

            List<InvoiceDetail> details = invoiceDAO.getInvoiceDetails(invoice.getId());
            for (InvoiceDetail detail : details) {
                String serviceName = detail.getServiceName();
                BigDecimal amount = detail.getAmount();
                result.merge(serviceName, amount, BigDecimal::add);
            }
        }

        return result;
    }

    private void addStatRow(Sheet sheet, int rowNum, String label, int value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        row.getCell(0).setCellStyle(style);
        row.getCell(1).setCellStyle(style);
    }

    private void addPDFStatRow(PdfPTable table, String label, String value) {
        table.addCell(new Phrase(label, new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 11
        )));
        PdfPCell valueCell = new PdfPCell(new Phrase(value, new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 11
        )));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return moneyFormat.format(amount.longValue());
    }

    /**
     * ===== EXCEL STYLES =====
     */
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();

        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();;
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(COLOR_HEADER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNormalStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createMoneyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createTotalStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(COLOR_TOTAL);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
}
