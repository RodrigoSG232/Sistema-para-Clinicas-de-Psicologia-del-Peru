package com.clinica.psicologia.controller;

import com.clinica.psicologia.dto.LoginRequest;
import com.clinica.psicologia.dto.LoginResponse;
import com.clinica.psicologia.entity.Usuario;
import com.clinica.psicologia.repository.UsuarioRepository;
import com.clinica.psicologia.security.JwtUtil;
import com.clinica.psicologia.service.RecuperacionService;

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
    private final RecuperacionService recuperacionService;


    // ─── LOGIN ────────────────────────────────────────────────────────────────

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

    // ─── PERFIL ───────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        return usuarioRepo.findByUsername(username)
            .map(u -> ResponseEntity.ok(Map.of(
                "id",             u.getId(),
                "username",       u.getUsername(),
                "nombreCompleto", u.getNombreCompleto(),
                "rol",            u.getRol().getNombre(),
                "email",          u.getEmail() != null ? u.getEmail() : ""
            )))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        return usuarioRepo.findByUsername(username).map(u -> {
            if (body.containsKey("nombreCompleto") && !body.get("nombreCompleto").isBlank())
                u.setNombreCompleto(body.get("nombreCompleto"));
            if (body.containsKey("email") && !body.get("email").isBlank())
                u.setEmail(body.get("email"));
            if (body.containsKey("password") && !body.get("password").isBlank())
                u.setPasswordHash(passwordEncoder.encode(body.get("password")));
            usuarioRepo.save(u);
            return ResponseEntity.ok(Map.of(
                "mensaje",        "Perfil actualizado correctamente",
                "nombreCompleto", u.getNombreCompleto(),
                "email",          u.getEmail() != null ? u.getEmail() : ""
            ));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // ─── RECUPERACIÓN DE CONTRASEÑA ───────────────────────────────────────────

    @PostMapping("/recuperar")
    public ResponseEntity<?> solicitarRecuperacion(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email requerido"));

        usuarioRepo.findByEmail(email).ifPresent(u ->
            recuperacionService.generarYEnviarCodigo(email)
        );

        return ResponseEntity.ok(Map.of(
            "mensaje", "Si el email existe, recibirás un código en tu correo"
        ));
    }

    // ─── Recuperar y verificar ────────────────────────────────────────────────
    @PostMapping("/recuperar/verificar")
    public ResponseEntity<?> verificarCodigo(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String codigo   = body.get("codigo");
        String password = body.get("password");

        if (email == null || codigo == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos"));

        if (!recuperacionService.verificarCodigo(email, codigo))
            return ResponseEntity.status(400).body(Map.of("error", "Código inválido o expirado"));

        return usuarioRepo.findByEmail(email).map(u -> {
            u.setPasswordHash(passwordEncoder.encode(password));
            usuarioRepo.save(u);
            recuperacionService.eliminarCodigo(email);
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── UTILIDADES ───────────────────────────────────────────────────────────

    @GetMapping("/hash")
    public String hash(@RequestParam String pass) {
        return passwordEncoder.encode(pass);
    }

    private String rutaPorRol(String rol) {
    return switch (rol) {
        case "RECEPCION"  -> "/recepcion";
        case "CAJA"       -> "/caja";
        case "PSICOLOGO"  -> "/psicologia";
        case "ADMIN"      -> "/admin";
        case "ANFITRIONA" -> "/anfitriona";  
        default           -> "/login";
    };
}
}