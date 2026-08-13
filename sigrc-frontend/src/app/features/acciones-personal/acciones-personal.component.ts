import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { TIPOS_ACCION_PERSONAL } from '@shared/models/gestion-personal.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-acciones-personal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './acciones-personal.component.html',
  styleUrl: './acciones-personal.component.css'
})
export class AccionesPersonalComponent implements OnInit {
  empleados: any[] = [];
  idEmpleado: number | null = null;
  estadoFiltro = '';
  acciones: any[] = [];
  tipos = TIPOS_ACCION_PERSONAL;

  form: any = {};
  formVisible = false;
  editandoId: number | null = null;
  cargando = false;

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
    this.svc.listarAcciones(this.idEmpleado, this.estadoFiltro || undefined).subscribe(r => this.acciones = r);
  }

  estadoBadge(estado: string): string {
    const map: Record<string, string> = {
      'BORRADOR': 'borde', 'EN_REVISION': 'info', 'APROBADA': 'ok', 'RECHAZADA': 'no', 'ANULADA': 'no'
    };
    return map[estado] || 'info';
  }

  nuevo() {
    this.editandoId = null;
    this.form = { tipo: 'NOMBRAMIENTO', fechaEmision: new Date().toISOString().split('T')[0] };
    this.formVisible = true;
  }

  editar(a: any) {
    this.editandoId = a.idAccion;
    this.form = {
      idEmpleado: a.idEmpleado,
      tipo: a.tipo,
      fechaEmision: a.fechaEmision,
      fechaVigenciaDesde: a.fechaVigenciaDesde,
      fechaVigenciaHasta: a.fechaVigenciaHasta,
      motivo: a.motivo,
      situacionActual: a.situacionActual,
      situacionPropuesta: a.situacionPropuesta,
      documentoId: a.documentoId
    };
    this.formVisible = true;
  }

  cancelar() {
    this.formVisible = false;
    this.editandoId = null;
    this.form = {};
  }

  guardar() {
    if (!this.form.idEmpleado || !this.form.tipo) {
      Swal.fire('Faltan datos', 'Empleado y tipo son obligatorios.', 'warning');
      return;
    }
    this.cargando = true;
    const obs = this.editandoId
      ? this.svc.actualizarAccion(this.editandoId, this.form)
      : this.svc.crearAccion(this.form);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        this.cancelar();
        Swal.fire('Guardado', 'Acción de personal registrada correctamente.', 'success');
        this.cargar();
      },
      error: (e) => {
        this.cargando = false;
        Swal.fire('Error', e.error?.error || 'No se pudo guardar la acción.', 'error');
      }
    });
  }

  enviarRevision(a: any) {
    this.svc.enviarRevisionAccion(a.idAccion).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Enviada', 'Acción enviada a revisión.', 'success');
    });
  }

  aprobar(a: any) {
    this.svc.aprobarAccion(a.idAccion).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Aprobada', 'Acción de personal aprobada.', 'success');
    });
  }

  rechazar(a: any) {
    this.svc.rechazarAccion(a.idAccion).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Rechazada', 'Acción rechazada.', 'info');
    });
  }

  anular(a: any) {
    this.svc.anularAccion(a.idAccion).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Anulada', 'Acción anulada.', 'info');
    });
  }

  private remplazar(r: any) {
    const i = this.acciones.findIndex(x => x.idAccion === r.idAccion);
    if (i >= 0) this.acciones[i] = r;
  }
}