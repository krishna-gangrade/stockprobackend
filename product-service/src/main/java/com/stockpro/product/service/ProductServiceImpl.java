package com.stockpro.product.service;

import com.stockpro.product.dto.ProductRequest;
import com.stockpro.product.entity.Product;
import com.stockpro.product.repository.ProductRepository;
import com.stockpro.product.response.ProductResponse;
import com.stockpro.product.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found with id: " + id, HttpStatus.NOT_FOUND));
        return mapToResponse(product);
    }

    @Override
    public ProductResponse getBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ApiException("Product not found with SKU: " + sku, HttpStatus.NOT_FOUND));
        return mapToResponse(product);
    }


    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllProducts();
        }
        String value = keyword.trim();
        return productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                        value, value, value)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new ApiException("Product with SKU " + request.getSku() + " already exists", HttpStatus.CONFLICT);
        }
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .brand(request.getBrand())
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .reorderLevel(request.getReorderLevel())
                .maxStockLevel(request.getMaxStockLevel())
                .leadTimeDays(request.getLeadTimeDays())
                .imageUrl(request.getImageUrl())
                .active(request.isActive())
                .unit(request.getUnitOfMeasure())
                .build();
        
        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found with id: " + id, HttpStatus.NOT_FOUND));
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setReorderLevel(request.getReorderLevel());
        product.setMaxStockLevel(request.getMaxStockLevel());
        product.setLeadTimeDays(request.getLeadTimeDays());
        product.setImageUrl(request.getImageUrl());
        product.setUnit(request.getUnitOfMeasure());
        product.setActive(request.isActive());
        
        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        updateActiveStatus(id, false);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        updateActiveStatus(id, true);
    }

    private void updateActiveStatus(Long id, boolean status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found with id: " + id, HttpStatus.NOT_FOUND));
        product.setActive(status);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ApiException("Product not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .sku(p.getSku())
                .category(p.getCategory())
                .brand(p.getBrand())
                .costPrice(p.getCostPrice())
                .sellingPrice(p.getSellingPrice())
                .unitOfMeasure(p.getUnit())
                .reorderLevel(p.getReorderLevel())
                .maxStockLevel(p.getMaxStockLevel())
                .leadTimeDays(p.getLeadTimeDays())
                .active(p.isActive())
                .imageUrl(p.getImageUrl())
                .build();
    }
}
