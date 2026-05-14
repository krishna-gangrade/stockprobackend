package com.stockpro.supplier.repository;

import com.stockpro.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByIsActive(boolean isActive);

    List<Supplier> findByCityIgnoreCase(String city);

    List<Supplier> findByCountryIgnoreCase(String country);

    boolean existsByEmail(String email);

    // Search by name or contact person — case-insensitive partial match
    @Query("SELECT s FROM Supplier s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Supplier> searchByKeyword(@Param("keyword") String keyword);

    // Top suppliers sorted by rating descending
    List<Supplier> findByIsActiveTrueOrderByRatingDesc();
}
