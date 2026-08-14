import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-puestos',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './puestos.component.html',
  styleUrl: './puestos.component.css'
})
export class PuestosComponent implements OnInit {
  puestos: any[] = [];
  menuAbierto: number | null = null;

  constructor(private svc: TalentoHumanoService, private auth: AuthService) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.cargarPuestos();
  }

  cargarPuestos() { this.svc.getPuestos().subscribe(r => this.puestos = r); }

  toggleMenu(id: number) {
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  desactivar(p: any) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `${p.nombre} quedará inactivo pero sus datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      this.svc.eliminarPuesto(p.idPuesto).subscribe({
        next: () => this.cargarPuestos(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar el puesto.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }
}