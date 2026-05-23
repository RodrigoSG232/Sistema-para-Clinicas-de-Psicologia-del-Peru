package com.clinica.psicologia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCodigoRecuperacion(String destinatario, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("clinica.cpp.noreply@gmail.com");
        message.setTo(destinatario);
        message.setSubject("Recuperacion de contrasenia - CPP");
        message.setText(
            "Hola,\n\n" +
            "Tu codigo de verificacion es: " + codigo + "\n\n" +
            "Expira en 15 minutos.\n\n" +
            "Clinicas Psicologia del Peru"
        );
        mailSender.send(message);
    }
}