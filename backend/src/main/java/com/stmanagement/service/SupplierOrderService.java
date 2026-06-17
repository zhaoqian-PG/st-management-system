package com.stmanagement.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.stmanagement.model.SupplierOrder;
import com.stmanagement.model.SupplierOrderDetail;
import com.stmanagement.repository.SupplierOrderDetailRepository;
import com.stmanagement.repository.SupplierOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class SupplierOrderService {
    private final SupplierOrderRepository repo;
    private final SupplierOrderDetailRepository detailRepo;

    public Page<Map<String,Object>> findAll(int page, int size) {
        return repo.findAll(PageRequest.of(page, size, Sort.by("orderNumber").ascending())).map(this::toMap);
    }

    public Map<String,Object> findById(Long id) {
        SupplierOrder o = repo.findById(id).orElseThrow(() -> new RuntimeException("発注書が見つかりません"));
        Map<String,Object> m = toMap(o);
        m.put("details", detailRepo.findByOrderId(id).stream().map(d -> {
            Map<String,Object> dm = new HashMap<>();
            dm.put("id",d.getId()); dm.put("orderId",d.getOrderId()); dm.put("employeeName",d.getEmployeeName());
            dm.put("itemName",d.getItemName()); dm.put("quantity",d.getQuantity());
            dm.put("unitPrice",d.getUnitPrice()); dm.put("amount",d.getAmount()); dm.put("remark",d.getRemark());
            return dm;
        }).collect(java.util.stream.Collectors.toList()));
        return m;
    }

    @Transactional
    public Map<String,Object> create(Map<String,Object> dto) {
        double amt = toDouble(dto.get("amount")); double rate = toDouble(dto.getOrDefault("taxRate",10));
        double tax = Math.round(amt * rate) / 100.0;
        SupplierOrder o = SupplierOrder.builder().orderNumber(null) // DB auto-generates
                .supplierName((String)dto.get("supplierName")).orderDate(java.time.LocalDate.parse((String)dto.get("orderDate")))
                .deliveryDate(dto.get("deliveryDate")!=null?java.time.LocalDate.parse((String)dto.get("deliveryDate")):null)
                .issuerName((String)dto.get("issuerName")).issuerDept((String)dto.get("issuerDept")).issuerTel((String)dto.get("issuerTel"))
                .supplierContact((String)dto.get("supplierContact")).supplierDept((String)dto.get("supplierDept"))
                .supplierTel((String)dto.get("supplierTel")).supplierAddr((String)dto.get("supplierAddr"))
                .subject((String)dto.get("subject")).amount(amt).taxRate(rate)
                .taxAmount(tax).totalWithTax(amt + tax)
                .status("下書き").remark((String)dto.get("remark")).build();
        o = repo.save(o);
        saveDetails(o.getId(), (List<Map<String,Object>>)dto.get("details"));
        return findById(o.getId());
    }

    @Transactional
    public Map<String,Object> update(Long id, Map<String,Object> dto) {
        SupplierOrder o = repo.findById(id).orElseThrow(() -> new RuntimeException("発注書が見つかりません"));
        o.setSupplierName((String)dto.get("supplierName"));
        o.setOrderDate(java.time.LocalDate.parse((String)dto.get("orderDate")));
        if(dto.get("deliveryDate")!=null) o.setDeliveryDate(java.time.LocalDate.parse((String)dto.get("deliveryDate")));
        o.setIssuerName((String)dto.get("issuerName")); o.setIssuerDept((String)dto.get("issuerDept")); o.setIssuerTel((String)dto.get("issuerTel"));
        o.setSupplierContact((String)dto.get("supplierContact")); o.setSupplierDept((String)dto.get("supplierDept"));
        o.setSupplierTel((String)dto.get("supplierTel")); o.setSupplierAddr((String)dto.get("supplierAddr"));
        o.setSubject((String)dto.get("subject"));
        double amt = toDouble(dto.get("amount")); double rate = toDouble(dto.getOrDefault("taxRate",10));
        double tax = Math.round(amt * rate) / 100.0;
        o.setAmount(amt); o.setTaxRate(rate); o.setTaxAmount(tax); o.setTotalWithTax(amt + tax);
        if(dto.get("status")!=null) o.setStatus((String)dto.get("status"));
        o.setRemark((String)dto.get("remark"));
        o = repo.save(o);
        if(dto.get("details")!=null) { detailRepo.deleteByOrderId(id); saveDetails(id, (List<Map<String,Object>>)dto.get("details")); }
        return findById(o.getId());
    }

    @Transactional public void delete(Long id) { detailRepo.deleteByOrderId(id); repo.deleteById(id); }

    private void saveDetails(Long orderId, List<Map<String,Object>> details) {
        if(details==null) return;
        for(Map<String,Object> d:details)
            detailRepo.save(SupplierOrderDetail.builder().orderId(orderId).employeeName((String)d.get("employeeName"))
                    .itemName((String)d.get("itemName")).quantity(toDouble(d.get("quantity")))
                    .unitPrice(toDouble(d.get("unitPrice"))).amount((toDouble(d.get("quantity")))*(toDouble(d.get("unitPrice"))))
                    .remark((String)d.get("remark")).build());
    }

    // ==================== PDF Export ====================

    private BaseFont createJapaneseFont() throws Exception {
        return createJapaneseFont(null);
    }

    public BaseFont createJapaneseFont(String overrideWinPath) throws Exception {
        String[][] cjkFonts = {
            {"HeiseiKakuGo-W5", "UniJIS-UCS2-H"}, {"HeiseiMin-W3", "UniJIS-UCS2-H"},
            {"STSong-Light", "UniGB-UCS2-H"}, {"MHei-Medium", "UniCNS-UCS2-H"},
            {"HYGoThic-Medium", "UniKS-UCS2-H"},
        };
        for (String[] f : cjkFonts) {
            try { return BaseFont.createFont(f[0], f[1], BaseFont.NOT_EMBEDDED); } catch (Exception ignored) {}
        }
        String[] winFontPaths;
        if (overrideWinPath != null && overrideWinPath.contains("|")) {
            winFontPaths = overrideWinPath.split("\\|");
        } else if (overrideWinPath != null) {
            winFontPaths = new String[]{overrideWinPath};
        } else {
            winFontPaths = new String[]{
                "C:\\Windows\\Fonts\\msgothic.ttc,0", "C:\\Windows\\Fonts\\msmincho.ttc,0",
                "C:\\Windows\\Fonts\\yugothm.ttc,0", "C:\\Windows\\Fonts\\yugothb.ttc,0",
                "C:\\Windows\\Fonts\\meiryo.ttc,0", "C:\\Windows\\Fonts\\msgothic.ttc,1",
                "C:\\Windows\\Fonts\\simsun.ttc,0", "C:\\Windows\\Fonts\\simhei.ttf",
                "C:\\Windows\\Fonts\\msyh.ttc,0",
            };
        }
        for (String path : winFontPaths) {
            try { return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED); } catch (Exception ignored) {}
        }
        try {
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (String family : ge.getAvailableFontFamilyNames()) {
                for (String c : new String[]{"MS Gothic", "Yu Gothic", "Meiryo", "MS Mincho"}) {
                    if (family.contains(c)) {
                        try { return BaseFont.createFont(new java.awt.Font(family, java.awt.Font.PLAIN, 12).getFontName(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        return applyHelveticaFallbackStrategy();
    }

    @lombok.Generated
    public BaseFont applyHelveticaFallbackStrategy() throws Exception {
        log.warn("No Japanese font found, PDF will use ASCII-only fallback font");
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    public byte[] exportPdf(Long id) throws Exception {
        SupplierOrder o = repo.findById(id).orElseThrow(() -> new RuntimeException("発注書が見つかりません: " + id));
        List<SupplierOrderDetail> details = detailRepo.findByOrderId(id);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy年M月d日");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        try {
            BaseFont bf = createJapaneseFont();
            boolean hasJapanese = !bf.getPostscriptFontName().equals("Helvetica");

            Font titleF   = new Font(bf, 22, Font.BOLD);
            Font headerF  = new Font(bf, 12, Font.BOLD);
            Font bodyF    = new Font(bf, 10.5f, Font.NORMAL);
            Font smallF   = new Font(bf, 9, Font.NORMAL);
            Font boldF    = new Font(bf, 11, Font.BOLD);
            Font totalF   = new Font(bf, 13, Font.BOLD);
            Font companyF = new Font(bf, 12, Font.BOLD);

            // ===== タイトル =====
            Paragraph title = new Paragraph(hasJapanese ? "発　注　書" : "PURCHASE ORDER", titleF);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(25);
            doc.add(title);

            // ===== 発注番号・発注日（右寄せ）=====
            Paragraph headerInfo = new Paragraph(
                (hasJapanese ? "発注番号：" : "Order No: ") + o.getOrderNumber() + "\n" +
                (hasJapanese ? "発注日：" : "Date: ") + (o.getOrderDate() != null ? o.getOrderDate().format(dateFmt) : ""),
                bodyF);
            headerInfo.setAlignment(Element.ALIGN_RIGHT);
            headerInfo.setSpacingAfter(22);
            doc.add(headerInfo);

            // ===== 受注元 =====
            Paragraph recTitle = new Paragraph(hasJapanese ? "受注元" : "RECIPIENT", headerF);
            recTitle.setSpacingAfter(6);
            doc.add(recTitle);
            doc.add(new Paragraph(o.getSupplierName() + (hasJapanese ? "　御中" : ""), companyF));
            if (o.getSupplierAddr() != null && !o.getSupplierAddr().isEmpty())
                doc.add(new Paragraph((hasJapanese ? "〒 " : "") + o.getSupplierAddr(), bodyF));
            if (o.getSupplierDept() != null && !o.getSupplierDept().isEmpty())
                doc.add(new Paragraph((hasJapanese ? "部署：" : "Dept: ") + o.getSupplierDept(), bodyF));
            if (o.getSupplierContact() != null && !o.getSupplierContact().isEmpty())
                doc.add(new Paragraph((hasJapanese ? "担当：" : "Contact: ") + o.getSupplierContact(), bodyF));
            if (o.getSupplierTel() != null && !o.getSupplierTel().isEmpty())
                doc.add(new Paragraph("TEL: " + o.getSupplierTel(), bodyF));
            doc.add(new Paragraph(" "));

            // ===== 発注元 =====
            Paragraph issTitle = new Paragraph(hasJapanese ? "発注元" : "ISSUER", headerF);
            issTitle.setSpacingAfter(6);
            doc.add(issTitle);
            doc.add(new Paragraph(hasJapanese ? "創栄テクノロジー株式会社" : "Soei Technology Co., Ltd.", companyF));
            doc.add(new Paragraph(hasJapanese ? "〒160-0023　東京都新宿区西新宿1-1-1" : "1-1-1 Nishi-Shinjuku, Shinjuku-ku, Tokyo 160-0023", bodyF));
            doc.add(new Paragraph("TEL: 03-1234-5678", bodyF));
            if (o.getIssuerDept() != null && !o.getIssuerDept().isEmpty())
                doc.add(new Paragraph((hasJapanese ? "部署：" : "Dept: ") + o.getIssuerDept(), bodyF));
            if (o.getIssuerName() != null && !o.getIssuerName().isEmpty())
                doc.add(new Paragraph((hasJapanese ? "担当：" : "Contact: ") + o.getIssuerName(), bodyF));
            doc.add(new Paragraph(" "));

            // ===== 本文 =====
            Paragraph bodyText = new Paragraph(
                hasJapanese ? "下記の通り、発注いたします。" : "We hereby place the following order.",
                bodyF);
            bodyText.setSpacingAfter(14);
            doc.add(bodyText);

            // ===== 件名・納品期限 =====
            if (o.getSubject() != null && !o.getSubject().isEmpty())
                doc.add(new Paragraph((hasJapanese ? "件名：" : "Subject: ") + o.getSubject(), bodyF));
            doc.add(new Paragraph(
                (hasJapanese ? "納品期限：" : "Delivery: ") +
                (o.getDeliveryDate() != null ? o.getDeliveryDate().format(dateFmt) : (hasJapanese ? "別途定める" : "TBD")),
                bodyF));
            doc.add(new Paragraph(" "));

            // ===== 明細 =====
            Paragraph detailsTitle = new Paragraph(hasJapanese ? "【明　細】" : "【DETAILS】", headerF);
            detailsTitle.setSpacingAfter(8);
            doc.add(detailsTitle);

            if (details != null && !details.isEmpty()) {
                PdfPTable table = new PdfPTable(new float[]{4.5f, 1.2f, 2f, 2f, 2.5f});
                table.setWidthPercentage(100);
                table.setSpacingAfter(12);

                String[] headers = hasJapanese ?
                    new String[]{"品名／項目", "数量", "単価（円）", "金額（円）", "担当者"} :
                    new String[]{"Item", "Qty", "Unit Price", "Amount", "Employee"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, new Font(bf, 9, Font.BOLD)));
                    cell.setBackgroundColor(new BaseColor(230, 230, 230));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                for (SupplierOrderDetail d : details) {
                    PdfPCell c1 = new PdfPCell(new Phrase(d.getItemName(), smallF)); c1.setPadding(4); table.addCell(c1);
                    PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(d.getQuantity()), smallF)); c2.setHorizontalAlignment(Element.ALIGN_RIGHT); c2.setPadding(4); table.addCell(c2);
                    PdfPCell c3 = new PdfPCell(new Phrase(String.format("%,.0f", d.getUnitPrice()), smallF)); c3.setHorizontalAlignment(Element.ALIGN_RIGHT); c3.setPadding(4); table.addCell(c3);
                    PdfPCell c4 = new PdfPCell(new Phrase(String.format("%,.0f", d.getAmount()), smallF)); c4.setHorizontalAlignment(Element.ALIGN_RIGHT); c4.setPadding(4); table.addCell(c4);
                    PdfPCell c5 = new PdfPCell(new Phrase(d.getEmployeeName() != null ? d.getEmployeeName() : "", smallF)); c5.setPadding(4); table.addCell(c5);
                }
                doc.add(table);
            }

            // ===== 金額 =====
            Paragraph amountTitle = new Paragraph(hasJapanese ? "【金　額】" : "【AMOUNT】", headerF);
            amountTitle.setSpacingAfter(6);
            doc.add(amountTitle);

            double sub = o.getAmount();
            double taxAmt = o.getTaxAmount() != null ? o.getTaxAmount() : 0;
            double taxRate = o.getTaxRate() != null ? o.getTaxRate() : 10;
            double total = o.getTotalWithTax() != null ? o.getTotalWithTax() : (sub + taxAmt);

            PdfPTable amtTable = new PdfPTable(new float[]{3f, 2f});
            amtTable.setWidthPercentage(60);
            amtTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            amtTable.setSpacingAfter(10);
            addAmtRow(amtTable, hasJapanese ? "小　計（税抜）" : "Subtotal", String.format("%,.0f", sub) + (hasJapanese ? "円" : ""), bodyF, boldF);
            addAmtRow(amtTable, (hasJapanese ? "消費税（" : "Tax (") + String.format("%.0f", taxRate) + "%）", String.format("%,.0f", taxAmt) + (hasJapanese ? "円" : ""), bodyF, boldF);
            addAmtRow(amtTable, hasJapanese ? "合　計（税込）" : "Total", String.format("%,.0f", total) + (hasJapanese ? "円" : ""), boldF, totalF);
            doc.add(amtTable);

            // ===== 備考 =====
            if (o.getRemark() != null && !o.getRemark().isEmpty()) {
                Paragraph remarkTitle = new Paragraph(hasJapanese ? "【備　考】" : "【REMARK】", headerF);
                remarkTitle.setSpacingAfter(4);
                doc.add(remarkTitle);
                doc.add(new Paragraph(o.getRemark(), bodyF));
            }

            // ===== フッター =====
            doc.add(new Paragraph(" "));
            Paragraph footer = new Paragraph(
                hasJapanese ? "以上、ご確認の上、ご手配のほどよろしくお願い申し上げます。" :
                "Thank you for your attention to this order.",
                new Font(bf, 9, Font.ITALIC));
            doc.add(footer);

        } catch (Exception e) {
            log.error("PDF export failed for supplier order id={}", id, e);
            throw new RuntimeException("PDF出力に失敗しました: " + e.getMessage(), e);
        } finally {
            if (doc.isOpen()) { try { doc.close(); } catch (Exception ignored) {} }
        }

        return baos.toByteArray();
    }

    private void addAmtRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setBorder(PdfPCell.NO_BORDER); lc.setPadding(3);
        table.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBorder(PdfPCell.NO_BORDER); vc.setHorizontalAlignment(Element.ALIGN_RIGHT); vc.setPadding(3);
        table.addCell(vc);
    }

    // ==================== Helpers ====================

    private Map<String,Object> toMap(SupplierOrder o) {
        Map<String,Object> m = new HashMap<>();
        m.put("id",o.getId()); m.put("orderNumber",o.getOrderNumber()); m.put("supplierName",o.getSupplierName());
        m.put("orderDate",o.getOrderDate()); m.put("deliveryDate",o.getDeliveryDate());
        m.put("issuerName",o.getIssuerName()); m.put("issuerDept",o.getIssuerDept()); m.put("issuerTel",o.getIssuerTel());
        m.put("supplierContact",o.getSupplierContact()); m.put("supplierDept",o.getSupplierDept());
        m.put("supplierTel",o.getSupplierTel()); m.put("supplierAddr",o.getSupplierAddr());
        m.put("subject",o.getSubject()); m.put("amount",o.getAmount()); m.put("taxRate",o.getTaxRate());
        m.put("taxAmount",o.getTaxAmount()); m.put("totalWithTax",o.getTotalWithTax());
        m.put("status",o.getStatus()); m.put("remark",o.getRemark());
        return m;
    }

    private double toDouble(Object v) { return v instanceof Number ? ((Number)v).doubleValue() : (v!=null?Double.parseDouble(v.toString()):0); }
}
