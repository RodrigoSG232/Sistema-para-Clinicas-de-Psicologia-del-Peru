package com.clinica.psicologia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RecuperacionService {

    private final EmailService emailService;

    private final Map<String, CodigoEntry> codigos = new ConcurrentHashMap<>();

    public void generarYEnviarCodigo(String email) {
        String codigo = String.format("%06d", new Random().nextInt(999999));
        codigos.put(email, new CodigoEntry(codigo, LocalDateTime.now().plusMinutes(15)));
        emailService.enviarCodigoRecuperacion(email, codigo);
    }

    public boolean verificarCodigo(String email, String codigo) {
        CodigoEntry entry = codigos.get(email);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiracion)) {
            codigos.remove(email);
            return false;
        }
        return entry.codigo.equals(codigo);
    }

    public void eliminarCodigo(String email) {
        codigos.remove(email);
    }

    private static class CodigoEntry {
        String codigo;
        LocalDateTime expiracion;

        CodigoEntry(String codigo, LocalDateTime expiracion) {
            this.codigo = codigo;
            this.expiracion = expiracion;
        }
    }
}