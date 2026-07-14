package com.clinica.psicologia.controller;

import com.clinica.psicologia.entity.Rol;
import com.clinica.psicologia.entity.Usuario;
import com.clinica.psicologia.repository.RolRepository;
import com.clinica.psicologia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_LETTER_PATTERN = Pattern.compile("[A-Za-z]");
    private static final Pattern PASSWORD_NUMBER_PATTERN = Pattern.compile("\\d");

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> resumen() {
        List<Usuario> usuarios = usuarioRepo.findAll();
        long activos = usuarios.stream().filter(u -> Boolean.TRUE.equals(u.getActivo())).count();
        long inactivos = usuarios.size() - activos;

        return ResponseEntity.ok(Map.of(
                "usuarios", usuarios.size(),
                "activos", activos,
                "inactivos", inactivos,
                "roles", rolRepo.count()
        ));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RolDTO>> listarRoles() {
        List<RolDTO> roles = rolRepo.findAll(Sort.by("nombre")).stream()
                .map(r -> new RolDTO(r.getId(), r.getNombre(), r.getDescripcion()))
                .toList();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios(@RequestParam(required = false) String q) {
        String filtro = q == null ? "" : q.trim().toLowerCase();
        List<UsuarioDTO> usuarios = usuarioRepo.findAll(Sort.by(Sort.Direction.DESC, "creadoEn")).stream()
                .filter(u -> filtro.isBlank()
                        || u.getUsername().toLowerCase().contains(filtro)
                        || u.getNombreCompleto().toLowerCase().contains(filtro)
                        || u.getEmail().toLowerCase().contains(filtro)
                        || u.getRol().getNombre().toLowerCase().contains(filtro))
                .map(this::toUsuarioDTO)
                .toList();

        return ResponseEntity.ok(usuarios);
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioRequest request) {
        String username = normalizarRequerido(request.username(), "username");
        String nombreCompleto = normalizarRequerido(request.nombreCompleto(), "nombreCompleto");
        String email = normalizarRequerido(request.email(), "email").toLowerCase();
        String password = normalizarRequerido(request.password(), "password");
        validarEmail(email);
        validarPassword(password);

        if (usuarioRepo.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un usuario con ese username"));
        }
        if (usuarioRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un usuario con ese email"));
        }

        Rol rol = obtenerRol(request.rolId());
        Usuario usuario = Usuario.builder()
                .username(username)
                .nombreCompleto(nombreCompleto)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .rol(rol)
                .activo(request.activo() == null || request.activo())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(toUsuarioDTO(usuarioRepo.save(usuario)));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Integer id,
            @RequestBody UsuarioRequest request) {
        return usuarioRepo.findById(id)
                .<ResponseEntity<?>>map(usuario -> {
                    String username = normalizarRequerido(request.username(), "username");
                    String nombreCompleto = normalizarRequerido(request.nombreCompleto(), "nombreCompleto");
                    String email = normalizarRequerido(request.email(), "email").toLowerCase();
                    validarEmail(email);

                    usuarioRepo.findByUsername(username)
                            .filter(existente -> !existente.getId().equals(id))
                            .ifPresent(existente -> {
                                throw new IllegalStateException("Ya existe un usuario con ese username");
                            });
                    usuarioRepo.findByEmail(email)
                            .filter(existente -> !existente.getId().equals(id))
                            .ifPresent(existente -> {
                                throw new IllegalStateException("Ya existe un usuario con ese email");
                            });

                    Rol rol = obtenerRol(request.rolId());
                    boolean nuevoActivo = request.activo() == null || request.activo();
                    validarNoDesactivarUltimoAdmin(usuario, rol, nuevoActivo);

                    usuario.setUsername(username);
                    usuario.setNombreCompleto(nombreCompleto);
                    usuario.setEmail(email);
                    usuario.setRol(rol);
                    usuario.setActivo(nuevoActivo);

                    if (request.password() != null && !request.password().isBlank()) {
                        String password = request.password().trim();
                        validarPassword(password);
                        usuario.setPasswordHash(passwordEncoder.encode(password));
                    }

                    return ResponseEntity.ok(toUsuarioDTO(usuarioRepo.save(usuario)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/usuarios/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        Boolean activo = body.get("activo");
        if (activo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo activo es requerido"));
        }

        return usuarioRepo.findById(id)
                .<ResponseEntity<?>>map(usuario -> {
                    validarNoDesactivarUltimoAdmin(usuario, usuario.getRol(), activo);
                    usuario.setActivo(activo);
                    return ResponseEntity.ok(toUsuarioDTO(usuarioRepo.save(usuario)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> manejarErrores(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    private Rol obtenerRol(Integer rolId) {
        if (rolId == null) {
            throw new IllegalArgumentException("El rol es requerido");
        }
        return rolRepo.findById(rolId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));
    }

    private String normalizarRequerido(String valor, String campo) {
        if (valor == null || valor.trim().isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es requerido");
        }
        return valor.trim();
    }

    private void validarEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Ingrese un correo electrónico válido");
        }
    }

    private void validarPassword(String password) {
        if (password.length() < 8
                || !PASSWORD_LETTER_PATTERN.matcher(password).find()
                || !PASSWORD_NUMBER_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres, una letra y un número");
        }
    }

    private void validarNoDesactivarUltimoAdmin(Usuario usuario, Rol rolNuevo, boolean activoNuevo) {
        boolean eraAdminActivo = Boolean.TRUE.equals(usuario.getActivo())
                && "ADMIN".equals(usuario.getRol().getNombre());
        boolean seguiraAdminActivo = activoNuevo && "ADMIN".equals(rolNuevo.getNombre());

        if (eraAdminActivo && !seguiraAdminActivo) {
            long adminsActivos = usuarioRepo.findAll().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                    .filter(u -> "ADMIN".equals(u.getRol().getNombre()))
                    .count();

            if (adminsActivos <= 1) {
                throw new IllegalStateException("Debe existir al menos un administrador activo");
            }
        }
    }

    private UsuarioDTO toUsuarioDTO(Usuario u) {
        return new UsuarioDTO(
                u.getId(),
                u.getUsername(),
                u.getNombreCompleto(),
                u.getEmail(),
                u.getRol().getId(),
                u.getRol().getNombre(),
                u.getActivo(),
                u.getCreadoEn() != null ? u.getCreadoEn().toString() : null
        );
    }

    public record RolDTO(Integer id, String nombre, String descripcion) {}

    public record UsuarioDTO(
            Integer id,
            String username,
            String nombreCompleto,
            String email,
            Integer rolId,
            String rol,
            Boolean activo,
            String creadoEn) {}

    public record UsuarioRequest(
            String username,
            String nombreCompleto,
            String email,
            Integer rolId,
            Boolean activo,
            String password) {}
}
