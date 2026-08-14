import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { TIPOS_DELEGACION, ALCANCES_DELEGACION } from '@shared/models/delegaciones.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-delegaciones-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './delegaciones-form.component.html',
  styleUrl: './delegaciones-form.component.css'
})
export class DelegacionesFormComponent implements OnInit {
  cargando = false;
  form: any = {};
  empleados: any[] = [];

  tipos = TIPOS_DELEGACION;
  alcances = ALCANCES_DELEGACION;

  constructor(
    private svc: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.cargando = true;
      this.svc.getDelegaciones().subscribe({
        next: (r) => {
          this.cargando = false;
          const d = r.find(x => x.idDelegacion === Number(id));
          if (!d) {
            Swal.fire({ icon: 'warning', title: 'No encontrada', text: 'La delegación no existe.' }).then(() => this.volver());
            return;
          }
          this.form = {
            idEmpleadoOrigen: d.idEmpleadoOrigen || null,
            idEmpleadoDelegado: d.idEmpleadoDelegado || null,
            idAsignacionOrigen: d.idAsignacionOrigen || null,
            idAsignacionDelegada: d.idAsignacionDelegada || null,
            origenInfo: d.puestoOrigen || '',
            delegadoInfo: d.puestoDelegado || '',
            fechaInicio: d.fechaInicio || '',
            fechaFin: d.fechaFin || '',
            tipo: d.tipo || 'VACACIONES',
            alcance: d.alcance || 'TOTAL',
            documentoRespaldoId: d.documentoRespaldoId || null,
            observacion: d.observacion || ''
          };
        },
        error: () => {
          this.cargando = false;
          Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo cargar la delegación.' }).then(() => this.volver());
        }
      });
    } else {
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
    }
    this.svc.getEmpleados().subscribe(r => this.empleados = r);
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

  volver() {
    this.router.navigate(['/talento-humano/delegaciones']);
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
    this.cargando = true;
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
        this.cargando = false;
        Swal.fire('Guardado', 'Delegación registrada correctamente.', 'success').then(() => this.volver());
      },
      error: (e) => {
        this.cargando = false;
        const msg = e.error?.error || 'Error al guardar la delegación.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}
