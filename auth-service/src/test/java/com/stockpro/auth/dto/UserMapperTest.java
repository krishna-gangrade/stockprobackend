package com.stockpro.auth.dto;

import com.stockpro.auth.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper unit tests")
class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toUserResponse_mapsAllFields() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime lastLoginAt = LocalDateTime.now();
        User user = User.builder()
                .userId(1L)
                .fullName("Jane Doe")
                .email("jane@stockpro.com")
                .phone("9999999999")
                .role(User.Role.ADMIN)
                .isActive(true)
                .createdAt(createdAt)
                .lastLoginAt(lastLoginAt)
                .build();

        UserResponse response = userMapper.toUserResponse(user);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Jane Doe");
        assertThat(response.getEmail()).isEqualTo("jane@stockpro.com");
        assertThat(response.getPhone()).isEqualTo("9999999999");
        assertThat(response.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getLastLoginAt()).isEqualTo(lastLoginAt);
    }

    @Test
    void toUserResponse_returnsNullForNullUser() {
        assertThat(userMapper.toUserResponse(null)).isNull();
    }

    @Test
    void toUserResponseList_mapsUsersAndHandlesNull() {
        User user = User.builder()
                .userId(2L)
                .fullName("John Doe")
                .email("john@stockpro.com")
                .role(User.Role.WAREHOUSE_STAFF)
                .isActive(true)
                .build();

        List<UserResponse> responses = userMapper.toUserResponseList(List.of(user));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getEmail()).isEqualTo("john@stockpro.com");
        assertThat(userMapper.toUserResponseList(null)).isEmpty();
    }
}
