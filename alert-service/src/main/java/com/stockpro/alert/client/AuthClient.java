package com.stockpro.alert.client;

import com.stockpro.alert.dto.external.UserResponse;
import com.stockpro.alert.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", path = "/api/v1/auth")
public interface AuthClient {

    @GetMapping("/users/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") Long userId);
}
