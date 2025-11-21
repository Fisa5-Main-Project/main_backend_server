package com.know_who_how.main_server.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


// 페이지네이션 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {
    private int totalCount;
    private int currentPage;
    private int totalPages;
    private int itemsPerPage;
}
