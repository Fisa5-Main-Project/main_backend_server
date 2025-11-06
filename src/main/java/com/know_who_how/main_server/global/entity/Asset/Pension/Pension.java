package com.know_who_how.main_server.global.entity.Asset.Pension;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "pensions")
@Getter
@RequiredArgsConstructor
public class Pension {

    @Id
    @Column(name = "asset_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "income_avg_3m", precision = 15, scale = 2)
    private BigDecimal incomeAvg3m;

    @Column(name = "working_years")
    private Integer workingYears;

    @Column(name = "principal", precision = 15, scale = 2)
    private BigDecimal principal;

    @Column(name = "company_contrib", precision = 15, scale = 2)
    private BigDecimal companyContrib;

    @Column(name = "personal_contrib", precision = 15, scale = 2)
    private BigDecimal personalContrib;

    @Column(name = "contrib_year")
    private Integer contribYear;

    @Column(name = "total_personal_contrib", precision = 15, scale = 2)
    private BigDecimal totalPersonalContrib;
}
