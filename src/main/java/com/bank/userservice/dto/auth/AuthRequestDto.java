package com.bank.userservice.dto.auth;

import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import lombok.Data;

@Data
public class AuthRequestDto {
    private Long rqid;
    private LoginDto loginData;
    private RegistrationDto registrationData;
}


