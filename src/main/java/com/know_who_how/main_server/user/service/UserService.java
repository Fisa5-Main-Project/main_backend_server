package com.know_who_how.main_server.user.service;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    public UserResponseDto getUserInfo(User user) {
        return UserResponseDto.from(user);
    }
}
