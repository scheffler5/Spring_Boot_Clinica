package com.learn.projeto_learn.controller.captcha;

import com.learn.projeto_learn.dto.captcha.CaptchaResponseDTO;
import com.learn.projeto_learn.service.captcha.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/captcha")
@Tag(name = "CAPTCHA", description = "Geração de desafios CAPTCHA para login e cadastro")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    @GetMapping("/generate")
    @Operation(summary = "Gera um novo desafio CAPTCHA", security = {})
    public ResponseEntity<CaptchaResponseDTO> generate() {
        return ResponseEntity.ok(captchaService.generate());
    }
}
