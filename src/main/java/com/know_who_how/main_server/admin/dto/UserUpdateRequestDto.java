package com.know_who_how.main_server.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequestDto {
    private String name;
    private Boolean resetPassword;
    private Boolean disconnectMyData;
}
