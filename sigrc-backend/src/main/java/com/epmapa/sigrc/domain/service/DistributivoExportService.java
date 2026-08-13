package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.DistributivoDTO;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exportación del distributivo de personal a Excel (Apache POI) y PDF (iText7).
 * Los filtros se aplican antes (ReportesTalentoHumanoService.distributivo); aquí solo se maqueta el documento.
 */
@Service
public class DistributivoExportService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COLUMNAS = {
        "Identificación", "Funcionario", "Unidad", "Puesto",
        "Grupo ocupacional", "Tipo relación", "F. ingreso", "Estado laboral", "Tipo personal"
    };

    public byte[] excelDistributivo(List<DistributivoDTO> datos) throws IOException {
        try (var wb = new XSSFWorkbook(); var baos = new ByteArrayOutputStream()) {
            var hoja = wb.createSheet("Distributivo");

            CellStyle tituloStyle = wb.createCellStyle();
            Font tituloFont = wb.createFont();
            tituloFont.setBold(true);
            tituloFont.setFontHeightInPoints((short) 14);
            tituloStyle.setFont(tituloFont);

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue("DISTRIBUTIVO DE PERSONAL");
            titulo.getCell(0).setCellStyle(tituloStyle);

            Row subtitulo = hoja.createRow(1);
            subtitulo.createCell(0).setCellValue("Generado: " + LocalDate.now().format(FECHA)
                + "  ·  Registros: " + datos.size());

            Row encabezado = hoja.createRow(3);
            for (int i = 0; i < COLUMNAS.length; i++) {
                Cell c = encabezado.createCell(i);
                c.setCellValue(COLUMNAS[i]);
                c.setCellStyle(headerStyle);
            }

            int r = 4;
            for (var d : datos) {
                Row row = hoja.createRow(r++);
                row.createCell(0).setCellValue(notNull(d.identificacion()));
                row.createCell(1).setCellValue(notNull(d.funcionario()));
                row.createCell(2).setCellValue(notNull(d.unidad()));
                row.createCell(3).setCellValue(notNull(d.puesto()));
                row.createCell(4).setCellValue(notNull(d.grupoOcupacional()));
                row.createCell(5).setCellValue(notNull(d.tipoRelacion()));
                row.createCell(6).setCellValue(d.fechaIngreso() != null ? d.fechaIngreso().format(FECHA) : "");
                row.createCell(7).setCellValue(notNull(d.estadoLaboral()));
                row.createCell(8).setCellValue(notNull(d.tipoPersonal()));
            }

            for (int i = 0; i < COLUMNAS.length; i++) {
                hoja.autoSizeColumn(i);
                if (hoja.getColumnWidth(i) > 40 * 256) hoja.setColumnWidth(i, 40 * 256);
            }
            hoja.createFreezePane(0, 4);
            if (r > 4) {
                hoja.setAutoFilter(new CellRangeAddress(4, r - 1, 0, COLUMNAS.length - 1));
            }

            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] pdfDistributivo(List<DistributivoDTO> datos) throws IOException {
        var baos = new ByteArrayOutputStream();
        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        try (var pdf = new PdfDocument(new PdfWriter(baos))) {
            pdf.setDefaultPageSize(PageSize.A4.rotate());
            try (var doc = new Document(pdf)) {
                doc.add(new Paragraph("DISTRIBUTIVO DE PERSONAL")
                    .setFont(bold).setFontSize(14).setTextAlignment(TextAlignment.CENTER));
                doc.add(new Paragraph("EPMAPA · Talento Humano · Generado: "
                    + LocalDate.now().format(FECHA) + "  ·  Registros: " + datos.size())
                    .setFont(normal).setFontSize(9).setTextAlignment(TextAlignment.CENTER));

                float[] anchos = {3.5f, 5f, 4.5f, 5.5f, 4f, 4f, 3.5f, 3.5f, 4f};
                var tabla = new Table(UnitValue.createPointArray(anchos)).useAllAvailableWidth();
                for (String h : COLUMNAS) {
                    tabla.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(h).setFont(bold).setFontSize(8))
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER));
                }
                for (var d : datos) {
                    tabla.addCell(celda(normal, notNull(d.identificacion())));
                    tabla.addCell(celda(normal, notNull(d.funcionario())));
                    tabla.addCell(celda(normal, notNull(d.unidad())));
                    tabla.addCell(celda(normal, notNull(d.puesto())));
                    tabla.addCell(celda(normal, notNull(d.grupoOcupacional())));
                    tabla.addCell(celda(normal, notNull(d.tipoRelacion())));
                    tabla.addCell(celda(normal, d.fechaIngreso() != null ? d.fechaIngreso().format(FECHA) : ""));
                    tabla.addCell(celda(normal, notNull(d.estadoLaboral())));
                    tabla.addCell(celda(normal, notNull(d.tipoPersonal())));
                }
                doc.add(tabla);
            }
        }
        return baos.toByteArray();
    }

    private com.itextpdf.layout.element.Cell celda(PdfFont fuente, String texto) {
        return new com.itextpdf.layout.element.Cell().add(new Paragraph(texto).setFont(fuente).setFontSize(7));
    }

    private String notNull(String s) {
        return s != null ? s : "";
    }
}
