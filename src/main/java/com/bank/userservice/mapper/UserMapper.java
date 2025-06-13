package com.bank.userservice.mapper;

import com.bank.userservice.dto.UserResponseDto;
import com.bank.userservice.model.User;

public class UserMapper {
    public static UserResponseDto usertoUserResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
