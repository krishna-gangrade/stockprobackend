package com.stockpro.supplier.service;

import com.stockpro.supplier.exception.ApiException;
import com.stockpro.supplier.dto.SupplierRatingRequest;
import com.stockpro.supplier.dto.SupplierRequest;
import com.stockpro.supplier.dto.SupplierResponse;
import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierNotificationService supplierNotificationService;

    // =====================================================================
    // CREATE
    // =====================================================================

    @Override
    @Transactional
    @SuppressWarnings("null")
    public SupplierResponse createSupplier(SupplierRequest request) {
        if (supplierRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(
                    "Supplier with email '" + request.getEmail() + "' already exists",
                    HttpStatus.CONFLICT);
        }

        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .paymentTerms(request.getPaymentTerms() != null
                        ? request.getPaymentTerms() : "NET-30")
                .leadTimeDays(request.getLeadTimeDays())
                .rating(0.00)
                .ratingCount(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return mapToResponse(supplierRepository.save(supplier));
    }

    // =====================================================================
    // READ
    // =====================================================================

    @Override
    public SupplierResponse getById(Long id) {
        return mapToResponse(findOrThrow(id));
    }

    @Override
    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findByIsActive(true)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<SupplierResponse> searchSuppliers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllSuppliers();
        }
        return supplierRepository.searchByKeyword(keyword.trim())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<SupplierResponse> getByCity(String city) {
        return supplierRepository.findByCityIgnoreCase(city)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<SupplierResponse> getByCountry(String country) {
        return supplierRepository.findByCountryIgnoreCase(country)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<SupplierResponse> getTopRatedSuppliers() {
        return supplierRepository.findByIsActiveTrueOrderByRatingDesc()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // =====================================================================
    // UPDATE
    // =====================================================================

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = findOrThrow(id);

        // Only check email uniqueness if the email is actually changing
        if (!supplier.getEmail().equals(request.getEmail())
                && supplierRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(
                    "Email '" + request.getEmail() + "' is already used by another supplier",
                    HttpStatus.CONFLICT);
        }

        supplier.setName(request.getName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setCountry(request.getCountry());
        supplier.setPaymentTerms(request.getPaymentTerms());
        supplier.setLeadTimeDays(request.getLeadTimeDays());

        return mapToResponse(supplierRepository.save(supplier));
    }

    // =====================================================================
    // RATING — rolling average formula
    // =====================================================================

    @Override
    @Transactional
    public SupplierResponse updateRating(Long id, SupplierRatingRequest request) {
        Supplier supplier = findOrThrow(id);

        if (!supplier.isActive()) {
            throw new ApiException(
                    "Cannot rate an inactive supplier", HttpStatus.BAD_REQUEST);
        }

        // Rolling average formula:
        // newAvg = ((currentAvg * ratingCount) + newRating) / (ratingCount + 1)
        //
        // Example: supplier has avg=4.0 from 5 ratings, new rating=3.0
        //   newAvg = ((4.0 * 5) + 3.0) / 6 = 23/6 = 3.83
        //
        // We store ratingCount separately so we never lose the history
        double currentTotal = supplier.getRating() * supplier.getRatingCount();
        int    newCount      = supplier.getRatingCount() + 1;
        double newAverage    = (currentTotal + request.getRating()) / newCount;

        // Round to 2 decimal places
        supplier.setRating(Math.round(newAverage * 100.0) / 100.0);
        supplier.setRatingCount(newCount);

        return mapToResponse(supplierRepository.save(supplier));
    }

    // =====================================================================
    // DEACTIVATE / DELETE
    // =====================================================================

    @Override
    @Transactional
    public void deactivateSupplier(Long id) {
        Supplier supplier = findOrThrow(id);
        if (!supplier.isActive()) {
            return;
        }
        supplier.setActive(false);
        supplierRepository.save(supplier);
        supplierNotificationService.sendSuspensionNotice(supplier);
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new ApiException(
                    "Supplier not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        supplierRepository.deleteById(id);
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    @SuppressWarnings("null")
    private Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Supplier not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private SupplierResponse mapToResponse(Supplier s) {
        return SupplierResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .contactPerson(s.getContactPerson())
                .email(s.getEmail())
                .phone(s.getPhone())
                .address(s.getAddress())
                .city(s.getCity())
                .country(s.getCountry())
                .paymentTerms(s.getPaymentTerms())
                .leadTimeDays(s.getLeadTimeDays())
                .rating(s.getRating())
                .ratingCount(s.getRatingCount())
                .isActive(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
