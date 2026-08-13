import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { TIPOS_AUSENCIA } from '@shared/models/gestion-personal.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-vacaciones-permisos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vacaciones-permisos.component.html',
  styleUrl: './vacaciones-permisos.component.css'
})
export class VacacionesPermisosComponent implements OnInit {
  empleados: any[] = [];
  idEmpleado: number | null = null;
  estadoFiltro = '';
  solicitudes: any[] = [];
  tipos = TIPOS_AUSENCIA;
  usuarioLogueado: any;

  form: any = {};
  formVisible = false;
  cargando = false;

  constructor(
    private svc: GestionPersonalService,
    private thSvc: TalentoHumanoService,
    public auth: AuthService
  ) {
    this.usuarioLogueado = this.auth.getUsuario();
  }

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

  nuevo() {
    this.form = {
      idEmpleado: this.soloMiEmpleado ? this.idEmpleado : null,
      tipo: 'PERMISO',
      fechaDesde: new Date().toISOString().split('T')[0],
      fechaHasta: new Date().toISOString().split('T')[0],
      dias: 1
    };
    this.formVisible = true;
  }

  get soloMiEmpleado(): boolean {
    return !!this.usuarioLogueado?.idEmpleado && !this.isAdmin;
  }

  cancelar() {
    this.formVisible = false;
    this.form = {};
  }

  guardar() {
    if (!this.form.idEmpleado || !this.form.tipo || !this.form.fechaDesde || !this.form.fechaHasta) {
      Swal.fire('Faltan datos', 'Empleado, tipo y fechas son obligatorios.', 'warning');
      return;
    }
    this.cargando = true;
    this.svc.crearAusencia(this.form).subscribe({
      next: () => {
        this.cargando = false;
        this.cancelar();
        Swal.fire('Solicitada', 'Solicitud enviada al jefe inmediato para aprobación.', 'success');
        this.cargar();
      },
      error: (e) => {
        this.cargando = false;
        Swal.fire('Error', e.error?.error || 'No se pudo crear la solicitud.', 'error');
      }
    });
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