import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  username = '';
  password = '';
  errorMessage = '';
  loading = false;

  constructor(private router: Router, private authService: AuthService) {}

  onLogin() {
    if (!this.username || !this.password) {
      this.errorMessage = 'Complete usuario y contraseña.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';

    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: (res) => {
        localStorage.setItem('token', res.token);
        localStorage.setItem('usuarioActual', res.nombreCompleto || res.username);
        localStorage.setItem('rol', res.rol);
        this.loading = false;
        this.router.navigate([res.ruta]);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.status === 401
          ? 'Usuario o contraseña incorrectos.'
          : 'Error de conexión con el servidor.';
      }
    });
  }
}
