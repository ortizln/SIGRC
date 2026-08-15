import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { AppMovilService } from '@core/services/app-movil.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-app-movil',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './app-movil.component.html',
  styleUrl: './app-movil.component.css'
})
export class AppMovilComponent implements OnInit {
  versiones: any[] = [];

  constructor(private svc: AppMovilService, private auth: AuthService) {}

  get puedeCrear(): boolean { return this.auth.canModulo('APP_MOVIL', 'ESCRITURA'); }

  ngOnInit() { this.cargar(); }

  cargar() {
    this.svc.listar().subscribe(r => this.versiones = r);
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
