package com.know_who_how.main_server.admin.dto;

import com.know_who_how.main_server.global.entity.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long id;
    private String name;
    private String loginId; // loginId
    private int age;
    private Long totalAsset;
    private LocalDateTime joinDate; // createdDate
    private LocalDateTime lastActive; // 마지막 활동일 (현재는 목업)
    private String status; // "active" | "inactive"

    public static UserResponseDto fromEntity(User user) {
        int age = user.getBirth() != null ? Period.between(user.getBirth(), LocalDate.now()).getYears() : 0;
        
        return UserResponseDto.builder()
                .id(user.getUserId())
                .name(user.getName())
                .loginId(user.getLoginId())
                .age(age)
                .totalAsset(user.getAssetTotal())
                .joinDate(user.getCreatedDate())
                .lastActive(null) // TODO: 실제 사용자 마지막 활동 시간을 추적하는 로직 구현 필요
                .status(user.isEnabled() ? "active" : "inactive")
                .build();
    }
}
