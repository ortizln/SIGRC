import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { TIPOS_AUSENCIA } from '@shared/models/gestion-personal.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-vacaciones-permisos-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './vacaciones-permisos-form.component.html',
  styleUrl: './vacaciones-permisos-form.component.css'
})
export class VacacionesPermisosFormComponent implements OnInit {
  empleados: any[] = [];
  tipos = TIPOS_AUSENCIA;
  cargando = false;
  usuarioLogueado: any;
  form: any = {};

  constructor(
    private svc: GestionPersonalService,
    private thSvc: TalentoHumanoService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.usuarioLogueado = this.auth.getUsuario();
  }

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  get soloMiEmpleado(): boolean {
    return !!this.usuarioLogueado?.idEmpleado && !this.isAdmin;
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      // Solo existe creación; no hay edición para solicitudes de ausencia.
      this.form = {};
    }
    this.form = {
      idEmpleado: this.soloMiEmpleado ? this.usuarioLogueado.idEmpleado : null,
      tipo: 'PERMISO',
      fechaDesde: new Date().toISOString().split('T')[0],
      fechaHasta: new Date().toISOString().split('T')[0],
      dias: 1
    };
    this.thSvc.getEmpleados().subscribe(r => this.empleados = r);
  }

  volver() {
    this.router.navigate(['/talento-humano/vacaciones-permisos']);
  }

  guardar() {
    if (!this.form.idEmpleado || !this.form.tipo || !this.form.fechaDesde || !this.form.fechaHasta) {
      Swal.fire('Faltan datos', 'Empleado, tipo y fechas son obligatorios.', 'warning');
      return;
    }
    this.cargando = true;
    this.svc.crearAusencia(this.form).subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Solicitud registrada correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar la solicitud.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}