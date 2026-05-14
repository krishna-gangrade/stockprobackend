package com.stockpro.report.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private Long id;
    private String name;
    private String location;
    private String address;
    private Long managerId;
    private int capacity;
    private int usedCapacity;
    private String phone;
    private boolean isActive;
    private LocalDateTime createdAt;
}
