import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-vacaciones-permisos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './vacaciones-permisos.component.html',
  styleUrl: './vacaciones-permisos.component.css'
})
export class VacacionesPermisosComponent implements OnInit {
  empleados: any[] = [];
  idEmpleado: number | null = null;
  estadoFiltro = '';
  solicitudes: any[] = [];

  constructor(
    private svc: GestionPersonalService,
    private thSvc: TalentoHumanoService,
    public auth: AuthService
  ) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.thSvc.getEmpleados().subscribe(r => this.empleados = r);
    this.cargar();
  }

  cargar() {
    this.svc.listarAusencias(this.idEmpleado, this.estadoFiltro || undefined).subscribe(r => this.solicitudes = r);
  }

  estadoBadge(estado: string): string {
    const map: Record<string, string> = {
      'PENDIENTE_JEFE': 'info', 'PENDIENTE_TH': 'info', 'APROBADA': 'ok', 'RECHAZADA': 'no', 'ANULADA': 'no'
    };
    return map[estado] || 'info';
  }

  aprobarJefe(s: any) {
    this.svc.aprobarJefeAusencia(s.idSolicitud).subscribe({
      next: r => {
        this.remplazar(r);
        Swal.fire('Aprobada', 'Aprobada por el jefe. Pasa a Talento Humano.', 'success');
      },
      error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo aprobar.', 'error')
    });
  }

  aprobarTh(s: any) {
    this.svc.aprobarThAusencia(s.idSolicitud).subscribe({
      next: r => {
        this.remplazar(r);
        Swal.fire('Aprobada', 'Solicitud aprobada por Talento Humano.', 'success');
      },
      error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo aprobar.', 'error')
    });
  }

  rechazar(s: any) {
    this.svc.rechazarAusencia(s.idSolicitud).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Rechazada', 'Solicitud rechazada.', 'info');
    });
  }

  anular(s: any) {
    this.svc.anularAusencia(s.idSolicitud).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Anulada', 'Solicitud anulada.', 'info');
    });
  }

  private remplazar(r: any) {
    const i = this.solicitudes.findIndex(x => x.idSolicitud === r.idSolicitud);
    if (i >= 0) this.solicitudes[i] = r;
  }
}