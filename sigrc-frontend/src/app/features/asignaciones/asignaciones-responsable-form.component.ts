import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-asignaciones-responsable-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './asignaciones-responsable-form.component.html',
  styleUrl: './asignaciones-responsable-form.component.css'
})
export class AsignacionesResponsableFormComponent implements OnInit {
  idAsignacion = 0;
  empleadoActual = '';
  fechaInicio = '';
  puestoActual = '';
  cargando = false;
  respForm: any = {};
  unidades: any[] = [];

  constructor(
    private svc: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.idAsignacion = Number(this.route.snapshot.paramMap.get('id'));
    const empId = this.route.snapshot.queryParamMap.get('empId');
    if (empId) {
      this.svc.getAsignaciones(Number(empId)).subscribe(r => {
        const asig = r.find(x => x.idAsignacion === this.idAsignacion);
        if (asig) {
          this.empleadoActual = asig.nombreEmpleado || '';
          this.fechaInicio = asig.fechaInicio || '';
          this.puestoActual = asig.puestoNombre || '';
        }
      });
    }
    this.svc.getUnidades().subscribe(r => this.unidades = r);
  }

  volver() {
    this.router.navigate(['/talento-humano/asignaciones']);
  }

  guardar() {
    if (!this.respForm.idUnidad) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Seleccione la unidad.' });
      return;
    }
    this.cargando = true;
    this.svc.asignarResponsableUnidad(this.respForm.idUnidad, this.idAsignacion).subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Responsable de unidad actualizado.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'No se pudo asignar el responsable.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}