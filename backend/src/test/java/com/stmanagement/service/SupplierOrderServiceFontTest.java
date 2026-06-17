package com.stmanagement.service;

import com.itextpdf.text.pdf.BaseFont;
import com.stmanagement.model.SupplierOrder;
import com.stmanagement.repository.SupplierOrderDetailRepository;
import com.stmanagement.repository.SupplierOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SupplierOrderServiceFontTest {
    @Mock private SupplierOrderRepository repo;
    @Mock private SupplierOrderDetailRepository detailRepo;
    @InjectMocks private SupplierOrderService service;

    private SupplierOrder order;

    @BeforeEach void setUp() {
        order = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001")
            .supplierName("テスト").orderDate(LocalDate.now())
            .amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0)
            .status("下書き").build();
    }

    // Strategy 2: null → default 9 font paths (msgothic.ttc succeeds)
    @Test void testFont_null_defaultPaths() throws Exception {
        BaseFont f = service.createJapaneseFont(null);
        assertNotNull(f);
        assertFalse("Helvetica".equals(f.getPostscriptFontName()));
    }

    // Strategy 2: single ghost path → fail → fall to AWT
    @Test void testFont_singleGhost_goesToAWT() throws Exception {
        BaseFont f = service.createJapaneseFont("C:\\NOT_EXIST\\ghost.ttc,0");
        assertNotNull(f);
    }

    // Strategy 2: pipe-separated paths → fake fails → real succeeds
    @Test void testFont_pipeDelimited_fakeThenReal() throws Exception {
        BaseFont f = service.createJapaneseFont(
            "C:\\NOT_EXIST\\fake.ttc,0|C:\\Windows\\Fonts\\msgothic.ttc,0");
        assertNotNull(f);
        assertFalse("Helvetica".equals(f.getPostscriptFontName()));
    }

    // Strategy 3: headless=true → AWT break → catch(Exception ignored)
    @Test void testFont_headless_breaksAWT() throws Exception {
        String orig = System.setProperty("java.awt.headless", "true");
        try {
            BaseFont f = service.createJapaneseFont("C:\\NOT_EXIST\\ghost.ttc,0");
            assertNotNull(f);
        } finally {
            if (orig == null) System.clearProperty("java.awt.headless");
            else System.setProperty("java.awt.headless", orig);
        }
    }

    // Strategy 4: direct Helvetica fallback
    @Test void testFont_helveticaFallback() throws Exception {
        BaseFont f = service.applyHelveticaFallbackStrategy();
        assertNotNull(f);
        assertEquals("Helvetica", f.getPostscriptFontName());
    }
}
