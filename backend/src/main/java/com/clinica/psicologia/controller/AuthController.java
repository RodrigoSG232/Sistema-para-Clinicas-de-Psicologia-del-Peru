package com.clinica.psicologia.controller;

import com.clinica.psicologia.dto.LoginRequest;
import com.clinica.psicologia.dto.LoginResponse;
import com.clinica.psicologia.entity.Usuario;
import com.clinica.psicologia.repository.UsuarioRepository;
import com.clinica.psicologia.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        var userOpt = usuarioRepo.findByUsername(req.getUsername());

        if (userOpt.isEmpty()) {
            System.out.println("❌ Usuario no encontrado");
            return ResponseEntity.status(401).body("Usuario no encontrado");
        }

        var u = userOpt.get();

        System.out.println("✅ Usuario encontrado: " + u.getUsername());
        System.out.println("Activo: " + u.getActivo());

        if (!Boolean.TRUE.equals(u.getActivo())) {
            System.out.println("❌ Usuario inactivo");
            return ResponseEntity.status(401).body("Usuario inactivo");
        }

        System.out.println("Password RAW: " + req.getPassword());
        System.out.println("Password HASH: " + u.getPasswordHash());

        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            System.out.println("❌ Password incorrecto");
            return ResponseEntity.status(401).body("Password incorrecto");
        }

        String token = jwtUtil.generateToken(u.getUsername(), u.getRol().getNombre());
        String ruta = rutaPorRol(u.getRol().getNombre());

        return ResponseEntity.ok(new LoginResponse(
            token,
            u.getUsername(),
            u.getNombreCompleto(),
            u.getRol().getNombre(),
            ruta
        ));
    }

    private String rutaPorRol(String rol) {
        return switch (rol) {
            case "RECEPCION" -> "/recepcion";
            case "CAJA"      -> "/caja";
            case "PSICOLOGO" -> "/psicologia";
            default          -> "/";
        };
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        return usuarioRepo.findByUsername(username)
            .map(u -> ResponseEntity.ok(Map.of(
                "username", u.getUsername(),
                "nombreCompleto", u.getNombreCompleto(),
                "rol", u.getRol().getNombre()
            )))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
