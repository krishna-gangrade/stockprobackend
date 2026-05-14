package com.stockpro.supplier.service;

import com.stockpro.supplier.dto.SupplierRatingRequest;
import com.stockpro.supplier.dto.SupplierRequest;
import com.stockpro.supplier.dto.SupplierResponse;

import java.util.List;

public interface SupplierService {

    SupplierResponse        createSupplier(SupplierRequest request);
    SupplierResponse        getById(Long id);
    List<SupplierResponse>  getAllSuppliers();
    List<SupplierResponse>  getActiveSuppliers();
    List<SupplierResponse>  searchSuppliers(String keyword);
    List<SupplierResponse>  getByCity(String city);
    List<SupplierResponse>  getByCountry(String country);
    List<SupplierResponse>  getTopRatedSuppliers();
    SupplierResponse        updateSupplier(Long id, SupplierRequest request);
    SupplierResponse        updateRating(Long id, SupplierRatingRequest request);
    void                    deactivateSupplier(Long id);
    void                    deleteSupplier(Long id);
}
