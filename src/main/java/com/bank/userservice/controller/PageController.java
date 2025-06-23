package com.bank.userservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
/**
 Контроллер для отображения шаблонных страниц
 */
@Controller
public class PageController {
    /**
     * Отображает страницу успешного подтверждения email.
     *
     * @return имя шаблона страницы "email-verified"
     */
    @GetMapping("/email-verified")
    public String emailVerified() {
        return "email-verified";
    }
}