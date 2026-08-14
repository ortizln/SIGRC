import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { VersionService } from '@core/services/version.service';
import { AuthService } from '@core/services/auth.service';
import { Version } from '@shared/models/version.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-versiones',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './versiones.component.html',
  styleUrl: './versiones.component.css'
})
export class VersionesComponent implements OnInit {
  versiones: Version[] = [];
  puedeEditar = false;

  constructor(
    private versionService: VersionService,
    public auth: AuthService
  ) {}

  ngOnInit() {
    this.puedeEditar = this.auth.hasRole('ADMIN') || this.auth.hasRole('JEFE_TI');
    this.cargar();
  }

  cargar() { this.versionService.listar().subscribe(r => this.versiones = r); }

  estadoClass(estado: string): string {
    const map: Record<string, string> = {
      'PENDIENTE': 'bg-warning text-dark',
      'EN_DESARROLLO': 'bg-primary',
      'EN_PRUEBAS': 'bg-info text-dark',
      'DESPLEGADO': 'bg-success',
      'REVERTIDO': 'bg-danger'
    };
    return map[estado] || 'bg-secondary';
  }

  eliminar(v: Version) {
    Swal.fire({
      title: '¿Desactivar versión?',
      text: `La versión ${v.version} quedará inactiva pero sus datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.versionService.eliminar(v.idVersion).subscribe({
        next: () => this.cargar(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar la versión.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }
}
