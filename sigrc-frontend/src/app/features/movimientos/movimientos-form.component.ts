import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { TIPOS_MOVIMIENTO } from '@shared/models/gestion-personal.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-movimientos-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './movimientos-form.component.html',
  styleUrl: './movimientos-form.component.css'
})
export class MovimientosFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};
  empleados: any[] = [];
  puestos: any[] = [];
  unidades: any[] = [];
  asignaciones: any[] = [];
  tipos = TIPOS_MOVIMIENTO;

  constructor(
    private svc: GestionPersonalService,
    private thSvc: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.thSvc.getEmpleados().subscribe(r => this.empleados = r);
    this.thSvc.getPuestos().subscribe(r => this.puestos = r);
    this.thSvc.getUnidades().subscribe(r => this.unidades = r);
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarMovimiento(this.editandoId);
    } else {
      this.form = { tipoMovimiento: 'TRASLADO', fechaSolicitud: new Date().toISOString().split('T')[0] };
    }
  }

  cargarMovimiento(id: number) {
    this.svc.listarMovimientos().subscribe(r => {
      const m = r.find(x => x.idMovimiento === id);
      if (m) {
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
        this.onEmpleadoChange();
      }
    });
  }

  onEmpleadoChange() {
    if (this.form.idEmpleado) {
      this.thSvc.getAsignaciones(this.form.idEmpleado).subscribe(r => this.asignaciones = r);
    } else {
      this.asignaciones = [];
    }
  }

  volver() {
    this.router.navigate(['/talento-humano/movimientos']);
  }

  guardar() {
    if (!this.form.idEmpleado || !this.form.tipoMovimiento) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Empleado y tipo de movimiento son obligatorios.' });
      return;
    }
    this.cargando = true;
    const obs = this.editandoId
      ? this.svc.actualizarMovimiento(this.editandoId, this.form)
      : this.svc.crearMovimiento(this.form);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Movimiento guardado correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el movimiento.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}