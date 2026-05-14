package com.stockpro.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenRefreshResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
}
