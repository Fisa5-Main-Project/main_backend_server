package com.know_who_how.main_server.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequestDto {
    // Refresh Token은 HttpOnly 쿠키로 관리되므로, 클라이언트에서 직접 전송하지 않습니다.
    // 따라서 LogoutRequestDto에서는 Refresh Token 필드를 제거합니다.
}
