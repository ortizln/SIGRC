import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppMovilService } from '@core/services/app-movil.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-app-movil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app-movil.component.html',
  styleUrl: './app-movil.component.css'
})
export class AppMovilComponent implements OnInit {
  versiones: any[] = [];
  cargando = false;
  subiendo = false;

  formVersion = '';
  formDescripcion = '';
  formArchivo: File | null = null;

  constructor(private svc: AppMovilService) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.svc.listar().subscribe(r => this.versiones = r);
  }

  onArchivoSeleccionado(event: any) {
    this.formArchivo = event.target.files?.[0] || null;
  }

  subir() {
    if (!this.formVersion || !this.formArchivo) return;
    this.subiendo = true;
    this.svc.subir(this.formVersion, this.formDescripcion, this.formArchivo).subscribe({
      next: () => {
        this.subiendo = false;
        this.formVersion = '';
        this.formDescripcion = '';
        this.formArchivo = null;
        this.cargar();
        Swal.fire('Subido', 'APK subido correctamente.', 'success');
      },
      error: () => {
        this.subiendo = false;
        Swal.fire('Error', 'Error al subir el APK.', 'error');
      }
    });
  }

  descargar(item: any) {
    this.svc.descargar(item.idAppMovil).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = item.nombreArchivo;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  eliminar(item: any) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `La versión ${item.version} quedará inactiva.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.eliminar(item.idAppMovil).subscribe(() => this.cargar());
    });
  }

  formatearBytes(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }
}
