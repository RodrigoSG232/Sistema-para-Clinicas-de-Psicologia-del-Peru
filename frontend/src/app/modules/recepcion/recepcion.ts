import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-recepcion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recepcion.html',
  styleUrl: './recepcion.css'
})
export class Recepcion {
  // 1. Cola de Tickets (Derivados por la Anfitriona)
  ticketsEnEspera = [
    { numero: 'A-012', estado: 'Esperando', tiempo: '5 min' },
    { numero: 'A-013', estado: 'Esperando', tiempo: '2 min' },
    { numero: 'A-014', estado: 'Recién llegado', tiempo: '0 min' }
  ];

  ticketActual = 'A-011'; // El paciente que está siendo atendido ahora

  // 2. Simulación de horarios disponibles
  horariosDisponibles = [
    { hora: '09:00 AM', medico: 'Dra. Ana López', especialidad: 'Psicología Clínica', estado: 'Libre' },
    { hora: '10:30 AM', medico: 'Dr. Carlos Mendoza', especialidad: 'Terapia de Pareja', estado: 'Ocupado' },
    { hora: '11:00 AM', medico: 'Dra. Ana López', especialidad: 'Psicología Clínica', estado: 'Libre' }
  ];

  // Simulación de búsqueda de paciente (para el HTML)
  pacienteEncontrado = true; // Cambia esto a false para ver cómo se vería si no tiene Historia Clínica
}