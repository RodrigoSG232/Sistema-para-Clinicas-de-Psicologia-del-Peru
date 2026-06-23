package com.clinica.psicologia.controller;

import com.clinica.psicologia.dto.LoginRequest;
import com.clinica.psicologia.dto.LoginResponse;
import com.clinica.psicologia.entity.Rol;
import com.clinica.psicologia.entity.Usuario;
import com.clinica.psicologia.repository.UsuarioRepository;
import com.clinica.psicologia.security.JwtUtil;
import com.clinica.psicologia.service.RecuperacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RecuperacionService recuperacionService;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginConUsuarioRecepcionActivoDevuelveTokenYRutaDelRol() {
        LoginRequest request = new LoginRequest();
        request.setUsername("recepcion");
        request.setPassword("123");

        Usuario usuario = Usuario.builder()
                .username("recepcion")
                .passwordHash("hash")
                .nombreCompleto("Usuario Recepcion")
                .rol(Rol.builder().nombre("RECEPCION").build())
                .activo(true)
                .email("recepcion@cpp.test")
                .build();

        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123", "hash")).thenReturn(true);
        when(jwtUtil.generateToken("recepcion", "RECEPCION")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("recepcion", "RECEPCION")).thenReturn("refresh-token");

        ResponseEntity<?> response = controller.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(LoginResponse.class);

        LoginResponse body = (LoginResponse) response.getBody();
        assertThat(body.getToken()).isEqualTo("access-token");
        assertThat(body.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(body.getRuta()).isEqualTo("/recepcion");
    }

    @Test
    void loginConPasswordIncorrectoRespondeNoAutorizadoSinGenerarTokens() {
        LoginRequest request = new LoginRequest();
        request.setUsername("recepcion");
        request.setPassword("incorrecta");

        Usuario usuario = Usuario.builder()
                .username("recepcion")
                .passwordHash("hash")
                .rol(Rol.builder().nombre("RECEPCION").build())
                .activo(true)
                .build();

        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        ResponseEntity<?> response = controller.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verify(jwtUtil, never()).generateToken("recepcion", "RECEPCION");
        verify(jwtUtil, never()).generateRefreshToken("recepcion", "RECEPCION");
    }
}
