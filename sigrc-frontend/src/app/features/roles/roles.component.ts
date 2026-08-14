import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { RolService } from '@core/services/rol.service';
import { PermisoService } from '@core/services/permiso.service';
import { Rol } from '@shared/models/rol.model';
import { Permiso } from '@shared/models/permiso.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './roles.component.html',
  styleUrl: './roles.component.css'
})
export class RolesComponent implements OnInit {
  roles: Rol[] = [];
  permisos: Permiso[] = [];

  constructor(private rolService: RolService, private permisoService: PermisoService, private auth: AuthService) {}

  get esAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.cargarRoles();
    this.cargarPermisos();
  }

  cargarRoles() { this.rolService.listar().subscribe(r => this.roles = r); }
  cargarPermisos() { this.permisoService.listar().subscribe(r => this.permisos = r); }

  eliminarRol(r: Rol) {
    Swal.fire({
      title: '¿Desactivar rol?',
      text: `${r.nombre} quedará inactivo pero sus datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(res => {
      if (!res.isConfirmed) return;
      this.rolService.eliminar(r.idRol).subscribe({
        next: () => this.cargarRoles(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar el rol.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }

  eliminarPermiso(p: Permiso) {
    Swal.fire({
      title: '¿Desactivar permiso?',
      text: `${p.codigo} quedará inactivo pero sus datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(res => {
      if (!res.isConfirmed) return;
      this.permisoService.eliminar(p.idPermiso).subscribe({
        next: () => this.cargarPermisos(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar el permiso.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }
}
