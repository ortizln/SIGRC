import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { TIPOS_ACCION_PERSONAL } from '@shared/models/gestion-personal.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-acciones-personal-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './acciones-personal-form.component.html',
  styleUrl: './acciones-personal-form.component.css'
})
export class AccionesPersonalFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};
  empleados: any[] = [];
  tipos = TIPOS_ACCION_PERSONAL;

  constructor(
    private svc: GestionPersonalService,
    private thSvc: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.thSvc.getEmpleados().subscribe(r => this.empleados = r);
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarAccion(this.editandoId);
    } else {
      this.form = {
        idEmpleado: null,
        tipo: 'NOMBRAMIENTO',
        fechaEmision: new Date().toISOString().split('T')[0],
        fechaVigenciaDesde: null,
        fechaVigenciaHasta: null,
        motivo: '',
        situacionActual: '',
        situacionPropuesta: '',
        documentoId: null
      };
    }
  }

  cargarAccion(id: number) {
    this.svc.listarAcciones().subscribe(acciones => {
      const a = acciones.find(x => x.idAccion === id);
      if (!a) {
        this.volver();
        return;
      }
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
    });
  }

  volver() {
    this.router.navigate(['/talento-humano/acciones-personal']);
  }

  guardar() {
    if (!this.form.idEmpleado || !this.form.tipo) {
      Swal.fire('Faltan datos', 'Empleado y tipo son obligatorios.', 'warning');
      return;
    }
    this.cargando = true;
    const payload = {
      idEmpleado: this.form.idEmpleado,
      tipo: this.form.tipo,
      fechaEmision: this.form.fechaEmision || null,
      fechaVigenciaDesde: this.form.fechaVigenciaDesde || null,
      fechaVigenciaHasta: this.form.fechaVigenciaHasta || null,
      motivo: this.form.motivo || null,
      situacionActual: this.form.situacionActual || null,
      situacionPropuesta: this.form.situacionPropuesta || null,
      documentoId: this.form.documentoId || null
    };
    const obs = this.editandoId
      ? this.svc.actualizarAccion(this.editandoId, payload)
      : this.svc.crearAccion(payload);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Acción de personal guardada correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar la acción de personal.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}