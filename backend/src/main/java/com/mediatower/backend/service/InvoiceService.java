package com.mediatower.backend.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.mediatower.backend.dto.InvoiceDto;
import com.mediatower.backend.model.Invoice;
import com.mediatower.backend.model.Order;
import com.mediatower.backend.model.OrderItem;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.InvoiceRepository;
import com.mediatower.backend.repository.OrderRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
    }

    public List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<InvoiceDto> getInvoiceById(Long id) {
        return invoiceRepository.findById(id).map(this::convertToDto);
    }

    public InvoiceDto generateInvoiceForOrder(Long orderId, Boolean includesVAT) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (invoiceRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalArgumentException("Invoice already exists for this order.");
        }

        BigDecimal totalHT = order.getTotalAmount();
        BigDecimal taxRate = BigDecimal.valueOf(0.20);
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalTTC = totalHT;

        if (includesVAT) {
            taxAmount = totalHT.multiply(taxRate);
            totalTTC = totalHT.add(taxAmount);
        }

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setInvoiceNumber("INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        invoice.setTotalHT(totalHT);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalTTC(totalTTC);
        invoice.setIncludesVAT(includesVAT);
        invoice.setBillingAddress(order.getUser().getAddress());

        return convertToDto(invoiceRepository.save(invoice));
    }


    public byte[] generatePdfInvoice(Long invoiceId) throws DocumentException, IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        Font fontHeader = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.DARK_GRAY);
        Font fontSubHeader = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.BLACK);
        Font fontNormal = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
        Font fontBold = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
        Font fontTotal = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLUE);

        try {
            Image logo = Image.getInstance(new ClassPathResource("logo.png").getURL());
            logo.scaleToFit(100, 100);
            logo.setAbsolutePosition(50, document.getPageSize().getHeight() - 100);
            document.add(logo);
        } catch (IOException | BadElementException e) {
            System.err.println("Could not load logo: " + e.getMessage());
        }

        Paragraph companyInfo = new Paragraph();
        companyInfo.add(new Chunk("Media Tower S.A.R.L.", fontSubHeader));
        companyInfo.add(new Chunk("\n123 Rue Principale, Casablanca, Maroc", fontNormal));
        companyInfo.add(new Chunk("\nEmail: contact@mediatower.com | Tel: +212 5XX XXX XXX", fontNormal));
        companyInfo.setAlignment(Element.ALIGN_RIGHT);
        companyInfo.setSpacingAfter(20);
        document.add(companyInfo);

        Paragraph title = new Paragraph("FACTURE", fontHeader);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(20f);
        infoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell invoiceInfoCell = new PdfPCell();
        invoiceInfoCell.setBorder(Rectangle.NO_BORDER);
        invoiceInfoCell.addElement(new Paragraph("Facture N°: " + invoice.getInvoiceNumber(), fontBold));
        invoiceInfoCell.addElement(new Paragraph("Date: " + invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontNormal));
        invoiceInfoCell.addElement(new Paragraph("Commande ID: " + invoice.getOrder().getId(), fontNormal));
        infoTable.addCell(invoiceInfoCell);

        User user = invoice.getOrder().getUser();
        PdfPCell customerInfoCell = new PdfPCell();
        customerInfoCell.setBorder(Rectangle.NO_BORDER);
        customerInfoCell.addElement(new Paragraph("Client:", fontBold));
        customerInfoCell.addElement(new Paragraph(user.getFirstName() + " " + user.getLastName(), fontNormal));
        customerInfoCell.addElement(new Paragraph(user.getEmail(), fontNormal));
        if (invoice.getBillingAddress() != null && !invoice.getBillingAddress().isEmpty()) {
            customerInfoCell.addElement(new Paragraph("Adresse: " + invoice.getBillingAddress(), fontNormal));
        }
        customerInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        infoTable.addCell(customerInfoCell);

        document.add(infoTable);

        PdfPTable itemsTable = new PdfPTable(5);
        itemsTable.setWidthPercentage(100);
        itemsTable.setSpacingBefore(10f);
        itemsTable.setSpacingAfter(20f);
        itemsTable.setWidths(new float[]{0.5f, 3f, 1f, 1.5f, 1.5f});

        addTableHeader(itemsTable, "N°", fontBold, BaseColor.LIGHT_GRAY);
        addTableHeader(itemsTable, "Produit", fontBold, BaseColor.LIGHT_GRAY);
        addTableHeader(itemsTable, "Quantité", fontBold, BaseColor.LIGHT_GRAY);
        addTableHeader(itemsTable, "Prix Unitaire", fontBold, BaseColor.LIGHT_GRAY);
        addTableHeader(itemsTable, "Sous-total", fontBold, BaseColor.LIGHT_GRAY);

        int itemCounter = 1;
        for (OrderItem item : invoice.getOrder().getOrderItems()) {
            // --- CORRECTION ICI ---
            String productName = item.getProduct().getNames().getOrDefault("fr", "N/A");
            // --- FIN DE LA CORRECTION ---
            addTableCell(itemsTable, String.valueOf(itemCounter++), fontNormal, Element.ALIGN_CENTER);
            addTableCell(itemsTable, productName, fontNormal, Element.ALIGN_LEFT); // Utilisation de la variable corrigée
            addTableCell(itemsTable, String.valueOf(item.getQuantity()), fontNormal, Element.ALIGN_CENTER);
            addTableCell(itemsTable, item.getUnitPrice().toString() + " DH", fontNormal, Element.ALIGN_RIGHT);
            addTableCell(itemsTable, item.getSubtotal().toString() + " DH", fontNormal, Element.ALIGN_RIGHT);
        }
        document.add(itemsTable);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(40);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        addTotalRow(totalsTable, "Total HT:", invoice.getTotalHT().toString() + " DH", fontNormal, fontNormal);
        if (invoice.getIncludesVAT()) {
            BigDecimal taxRateValue = invoice.getTotalHT().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : invoice.getTaxAmount().divide(invoice.getTotalHT(), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
            addTotalRow(totalsTable, "TVA (" + taxRateValue.stripTrailingZeros() + "%):", invoice.getTaxAmount().toString() + " DH", fontNormal, fontNormal);
        } else {
            addTotalRow(totalsTable, "TVA:", "Non applicable", fontNormal, fontNormal);
        }
        addTotalRow(totalsTable, "Total TTC:", invoice.getTotalTTC().toString() + " DH", fontTotal, fontTotal);
        document.add(totalsTable);

        document.add(Chunk.NEWLINE);

        Paragraph footer = new Paragraph("Merci pour votre commande ! Visitez notre site : mediatower.com", fontNormal);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);


        document.close();
        writer.close();
        return baos.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String headerText, Font font, BaseColor backgroundColor) {
        PdfPCell header = new PdfPCell(new Phrase(headerText, font));
        header.setBackgroundColor(backgroundColor);
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.setPadding(5);
        table.addCell(header);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(3);
        table.addCell(valueCell);
    }

    public InvoiceDto convertToDto(Invoice invoice) {
        return new InvoiceDto(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getOrder().getId(),
                invoice.getInvoiceDate(),
                invoice.getTotalHT(),
                invoice.getTaxAmount(),
                invoice.getTotalTTC(),
                invoice.getIncludesVAT(),
                invoice.getBillingAddress()
        );
    }
    public Optional<InvoiceDto> getInvoiceByOrderId(Long orderId) {
        return invoiceRepository.findByOrderId(orderId).map(this::convertToDto);
    }
}