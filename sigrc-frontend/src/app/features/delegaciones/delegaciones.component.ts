import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { TIPOS_DELEGACION, ALCANCES_DELEGACION } from '@shared/models/delegaciones.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-delegaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './delegaciones.component.html',
  styleUrl: './delegaciones.component.css'
})
export class DelegacionesComponent implements OnInit {
  delegaciones: any[] = [];
  empleados: any[] = [];
  modal = false;
  form: any = {};
  guardando = false;
  cargando = false;

  tipos = TIPOS_DELEGACION;
  alcances = ALCANCES_DELEGACION;

  constructor(private svc: TalentoHumanoService) {}

  ngOnInit() {
    this.cargar();
    this.svc.getEmpleados().subscribe(r => this.empleados = r);
  }

  cargar() {
    this.cargando = true;
    this.svc.getDelegaciones().subscribe({
      next: r => { this.delegaciones = r; this.cargando = false; },
      error: () => this.cargando = false
    });
  }

  abrirNueva() {
    this.form = {
      idEmpleadoOrigen: null,
      idEmpleadoDelegado: null,
      idAsignacionOrigen: null,
      idAsignacionDelegada: null,
      origenInfo: '',
      delegadoInfo: '',
      fechaInicio: '',
      fechaFin: '',
      tipo: 'VACACIONES',
      alcance: 'TOTAL',
      documentoRespaldoId: null,
      observacion: ''
    };
    this.modal = true;
  }

  onEmpleadoOrigenChange() {
    const e = this.form.idEmpleadoOrigen;
    if (!e) { this.form.idAsignacionOrigen = null; this.form.origenInfo = ''; return; }
    this.svc.getAsignacionActual(e).subscribe({
      next: a => {
        this.form.idAsignacionOrigen = a?.idAsignacion || null;
        this.form.origenInfo = a?.puestoNombre || '';
      },
      error: () => { this.form.idAsignacionOrigen = null; this.form.origenInfo = 'Sin asignación vigente'; }
    });
  }

  onEmpleadoDelegadoChange() {
    const e = this.form.idEmpleadoDelegado;
    if (!e) { this.form.idAsignacionDelegada = null; this.form.delegadoInfo = ''; return; }
    this.svc.getAsignacionActual(e).subscribe({
      next: a => {
        this.form.idAsignacionDelegada = a?.idAsignacion || null;
        this.form.delegadoInfo = a?.puestoNombre || '';
      },
      error: () => { this.form.idAsignacionDelegada = null; this.form.delegadoInfo = 'Sin asignación vigente'; }
    });
  }

  guardar() {
    if (!this.form.idEmpleadoOrigen || !this.form.idEmpleadoDelegado) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Seleccione el funcionario a cubrir y el delegado.' });
      return;
    }
    if (!this.form.idAsignacionOrigen || !this.form.idAsignacionDelegada) {
      Swal.fire({ icon: 'warning', title: 'Asignación no vigente', text: 'Ambos funcionarios deben tener una asignación activa.' });
      return;
    }
    if (!this.form.fechaInicio) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'La fecha de inicio es obligatoria.' });
      return;
    }
    if (this.form.idAsignacionOrigen === this.form.idAsignacionDelegada) {
      Swal.fire({ icon: 'warning', title: 'Asignación inválida', text: 'La asignación delegada debe ser distinta de la de origen.' });
      return;
    }
    this.guardando = true;
    this.svc.crearDelegacion({
      idAsignacionOrigen: this.form.idAsignacionOrigen,
      idAsignacionDelegada: this.form.idAsignacionDelegada,
      fechaInicio: this.form.fechaInicio || null,
      fechaFin: this.form.fechaFin || null,
      tipo: this.form.tipo || null,
      alcance: this.form.alcance || null,
      documentoRespaldoId: this.form.documentoRespaldoId || null,
      observacion: this.form.observacion || null
    }).subscribe({
      next: () => {
        this.guardando = false;
        this.modal = false;
        this.cargar();
        Swal.fire('Guardado', 'Delegación creada correctamente.', 'success');
      },
      error: (e) => {
        this.guardando = false;
        Swal.fire('Error', e.error?.error || 'No se pudo crear la delegación.', 'error');
      }
    });
  }

  cancelar(d: any) {
    Swal.fire({
      title: '¿Cancelar delegación?',
      text: `${d.empleadoOrigen || ''} ya no estará cubierto por ${d.empleadoDelegado || ''}.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, cancelar',
      cancelButtonText: 'Volver'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.cancelarDelegacion(d.idDelegacion).subscribe({
        next: () => this.cargar(),
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo cancelar.', 'error')
      });
    });
  }

  finalizar(d: any) {
    Swal.fire({
      title: '¿Finalizar delegación?',
      text: 'La delegación quedará marcada como finalizada.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, finalizar',
      cancelButtonText: 'Volver'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.finalizarDelegacion(d.idDelegacion).subscribe({
        next: () => this.cargar(),
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo finalizar.', 'error')
      });
    });
  }

  estadoBadge(estado: string): string {
    return estado === 'ACTIVA' ? 'badge-ok' : estado === 'CANCELADA' ? 'badge-no' : 'badge-otro';
  }

  activaHoy(d: any): boolean {
    if (d.estado !== 'ACTIVA') return false;
    const hoy = new Date();
    const ini = new Date(d.fechaInicio + 'T00:00:00');
    if (d.fechaFin) {
      const fin = new Date(d.fechaFin + 'T00:00:00');
      return ini <= hoy && fin >= hoy;
    }
    return ini <= hoy;
  }

  etiquetaTipo(tipo: string): string {
    const t = this.tipos.find(x => x.valor === tipo);
    return t ? t.etiqueta : (tipo || '—');
  }
}