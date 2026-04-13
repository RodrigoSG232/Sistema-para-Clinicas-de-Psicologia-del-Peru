import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-caja',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './caja.html',
  styleUrl: './caja.css'
})
export class Caja{
  metodoPago: string = 'tarjeta';
  deudaSeleccionada: any = null;

  seleccionarMetodo(metodo: string) {
    this.metodoPago = metodo;
  }

  cobrarDeuda() {
    this.deudaSeleccionada = {
      paciente: 'María González Pérez',
      concepto: 'Gastos de cita',
      monto: 'S/ 80.00'
    };
  }

  cancelarCobro() {
    this.deudaSeleccionada = null;
  }
}