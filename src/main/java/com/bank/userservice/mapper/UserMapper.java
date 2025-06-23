package com.bank.userservice.mapper;

import com.bank.userservice.dto.UserResponseDto;
import com.bank.userservice.model.User;
/**
 * Класс для преобразования между сущностью User и DTO.
 */
public class UserMapper {
    /**
     * Преобразует сущность User в UserResponseDto.
     *
     * @param user сущность пользователя
     * @return UserResponseDto с данными пользователя:
     *         <ul>
     *           <li>id - идентификатор</li>
     *           <li>username - имя пользователя</li>
     *           <li>email - адрес электронной почты</li>
     *           <li>createdAt - дата создания</li>
     *         </ul>
     */
    public static UserResponseDto usertoUserResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
