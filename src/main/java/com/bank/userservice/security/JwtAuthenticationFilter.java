//package com.bank.userservice.security;
//
//
//import com.bank.userservice.dto.UserDto;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import java.io.IOException;
//import java.util.Collections;
//
//@Slf4j
//public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
//
//    private final JwtUtils jwtUtils;
//
//    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
//        super(authenticationManager);
//        this.jwtUtils = jwtUtils;
//        setFilterProcessesUrl("/api/auth/login");
//    }
//
//    @Override
//    public Authentication attemptAuthentication(HttpServletRequest request,
//                                                HttpServletResponse response) throws AuthenticationException {
//        try {
//            UserDto credentials = new ObjectMapper()
//                    .readValue(request.getInputStream(), UserDto.class);
//
//            return getAuthenticationManager().authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            credentials.getUsername(),
//                            credentials.getPassword(),
//                            Collections.emptyList()));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    protected void successfulAuthentication(HttpServletRequest request,
//                                            HttpServletResponse response,
//                                            FilterChain chain,
//                                            Authentication authResult) {
//        String token = jwtUtils.generateJwtToken(authResult);
//        response.addHeader("Authorization", "Bearer " + token);
//    }
//}