package com.stockpro.alert.dto.external;

import lombok.Data;

@Data
public class UserResponse {
    private Long userId;
    private String fullName;
    private String email;
}
