import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-manual-funciones-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './manual-funciones-form.component.html',
  styleUrl: './manual-funciones-form.component.css'
})
export class ManualFuncionesFormComponent {
  versForm: any = {
    nombre: 'Manual Orgánico Funcional',
    version: '',
    fechaAprobacion: '',
    fechaVigencia: '',
    documentoId: null,
    observaciones: ''
  };
  guardando = false;

  constructor(private svc: TalentoHumanoService, private router: Router) {}

  volver() {
    this.router.navigate(['/talento-humano/manual-funciones']);
  }

  guardar() {
    if (!this.versForm.nombre || !this.versForm.version) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Nombre y versión son obligatorios.' });
      return;
    }
    this.guardando = true;
    this.svc.crearVersionManual({
      nombre: this.versForm.nombre,
      version: this.versForm.version,
      fechaAprobacion: this.versForm.fechaAprobacion || null,
      fechaVigencia: this.versForm.fechaVigencia || null,
      documentoId: this.versForm.documentoId || null,
      observaciones: this.versForm.observaciones || null
    }).subscribe({
      next: () => {
        this.guardando = false;
        Swal.fire('Guardado', 'Versión creada en estado BORRADOR.', 'success').then(() => this.volver());
      },
      error: (e) => {
        this.guardando = false;
        Swal.fire('Error', e.error?.error || 'No se pudo crear la versión.', 'error');
      }
    });
  }
}