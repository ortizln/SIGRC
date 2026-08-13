import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { TIPOS_MOVIMIENTO } from '@shared/models/gestion-personal.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-movimientos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './movimientos.component.html',
  styleUrl: './movimientos.component.css'
})
export class MovimientosComponent implements OnInit {
  empleados: any[] = [];
  puestos: any[] = [];
  unidades: any[] = [];
  asignaciones: any[] = [];

  idEmpleado: number | null = null;
  estadoFiltro = '';
  movimientos: any[] = [];
  tipos = TIPOS_MOVIMIENTO;

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
    this.thSvc.getPuestos().subscribe(r => this.puestos = r);
    this.thSvc.getUnidades().subscribe(r => this.unidades = r);
    this.cargar();
  }

  cargar() {
    this.svc.listarMovimientos(this.idEmpleado, this.estadoFiltro || undefined).subscribe(r => this.movimientos = r);
  }

  onEmpleadoChange() {
    if (this.idEmpleado) {
      this.thSvc.getAsignaciones(this.idEmpleado).subscribe(r => this.asignaciones = r);
    } else {
      this.asignaciones = [];
    }
    this.cargar();
  }

  estadoBadge(estado: string): string {
    const map: Record<string, string> = {
      'BORRADOR': 'borde', 'PENDIENTE': 'info', 'APROBADA': 'ok', 'RECHAZADA': 'no', 'ANULADA': 'no'
    };
    return map[estado] || 'info';
  }

  nuevo() {
    this.editandoId = null;
    this.form = { tipoMovimiento: 'TRASLADO', fechaSolicitud: new Date().toISOString().split('T')[0] };
    this.formVisible = true;
  }

  editar(m: any) {
    this.editandoId = m.idMovimiento;
    this.form = {
      idEmpleado: m.idEmpleado,
      tipoMovimiento: m.tipoMovimiento,
      idAsignacionOrigen: m.idAsignacionOrigen,
      idPuestoDestino: m.idPuestoDestino,
      idUnidadDestino: m.idUnidadDestino,
      fechaSolicitud: m.fechaSolicitud,
      fechaDesde: m.fechaDesde,
      fechaHasta: m.fechaHasta,
      motivo: m.motivo,
      documentoRespaldoId: m.documentoRespaldoId
    };
    this.idEmpleado = m.idEmpleado;
    this.onEmpleadoChange();
    this.formVisible = true;
  }

  cancelar() {
    this.formVisible = false;
    this.editandoId = null;
    this.form = {};
  }

  guardar() {
    if (!this.form.idEmpleado || !this.form.tipoMovimiento) {
      Swal.fire('Faltan datos', 'Empleado y tipo de movimiento son obligatorios.', 'warning');
      return;
    }
    this.cargando = true;
    const obs = this.editandoId
      ? this.svc.actualizarMovimiento(this.editandoId, this.form)
      : this.svc.crearMovimiento(this.form);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        this.cancelar();
        Swal.fire('Guardado', 'Movimiento registrado correctamente.', 'success');
        this.cargar();
      },
      error: (e) => {
        this.cargando = false;
        Swal.fire('Error', e.error?.error || 'No se pudo guardar el movimiento.', 'error');
      }
    });
  }

  enviar(m: any) {
    this.svc.enviarMovimiento(m.idMovimiento).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Enviado', 'Movimiento enviado a aprobación.', 'success');
    });
  }

  aprobar(m: any) {
    this.svc.aprobarMovimiento(m.idMovimiento).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Aprobado', 'Movimiento aprobado. Ahora puede ejecutarse.', 'success');
    });
  }

  ejecutar(m: any) {
    Swal.fire({
      title: '¿Ejecutar movimiento?',
      text: 'Se registrará la nueva asignación de puesto y se cerrará la anterior (se conserva historial).',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, ejecutar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.svc.ejecutarMovimiento(m.idMovimiento).subscribe(r => {
          this.remplazar(r);
          Swal.fire('Ejecutado', 'Asignación de puesto registrada.', 'success');
        });
      }
    });
  }

  rechazar(m: any) {
    this.svc.rechazarMovimiento(m.idMovimiento).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Rechazado', 'Movimiento rechazado.', 'info');
    });
  }

  anular(m: any) {
    this.svc.anularMovimiento(m.idMovimiento).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Anulado', 'Movimiento anulado.', 'info');
    });
  }

  private remplazar(r: any) {
    const i = this.movimientos.findIndex(x => x.idMovimiento === r.idMovimiento);
    if (i >= 0) this.movimientos[i] = r;
  }
}
