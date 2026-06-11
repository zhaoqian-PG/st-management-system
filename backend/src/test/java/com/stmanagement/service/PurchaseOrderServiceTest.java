package com.stmanagement.service;

import com.stmanagement.dto.PurchaseOrderDTO;
import com.stmanagement.model.Customer;
import com.stmanagement.model.PurchaseOrder;
import com.stmanagement.model.PurchaseOrderDetail;
import com.stmanagement.model.PurchaseOrderAttachment;
import com.stmanagement.repository.CustomerRepository;
import com.stmanagement.repository.PurchaseOrderAttachmentRepository;
import com.stmanagement.repository.PurchaseOrderDetailRepository;
import com.stmanagement.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository orderRepository;
    @Mock private PurchaseOrderDetailRepository detailRepository;
    @Mock private PurchaseOrderAttachmentRepository attachmentRepository;
    @Mock private CustomerRepository customerRepository;
    @InjectMocks private PurchaseOrderService service;

    private PurchaseOrder order;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L); customer.setCompanyName("テスト株式会社");

        order = PurchaseOrder.builder().id(1L).orderNumber("PO-2026-0001")
                .customerId(1L).orderDate(LocalDate.of(2026, 5, 1))
                .deliveryDate(LocalDate.of(2026, 6, 15))
                .subject("テスト発注").amount(1000000.0).taxRate(10.0)
                .taxAmount(100000.0).totalWithTax(1100000.0)
                .status("下書き").remark("テスト備考")
                .issuerName("山田 太郎").issuerDept("営業部").issuerTel("090-1111-2222")
                .recipientName("田中 花子").recipientDept("開発部").recipientTel("03-1234-5678")
                .build();
    }

    @Test
    void testFindAll() {
        List<PurchaseOrder> orders = Collections.singletonList(order);
        Page<PurchaseOrder> page = new PageImpl<>(orders);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Page<PurchaseOrderDTO> result = service.findAll(null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PO-2026-0001", result.getContent().get(0).getOrderNumber());
        assertEquals("テスト株式会社", result.getContent().get(0).getCustomerName());
        verify(orderRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void testFindById() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        PurchaseOrderDTO result = service.findById(1L);

        assertNotNull(result);
        assertEquals("PO-2026-0001", result.getOrderNumber());
        assertEquals("テスト株式会社", result.getCustomerName());
        assertEquals("下書き", result.getStatus());
        assertNotNull(result.getDetails());
        assertNotNull(result.getAttachments());
    }

    @Test
    void testFindById_NotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }

    @Test
    void testCreate() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder()
                .customerId(1L).orderDate(LocalDate.of(2026, 5, 1))
                .subject("新規発注").amount(500000.0).taxRate(10.0).build();
        when(orderRepository.save(any(PurchaseOrder.class))).thenReturn(order);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        PurchaseOrderDTO result = service.create(dto);

        assertNotNull(result);
        assertEquals("下書き", result.getStatus());
        verify(orderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void testUpdate() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder()
                .customerId(1L).orderDate(LocalDate.of(2026, 6, 1))
                .subject("更新発注").amount(800000.0).taxRate(10.0).status("発注済").build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(PurchaseOrder.class))).thenReturn(order);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        PurchaseOrderDTO result = service.update(1L, dto);

        assertNotNull(result);
        assertEquals("発注済", result.getStatus());
        verify(orderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void testDelete() {
        doNothing().when(detailRepository).deleteByOrderId(1L);
        doNothing().when(attachmentRepository).deleteByOrderId(1L);
        doNothing().when(orderRepository).deleteById(1L);

        service.delete(1L);

        verify(detailRepository).deleteByOrderId(1L);
        verify(attachmentRepository).deleteByOrderId(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void testToDTO_includesAttachmentPath() {
        order.setAttachmentPath("uploads/test.pdf");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        PurchaseOrderDTO result = service.findById(1L);

        assertEquals("uploads/test.pdf", result.getAttachmentPath());
    }

    @Test
    void testToDTO_includesIssuerFields() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        PurchaseOrderDTO result = service.findById(1L);

        assertEquals("山田 太郎", result.getIssuerName());
        assertEquals("営業部", result.getIssuerDept());
        assertEquals("090-1111-2222", result.getIssuerTel());
    }

    @Test
    void testToDTO_includesRecipientFields() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        PurchaseOrderDTO result = service.findById(1L);

        assertEquals("田中 花子", result.getRecipientName());
        assertEquals("開発部", result.getRecipientDept());
        assertEquals("03-1234-5678", result.getRecipientTel());
    }

    @Test
    void testFindAll_withFilters() {
        List<PurchaseOrder> orders = Collections.singletonList(order);
        Page<PurchaseOrder> page = new PageImpl<>(orders);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Page<PurchaseOrderDTO> result = service.findAll(1L, "下書き", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
