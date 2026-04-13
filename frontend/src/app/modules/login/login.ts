import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

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

  private usuariosSimulados = [
    { user: 'recepcion', pass: '123', ruta: '/recepcion' },
    { user: 'caja', pass: '123', ruta: '/caja' },
    { user: 'psicologo', pass: '123', ruta: '/psicologia' },
  ];

  constructor(private router: Router) {}

  onLogin() {
    const usuarioEncontrado = this.usuariosSimulados.find(
      u => u.user === this.username && u.pass === this.password
    );

    if (usuarioEncontrado) {
      this.errorMessage = '';
      
      localStorage.setItem('usuarioActual', this.username);
      
      this.router.navigate([usuarioEncontrado.ruta]);
    } else {
      this.errorMessage = 'Usuario o contraseña incorrectos.';
    }
  }
}