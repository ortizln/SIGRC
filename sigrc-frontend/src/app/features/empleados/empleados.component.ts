import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-empleados',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './empleados.component.html',
  styleUrl: './empleados.component.css'
})
export class EmpleadosComponent implements OnInit {
  empleados: any[] = [];
  menuAbierto: number | null = null;

  ver = false;
  expediente: any = null;
  asignacionActual: any = null;
  asignaciones: any[] = [];
  jefe: any = null;
  movimientos: any[] = [];
  acciones: any[] = [];
  ausencias: any[] = [];

  constructor(private svc: TalentoHumanoService,
              private gp: GestionPersonalService,
              private auth: AuthService,
              private router: Router) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }
  get puedeVerExpediente(): boolean {
    return this.isAdmin || this.auth.canModulo('TALENTO_HUMANO', 'LECTURA');
  }
  get columnas(): number { return this.puedeVerExpediente ? 6 : 5; }

  ngOnInit() { this.cargarEmpleados(); }

  cargarEmpleados() { this.svc.getEmpleados().subscribe(r => this.empleados = r); }

  toggleMenu(id: number) {
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  desactivar(e: any) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `${e.nombreCompleto} quedará inactivo pero su expediente se conserva.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      this.svc.eliminarEmpleado(e.idEmpleado).subscribe({
        next: () => this.cargarEmpleados(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar el empleado.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }

  // ---------- Expediente único (§8) ----------

  verExpediente(e: any) {
    this.ver = true;
    this.menuAbierto = null;
    const id = e.idEmpleado;
    forkJoin({
      expediente: this.svc.getEmpleadoExpediente(id),
      asignacionActual: this.svc.getAsignacionActual(id),
      asignaciones: this.svc.getAsignaciones(id),
      jefe: this.svc.getJefeInmediato(id),
      movimientos: this.gp.listarMovimientos(id),
      acciones: this.gp.listarAcciones(id),
      ausencias: this.gp.listarAusencias(id)
    }).subscribe({
      next: r => {
        this.expediente = r.expediente;
        this.asignacionActual = r.asignacionActual;
        this.asignaciones = r.asignaciones || [];
        this.jefe = r.jefe;
        this.movimientos = r.movimientos || [];
        this.acciones = r.acciones || [];
        this.ausencias = r.ausencias || [];
      },
      error: (err) => {
        this.ver = false;
        const msg = err.error?.error || 'No se pudo cargar el expediente.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  cerrarExpediente() {
    this.ver = false;
    this.expediente = null;
    this.asignacionActual = null;
    this.asignaciones = [];
    this.jefe = null;
    this.movimientos = [];
    this.acciones = [];
    this.ausencias = [];
  }

  editarDesdeExpediente() {
    if (this.expediente?.idEmpleado) {
      this.router.navigate(['/talento-humano/empleados/editar', this.expediente.idEmpleado]);
    }
  }

  descargarArchivo(d: any) {
    const idEmpleado = this.expediente?.idEmpleado;
    if (!idEmpleado || !d.idEmpleadoDocumento) return;
    this.svc.descargarArchivoExpediente(idEmpleado, d.idEmpleadoDocumento).subscribe({
      next: blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = d.nombreArchivo || `documento-${d.idEmpleadoDocumento}`;
        a.click();
        URL.revokeObjectURL(a.href);
      },
      error: (err) => {
        const msg = err.error?.error || 'No se pudo descargar el archivo.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  tieneDatos(coleccion: any[] | undefined): boolean {
    return !!coleccion && coleccion.length > 0;
  }

  badgeEstado(estado?: string): string {
    const e = (estado || '').toUpperCase();
    if (['ACTIVA', 'APROBADA'].includes(e)) return 'badge-ok';
    if (['PENDIENTE', 'EN_REVISION', 'PENDIENTE_JEFE', 'PENDIENTE_TH', 'BORRADOR'].includes(e)) return 'badge-warn';
    if (['RECHAZADA', 'ANULADA'].includes(e)) return 'badge-danger';
    return 'badge-neutral';
  }
}