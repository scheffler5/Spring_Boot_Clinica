package com.learn.projeto_learn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;



    @Value("${spring.mail.from:${spring.mail.username:}}")
    private String from;

    private static final String FALLBACK_FROM = "noreply@clinica.local";

    public void sendEmailText(String para, String assunto, String mensagem) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from.isBlank() ? FALLBACK_FROM : from);
            msg.setTo(para);
            msg.setSubject(assunto);
            msg.setText(mensagem);
            mailSender.send(msg);
            System.out.println("E-mail enviado para: " + para + " | From: " + (from.isBlank() ? FALLBACK_FROM : from));
        } catch (Exception e) {
            System.out.println("ERRO AO ENVIAR E-MAIL para " + para + ": " + e.getMessage());
        }
    }
}
