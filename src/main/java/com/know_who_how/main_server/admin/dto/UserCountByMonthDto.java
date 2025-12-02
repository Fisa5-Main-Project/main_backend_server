package com.know_who_how.main_server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCountByMonthDto {
    private Integer year;
    private Integer month;
    private Long count;
}
