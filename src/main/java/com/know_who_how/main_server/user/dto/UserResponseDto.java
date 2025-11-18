package com.know_who_how.main_server.user.dto;

import com.know_who_how.main_server.global.entity.User.Gender;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.entity.User.User;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long userId;
    private String loginId;
    private String name;
    private String phoneNum;
    private LocalDate birth;
    private Gender gender;
    private InvestmentTendancy investmentTendancy;
    private String provider;
    private boolean userMydataRegistration;
    private Long assetTotal;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
            .userId(user.getUserId())
            .loginId(user.getLoginId())
            .name(user.getName())
            .phoneNum(user.getPhoneNum())
            .birth(user.getBirth())
            .gender(user.getGender())
            .investmentTendancy(user.getInvestmentTendancy())
            .provider(user.getProvider())
            .userMydataRegistration(user.isUserMydataRegistration())
            .assetTotal(user.getAssetTotal())
            .build();
    }
}
