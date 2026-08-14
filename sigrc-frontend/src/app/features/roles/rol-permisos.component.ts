import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { RolService } from '@core/services/rol.service';
import { PermisoService } from '@core/services/permiso.service';
import { Permiso } from '@shared/models/permiso.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-rol-permisos',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './rol-permisos.component.html',
  styleUrl: './rol-permisos.component.css'
})
export class RolPermisosComponent implements OnInit {
  rol: any = null;
  permisos: Permiso[] = [];
  permisoSeleccionado = new Set<number>();
  cargando = false;

  constructor(
    private rolService: RolService,
    private permisoService: PermisoService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  get esAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  get permisosAgrupados() {
    const grupos = new Map<string, Permiso[]>();
    for (const p of this.permisos) {
      if (!grupos.has(p.modulo)) grupos.set(p.modulo, []);
      grupos.get(p.modulo)!.push(p);
    }
    return Array.from(grupos.entries()).map(([modulo, permisos]) => ({ modulo, permisos }));
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.volver();
      return;
    }
    this.rolService.obtener(Number(id)).subscribe({
      next: r => {
        this.rol = r;
        this.permisoSeleccionado = new Set(r.permisoIds || []);
      },
      error: () => this.volver()
    });
    this.permisoService.listar().subscribe(r => this.permisos = r);
  }

  volver() {
    this.router.navigate(['/roles']);
  }

  togglePermiso(id: number) {
    if (this.permisoSeleccionado.has(id)) this.permisoSeleccionado.delete(id);
    else this.permisoSeleccionado.add(id);
  }

  guardarPermisos() {
    if (!this.rol) return;
    this.cargando = true;
    this.rolService.actualizar(this.rol.idRol, {
      codigo: this.rol.codigo,
      nombre: this.rol.nombre,
      permisoIds: Array.from(this.permisoSeleccionado)
    }).subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Permisos del rol actualizados correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar los permisos.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}