package com.know_who_how.main_server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGrowthResponseDto {
    private String month;
    private long users;
    private long newUsers;
    private long dau;
}
