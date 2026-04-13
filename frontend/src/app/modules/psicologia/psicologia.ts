import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-psicologia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './psicologia.html',
  styleUrl: './psicologia.css'
})
export class Psicologia {
  pacientesAgenda = [
    { nombre: 'María González', estado: 'Esperando', hc: 'HC-089', faseActual: 0 },
    { nombre: 'Carlos Ruiz', estado: 'Por llegar', hc: 'HC-142', faseActual: 2 }
  ];

  pacienteActivo: any = null;
  notasSesion: string = '';
  tareasAsignadas: string = '';

  fasesUNIR = [
    { id: 0, titulo: '1. Evaluación y Orientación', desc: 'Semanas iniciales: Recopilación de información, rapport y evaluación.' },
    { id: 1, titulo: '2. Explicación de Hipótesis', desc: 'Sesión clave: Presentación del análisis funcional y fijación de metas.' },
    { id: 2, titulo: '3. Tratamiento e Intervención', desc: 'Fase central (Meses): Aplicación de técnicas terapéuticas activas.' },
    { id: 3, titulo: '4. Terminación y Seguimiento', desc: 'Fase final: Consolidación de logros y prevención de recaídas a largo plazo.' }
  ];

  atenderPaciente(paciente: any) {
    this.pacienteActivo = paciente;
    this.notasSesion = '';
    this.tareasAsignadas = '';
  }

  finalizarSesion() {
    alert('Notas de evolución de HOY guardadas en la historia clínica. La fase global del paciente se ha actualizado.');
    this.pacienteActivo = null;
  }
}