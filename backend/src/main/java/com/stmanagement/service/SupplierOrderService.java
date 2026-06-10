package com.stmanagement.service;

import com.stmanagement.model.SupplierOrder;
import com.stmanagement.repository.SupplierOrderRepository;
import lombok.RequiredArgsConstructor;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
public class SupplierOrderService {
    private final SupplierOrderRepository repo;

    public Page<Map<String,Object>> findAll(int page, int size) {
        return repo.findAll(PageRequest.of(page, size, Sort.by("orderDate").descending())).map(o -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id",o.getId()); m.put("orderNumber",o.getOrderNumber()); m.put("supplierName",o.getSupplierName());
            m.put("orderDate",o.getOrderDate()); m.put("deliveryDate",o.getDeliveryDate()); m.put("subject",o.getSubject());
            m.put("amount",o.getAmount()); m.put("taxRate",o.getTaxRate()); m.put("taxAmount",o.getTaxAmount());
            m.put("totalWithTax",o.getTotalWithTax()); m.put("status",o.getStatus()); m.put("remark",o.getRemark());
            return m;
        });
    }

    public Map<String,Object> findById(Long id) {
        SupplierOrder o = repo.findById(id).orElseThrow(() -> new RuntimeException("発注書が見つかりません"));
        Map<String,Object> m = new HashMap<>();
        m.put("id",o.getId()); m.put("orderNumber",o.getOrderNumber()); m.put("supplierName",o.getSupplierName());
        m.put("orderDate",o.getOrderDate()); m.put("deliveryDate",o.getDeliveryDate()); m.put("subject",o.getSubject());
        m.put("amount",o.getAmount()); m.put("taxRate",o.getTaxRate()); m.put("taxAmount",o.getTaxAmount());
        m.put("totalWithTax",o.getTotalWithTax()); m.put("status",o.getStatus()); m.put("remark",o.getRemark());
        return m;
    }

    @Transactional
    public Map<String,Object> create(Map<String,Object> dto) {
        double amt = toDouble(dto.get("amount"));
        double rate = dto.get("taxRate") != null ? toDouble(dto.get("taxRate")) : 10;
        double tax = Math.round(amt * rate) / 100.0;
        SupplierOrder o = SupplierOrder.builder()
                .orderNumber((String)dto.get("orderNumber")).supplierName((String)dto.get("supplierName"))
                .orderDate(java.time.LocalDate.parse((String)dto.get("orderDate")))
                .deliveryDate(dto.get("deliveryDate")!=null?java.time.LocalDate.parse((String)dto.get("deliveryDate")):null)
                .subject((String)dto.get("subject")).amount(amt).taxRate(rate).taxAmount(tax).totalWithTax(amt+tax)
                .status("下書き").remark((String)dto.get("remark")).build();
        o = repo.save(o);
        return findById(o.getId());
    }

    @Transactional
    public Map<String,Object> update(Long id, Map<String,Object> dto) {
        SupplierOrder o = repo.findById(id).orElseThrow(() -> new RuntimeException("発注書が見つかりません"));
        o.setSupplierName((String)dto.get("supplierName"));
        o.setOrderDate(java.time.LocalDate.parse((String)dto.get("orderDate")));
        if(dto.get("deliveryDate")!=null) o.setDeliveryDate(java.time.LocalDate.parse((String)dto.get("deliveryDate")));
        o.setSubject((String)dto.get("subject"));
        double amt = toDouble(dto.get("amount")); double rate = dto.get("taxRate")!=null?toDouble(dto.get("taxRate")):10;
        o.setAmount(amt); o.setTaxRate(rate); o.setTaxAmount(Math.round(amt*rate)/100.0); o.setTotalWithTax(amt+Math.round(amt*rate)/100.0);
        if(dto.get("status")!=null) o.setStatus((String)dto.get("status"));
        o.setRemark((String)dto.get("remark"));
        return findById(repo.save(o).getId());
    }

    @Transactional public void delete(Long id) { repo.deleteById(id); }

    public byte[] exportPdf(Long id) throws Exception {
        SupplierOrder o = repo.findById(id).orElseThrow(() -> new RuntimeException("発注書が見つかりません"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        // Japanese font
        BaseFont bf = BaseFont.createFont("HeiseiKakuGo-W5", "UniJIS-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bf, 20, Font.BOLD);
        Font headFont = new Font(bf, 12, Font.BOLD);
        Font bodyFont = new Font(bf, 10, Font.NORMAL);

        // Title
        Paragraph title = new Paragraph("　発　注　書", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
        doc.add(new Paragraph("\n"));

        // Info
        doc.add(new Paragraph("発注番号：" + o.getOrderNumber(), bodyFont));
        doc.add(new Paragraph("発注日：" + (o.getOrderDate() != null ? o.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) : ""), bodyFont));
        doc.add(new Paragraph("納品期限：" + (o.getDeliveryDate() != null ? o.getDeliveryDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) : "未定"), bodyFont));
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("【発注先】", headFont));
        doc.add(new Paragraph("　" + o.getSupplierName() + "　御中", bodyFont));
        doc.add(new Paragraph("\n"));
        if (o.getSubject() != null) doc.add(new Paragraph("件名：" + o.getSubject(), bodyFont));
        doc.add(new Paragraph("\n"));

        // Amounts
        doc.add(new Paragraph("【金額】", headFont));
        doc.add(new Paragraph("　税抜金額：¥" + String.format("%,.0f", o.getAmount()), bodyFont));
        double rate = o.getTaxRate() != null ? o.getTaxRate() : 10;
        double tax = o.getTaxAmount() != null ? o.getTaxAmount() : 0;
        double total = o.getTotalWithTax() != null ? o.getTotalWithTax() : o.getAmount() + tax;
        doc.add(new Paragraph("　消費税(" + String.format("%.0f", rate) + "%)：¥" + String.format("%,.0f", tax), bodyFont));
        doc.add(new Paragraph("　税込合計：¥" + String.format("%,.0f", total), new Font(bf, 12, Font.BOLD)));
        doc.add(new Paragraph("\n"));

        // Status
        doc.add(new Paragraph("状態：" + o.getStatus(), bodyFont));
        if (o.getRemark() != null) doc.add(new Paragraph("備考：" + o.getRemark(), bodyFont));

        doc.close();
        return baos.toByteArray();
    }

    private double toDouble(Object v) { return v instanceof Number ? ((Number)v).doubleValue() : Double.parseDouble(v.toString()); }
}
