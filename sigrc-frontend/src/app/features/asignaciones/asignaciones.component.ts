import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-asignaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './asignaciones.component.html',
  styleUrl: './asignaciones.component.css'
})
export class AsignacionesComponent implements OnInit {
  empleados: any[] = [];
  puestos: any[] = [];
  unidades: any[] = [];

  idEmpleado: number | null = null;
  asignacionActual: any = null;
  historial: any[] = [];
  jefe: any = null;

  form: any = {};
  formVisible = false;

  respForm: any = {};
  respVisible = false;

  cargando = false;
  cargandoResp = false;

  constructor(private svc: TalentoHumanoService, private auth: AuthService) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.svc.getEmpleados().subscribe(r => this.empleados = r);
    this.svc.getPuestos().subscribe(r => this.puestos = r);
    this.svc.getUnidades().subscribe(r => this.unidades = r);
  }

  onEmpleadoChange() {
    this.asignacionActual = null;
    this.historial = [];
    this.jefe = null;
    if (!this.idEmpleado) return;
    this.cargarAsignaciones();
    this.cargarJefe();
  }

  cargarAsignaciones() {
    this.svc.getAsignacionActual(this.idEmpleado!).subscribe(r => this.asignacionActual = r);
    this.svc.getAsignaciones(this.idEmpleado!).subscribe(r => this.historial = r);
  }

  cargarJefe() {
    this.svc.getJefeInmediato(this.idEmpleado!).subscribe(r => this.jefe = r);
  }

  nuevo() {
    this.form = {
      idEmpleado: this.idEmpleado,
      tipoAsignacion: 'TITULAR',
      fechaInicio: new Date().toISOString().slice(0, 10),
      fechaFin: ''
    };
    this.formVisible = true;
  }

  cancelar() {
    this.form = {};
    this.formVisible = false;
  }

  guardar() {
    if (!this.form.idPuesto) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Seleccione el puesto a asignar.' });
      return;
    }
    this.cargando = true;
    this.svc.asignarPuesto({
      idEmpleado: this.form.idEmpleado,
      idPuesto: this.form.idPuesto,
      idUnidad: this.form.idUnidad || null,
      tipoAsignacion: this.form.tipoAsignacion || 'TITULAR',
      fechaInicio: this.form.fechaInicio || null,
      fechaFin: this.form.fechaFin || null,
      observacion: this.form.observacion || null
    }).subscribe({
      next: () => {
        this.cargando = false;
        this.cancelar();
        this.cargarAsignaciones();
        Swal.fire('Asignado', 'Puesto asignado correctamente. La asignación anterior quedó cerrada.', 'success');
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al asignar el puesto.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  finalizar(a: any) {
    Swal.fire({
      title: '¿Finalizar asignación?',
      text: `${a.puestoNombre || 'Puesto'} quedarán cerrados y se conservará en el historial.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, finalizar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.finalizarAsignacion(a.idAsignacion).subscribe({
        next: () => { this.cargarAsignaciones(); },
        error: (err) => {
          const msg = err.error?.error || 'No se pudo finalizar la asignación.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }

  onPuestoChange() {
    const puesto = this.puestos.find(p => p.idPuesto === this.form.idPuesto);
    if (puesto && puesto.idUnidad) this.form.idUnidad = puesto.idUnidad;
  }

  mostrarResponsables() {
    this.respVisible = !this.respVisible;
  }

  guardarResponsable() {
    if (!this.respForm.idUnidad || !this.respForm.idEmpleado) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Seleccione unidad y responsable.' });
      return;
    }
    this.cargandoResp = true;
    // Resolver la asignación principal vigente del empleado elegido
    this.svc.getAsignacionActual(this.respForm.idEmpleado).subscribe({
      next: (asig) => {
        if (!asig) {
          this.cargandoResp = false;
          Swal.fire({ icon: 'warning', title: 'Sin asignación', text: 'El empleado seleccionado no tiene una asignación vigente.' });
          return;
        }
        this.svc.asignarResponsableUnidad(this.respForm.idUnidad, asig.idAsignacion).subscribe({
          next: () => {
            this.cargandoResp = false;
            this.respForm = {};
            Swal.fire('Guardado', 'Responsable de unidad actualizado.', 'success');
          },
          error: (err) => {
            this.cargandoResp = false;
            const msg = err.error?.error || 'No se pudo asignar el responsable.';
            Swal.fire({ icon: 'error', title: 'Error', text: msg });
          }
        });
      },
      error: (err) => {
        this.cargandoResp = false;
        const msg = err.error?.error || 'No se pudo consultar la asignación.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}
