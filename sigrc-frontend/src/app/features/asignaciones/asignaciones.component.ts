import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-asignaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './asignaciones.component.html',
  styleUrl: './asignaciones.component.css'
})
export class AsignacionesComponent implements OnInit {
  empleados: any[] = [];

  idEmpleado: number | null = null;
  asignacionActual: any = null;
  historial: any[] = [];
  jefe: any = null;

  constructor(private svc: TalentoHumanoService, private auth: AuthService) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.svc.getEmpleados().subscribe(r => this.empleados = r);
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
}