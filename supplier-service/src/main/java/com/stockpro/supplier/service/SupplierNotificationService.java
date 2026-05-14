package com.stockpro.supplier.service;

import com.stockpro.supplier.entity.Supplier;

public interface SupplierNotificationService {
    void sendSuspensionNotice(Supplier supplier);
}
