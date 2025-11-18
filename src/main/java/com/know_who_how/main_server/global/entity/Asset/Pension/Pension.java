package com.know_who_how.main_server.global.entity.Asset.Pension;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "pension") // Table name is 'pension'
public class Pension {

    @Id
    @Column(name = "asset_id")
    private Long assetId; // PK and FK to Asset

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Maps the primary key of the owning entity to the primary key of the associated entity
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "pension_type")
    private PensionType pensionType;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "income_avg_3m", precision = 15, scale = 2)
    private BigDecimal incomeAvg3m;

    @Column(name = "working_date")
    private Integer workingDate;

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