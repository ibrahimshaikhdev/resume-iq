package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.model.AnalysisResult;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(124, 58, 237));
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(30, 27, 75));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(55, 65, 81));
    private static final Font BOLD_BODY = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(55, 65, 81));
    private static final Font SCORE_FONT = new Font(Font.HELVETICA, 28, Font.BOLD, new Color(124, 58, 237));
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(107, 114, 128));

    public byte[] generateReport(AnalysisResult result) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addHeader(document);
            addScoreSection(document, result);
            addAtsBreakdown(document, result);
            addSkillsSection(document, result);
            addReviewSection(document, "Strengths", result.getPros(), new Color(16, 185, 129));
            addReviewSection(document, "Areas for Improvement", result.getCons(), new Color(239, 68, 68));
            addReviewSection(document, "Suggestions", result.getSuggestions(), new Color(124, 58, 237));
            addFooter(document);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }

        return baos.toByteArray();
    }

    private void addHeader(Document document) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase("ResumeIQ - Resume Analysis Report", TITLE_FONT));
        cell.setBackgroundColor(new Color(26, 26, 26));
        cell.setPadding(20);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.addCell(cell);
        document.add(header);
        document.add(Chunk.NEWLINE);
    }

    private void addScoreSection(Document document, AnalysisResult result) throws DocumentException {
        PdfPTable scoreTable = new PdfPTable(2);
        scoreTable.setWidthPercentage(80);
        scoreTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        scoreTable.setSpacingBefore(10);
        scoreTable.setSpacingAfter(20);

        try {
            scoreTable.setWidths(new int[]{1, 1});
        } catch (Exception ignored) {}

        addScoreCard(scoreTable, "Overall Score", result.getScore());
        addScoreCard(scoreTable, "ATS Optimization", result.getAtsoptimizationscore());

        document.add(scoreTable);
    }

    private void addScoreCard(PdfPTable table, String label, int score) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(15);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph scorePara = new Paragraph(String.valueOf(score), SCORE_FONT);
        scorePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(scorePara);

        Paragraph labelPara = new Paragraph(label, BOLD_BODY);
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        Paragraph pctPara = new Paragraph("/100", SMALL_FONT);
        pctPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pctPara);

        table.addCell(cell);
    }

    private void addAtsBreakdown(Document document, AnalysisResult result) throws DocumentException {
        Map<String, Integer> breakdown = result.getAtsbreakdown();
        if (breakdown == null || breakdown.isEmpty()) return;

        document.add(new Paragraph("ATS Breakdown", SECTION_FONT));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(90);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setSpacingAfter(15);

        try {
            table.setWidths(new int[]{3, 1, 1});
        } catch (Exception ignored) {}

        addTableHeader(table, "Category", "Score", "Max");

        String[] keys = {"keywordMatch", "formatting", "readability", "sectionClarity", "contentRelevance", "contactInfo", "grammar"};
        String[] labels = {"Keyword Match", "Formatting", "Readability", "Section Clarity", "Content Relevance", "Contact Info", "Grammar"};
        int[] maxes = {15, 15, 15, 15, 15, 10, 15};

        for (int i = 0; i < keys.length; i++) {
            int score = breakdown.getOrDefault(keys[i], 0);
            addTableRow(table, labels[i], String.valueOf(score), String.valueOf(maxes[i]));
        }

        document.add(table);
    }

    private void addSkillsSection(Document document, AnalysisResult result) throws DocumentException {
        List<String> matched = result.getMatchedSkills();
        List<String> missing = result.getMissingSkills();

        PdfPTable skillsTable = new PdfPTable(2);
        skillsTable.setWidthPercentage(100);
        skillsTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        skillsTable.setSpacingBefore(10);
        skillsTable.setSpacingAfter(15);

        try {
            skillsTable.setWidths(new int[]{1, 1});
        } catch (Exception ignored) {}

        PdfPCell matchedHeader = new PdfPCell(new Phrase("Matched Skills (" + (matched != null ? matched.size() : 0) + ")", new Font(Font.HELVETICA, 11, Font.BOLD, new Color(16, 185, 129))));
        matchedHeader.setBackgroundColor(new Color(240, 253, 244));
        matchedHeader.setPadding(8);
        skillsTable.addCell(matchedHeader);

        PdfPCell missingHeader = new PdfPCell(new Phrase("Missing Skills (" + (missing != null ? missing.size() : 0) + ")", new Font(Font.HELVETICA, 11, Font.BOLD, new Color(239, 68, 68))));
        missingHeader.setBackgroundColor(new Color(254, 242, 242));
        missingHeader.setPadding(8);
        skillsTable.addCell(missingHeader);

        StringBuilder matchedText = new StringBuilder();
        if (matched != null) {
            for (String s : matched) {
                matchedText.append("\u2022 ").append(s).append("\n");
            }
        }
        PdfPCell matchedCell = new PdfPCell(new Phrase(matchedText.length() > 0 ? matchedText.toString() : "None identified", BODY_FONT));
        matchedCell.setPadding(8);
        skillsTable.addCell(matchedCell);

        StringBuilder missingText = new StringBuilder();
        if (missing != null) {
            for (String s : missing) {
                missingText.append("\u2022 ").append(s).append("\n");
            }
        }
        PdfPCell missingCell = new PdfPCell(new Phrase(missingText.length() > 0 ? missingText.toString() : "None identified", BODY_FONT));
        missingCell.setPadding(8);
        skillsTable.addCell(missingCell);

        document.add(skillsTable);
    }

    private void addReviewSection(Document document, String title, List<String> items, Color accentColor) throws DocumentException {
        if (items == null || items.isEmpty()) return;

        document.add(new Paragraph(title, new Font(Font.HELVETICA, 12, Font.BOLD, accentColor)));
        document.add(Chunk.NEWLINE);

        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            if (item != null && !item.isEmpty()) {
                Paragraph p = new Paragraph((i + 1) + ". " + item, BODY_FONT);
                p.setIndentationLeft(15);
                p.setSpacingAfter(4);
                document.add(p);
            }
        }

        document.add(Chunk.NEWLINE);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        String date = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a").format(new Date());
        Paragraph footer = new Paragraph("Generated by ResumeIQ on " + date, SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(new Color(124, 58, 237));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String... values) {
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v, BODY_FONT));
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }
}
