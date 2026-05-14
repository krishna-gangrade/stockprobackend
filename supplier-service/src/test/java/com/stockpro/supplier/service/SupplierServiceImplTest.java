package com.stockpro.supplier.service;

import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierNotificationService supplierNotificationService;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Test
    @DisplayName("deactivateSupplier marks supplier inactive and sends suspension email")
    void deactivateSupplier_sendsSuspensionNotice() {
        Supplier supplier = Objects.requireNonNull(Supplier.builder()
                .id(1L)
                .name("Acme Supplies")
                .email("acme@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deactivateSupplier(1L);

        assertThat(supplier.isActive()).isFalse();
        verify(supplierRepository).save(supplier);
        verify(supplierNotificationService).sendSuspensionNotice(supplier);
    }

    @Test
    @DisplayName("deactivateSupplier is a no-op for already inactive suppliers")
    void deactivateSupplier_alreadyInactive_skipsEmail() {
        Supplier supplier = Objects.requireNonNull(Supplier.builder()
                .id(1L)
                .name("Acme Supplies")
                .email("acme@example.com")
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .build());

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deactivateSupplier(1L);

        verify(supplierRepository, never()).save(supplier);
        verify(supplierNotificationService, never()).sendSuspensionNotice(supplier);
    }
}
