package com.stockpro.supplier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {

    private Long          id;
    private String        name;
    private String        contactPerson;
    private String        email;
    private String        phone;
    private String        address;
    private String        city;
    private String        country;
    private String        paymentTerms;
    private int           leadTimeDays;
    private double        rating;
    private int           ratingCount;
    private boolean       isActive;
    private LocalDateTime createdAt;
}
