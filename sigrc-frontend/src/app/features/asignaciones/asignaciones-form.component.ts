import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-asignaciones-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './asignaciones-form.component.html',
  styleUrl: './asignaciones-form.component.css'
})
export class AsignacionesFormComponent implements OnInit {
  empleados: any[] = [];
  puestos: any[] = [];
  unidades: any[] = [];
  cargando = false;
  form: any = {};

  constructor(
    private svc: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    void this.route.snapshot.paramMap.get('id');
    this.form = {
      idEmpleado: null,
      idPuesto: null,
      idUnidad: null,
      tipoAsignacion: 'TITULAR',
      fechaInicio: new Date().toISOString().slice(0, 10),
      fechaFin: '',
      observacion: ''
    };
    this.svc.getEmpleados().subscribe(r => this.empleados = r);
    this.svc.getPuestos().subscribe(r => this.puestos = r);
    this.svc.getUnidades().subscribe(r => this.unidades = r);
  }

  onPuestoChange() {
    const puesto = this.puestos.find(p => p.idPuesto === this.form.idPuesto);
    if (puesto && puesto.idUnidad) this.form.idUnidad = puesto.idUnidad;
  }

  volver() {
    this.router.navigate(['/talento-humano/asignaciones']);
  }

  guardar() {
    if (!this.form.idEmpleado || !this.form.idPuesto) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Seleccione el empleado y el puesto a asignar.' });
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
        Swal.fire('Guardado', 'Asignación registrada correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar la asignación.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}