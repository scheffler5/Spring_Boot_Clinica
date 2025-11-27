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

    @Value("${spring.mail.username}")
    private String remetente;

    public void sendEmailText(String para, String assunto, String mensagem) {
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom(remetente);
            simpleMailMessage.setTo(para);
            simpleMailMessage.setSubject(assunto);
            simpleMailMessage.setText(mensagem);

            mailSender.send(simpleMailMessage);
        } catch (Exception e) {
            System.out.println("ERRO AO ENVIAR E-MAIL: " + e.getMessage());
        }
    }
}