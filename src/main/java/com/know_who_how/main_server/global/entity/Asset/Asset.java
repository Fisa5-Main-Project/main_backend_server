package com.know_who_how.main_server.global.entity.Asset;

import com.know_who_how.main_server.global.entity.User.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;

    //Cascade 삭제 user_id == null 유발
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AssetType type;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;


    @Column(name = "bank_code", nullable = true)
    private String bankCode;
}