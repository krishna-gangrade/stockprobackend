package com.stockpro.product.service;

import com.stockpro.product.dto.ProductRequest;
import com.stockpro.product.response.ProductResponse;
import java.util.List;

public interface ProductService {
    ProductResponse getById(Long id);
    ProductResponse getBySku(String sku);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getActiveProducts();
    List<ProductResponse> getByCategory(String category);
    List<ProductResponse> search(String keyword);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    void deactivate(Long id);
    void activate(Long id);
    void delete(Long id);
}
