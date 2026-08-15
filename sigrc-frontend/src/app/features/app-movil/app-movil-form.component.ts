import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AppMovilService } from '@core/services/app-movil.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-app-movil-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './app-movil-form.component.html',
  styleUrl: './app-movil-form.component.css'
})
export class AppMovilFormComponent {
  formVersion = '';
  formDescripcion = '';
  formArchivo: File | null = null;
  intentado = false;
  subiendo = false;

  constructor(private svc: AppMovilService, private router: Router) {}

  volver() {
    this.router.navigate(['/app-movil']);
  }

  onArchivoSeleccionado(event: any) {
    this.formArchivo = event.target.files?.[0] || null;
  }

  subir() {
    this.intentado = true;
    if (!this.formVersion || !this.formArchivo) return;
    this.subiendo = true;
    this.svc.subir(this.formVersion, this.formDescripcion, this.formArchivo).subscribe({
      next: () => {
        this.subiendo = false;
        Swal.fire('Subido', 'APK subido correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.subiendo = false;
        const msg = err.error?.error || 'Error al subir el APK.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}