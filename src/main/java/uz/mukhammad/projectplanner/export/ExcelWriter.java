package uz.mukhammad.projectplanner.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ExcelWriter implements AutoCloseable {

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final ZoneId zone;
    private final CellStyle headerStyle;
    private final CellStyle dateStyle;
    private final CellStyle dateTimeStyle;

    public ExcelWriter(ZoneId zone) {
        this.zone = zone;
        CreationHelper helper = workbook.getCreationHelper();

        Font bold = workbook.createFont();
        bold.setBold(true);
        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(bold);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd-mm-yyyy"));

        dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(helper.createDataFormat().getFormat("dd-mm-yyyy hh:mm"));
    }

    public SheetWriter sheet(String name, int... widths) {
        return new SheetWriter(workbook.createSheet(name), widths);
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }

    public class SheetWriter {

        private final Sheet sheet;
        private int nextRow = 0;

        private SheetWriter(Sheet sheet, int[] widths) {
            this.sheet = sheet;
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256); // в POI ширина в 1/256 символа
            }
        }

        public SheetWriter header(String... titles) {
            Row row = sheet.createRow(nextRow++);
            for (int i = 0; i < titles.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(titles[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, nextRow); // шапка остаётся видна при прокрутке
            return this;
        }

        public SheetWriter row(Object... values) {
            Row row = sheet.createRow(nextRow++);
            for (int i = 0; i < values.length; i++) {
                set(row.createCell(i), values[i]);
            }
            return this;
        }

        public SheetWriter blank() {
            nextRow++;
            return this;
        }

        private void set(Cell cell, Object value) {
            switch (value) {
                case null -> { }
                case Number n -> cell.setCellValue(n.doubleValue());
                case LocalDate d -> {
                    cell.setCellValue(d);
                    cell.setCellStyle(dateStyle);
                }
                case Instant i -> {
                    cell.setCellValue(LocalDateTime.ofInstant(i, zone));
                    cell.setCellStyle(dateTimeStyle);
                }
                case Boolean b -> cell.setCellValue(b ? "Yes" : "No");
                default -> cell.setCellValue(value.toString());
            }
        }
    }
}