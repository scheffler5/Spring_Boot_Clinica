package com.learn.projeto_learn.controller.captcha;

import com.learn.projeto_learn.dto.captcha.CaptchaResponseDTO;
import com.learn.projeto_learn.service.captcha.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    @GetMapping("/generate")
    public ResponseEntity<CaptchaResponseDTO> generate() {
        return ResponseEntity.ok(captchaService.generate());
    }
}
