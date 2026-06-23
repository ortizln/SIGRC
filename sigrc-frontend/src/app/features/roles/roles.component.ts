import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RolService } from '@core/services/rol.service';
import { PermisoService } from '@core/services/permiso.service';
import { Rol } from '@shared/models/rol.model';
import { Permiso } from '@shared/models/permiso.model';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roles.component.html',
  styleUrl: './roles.component.css'
})
export class RolesComponent implements OnInit {
  roles: Rol[] = [];
  permisos: Permiso[] = [];
  rolSeleccionado: Rol | null = null;
  permisoSeleccionado = new Set<number>();

  rolFormVisible = false;
  editandoRol = false;
  editandoRolId: number | null = null;
  rolForm: any = {};

  editandoPermisos = false;
  gestionarDefiniciones = false;
  cargando = false;
  cargandoPermiso = false;
  permisoForm: any = {};

  constructor(private rolService: RolService, private permisoService: PermisoService) {}

  ngOnInit() {
    this.cargarRoles();
    this.cargarPermisos();
  }

  get permisosAgrupados() {
    const grupos = new Map<string, Permiso[]>();
    for (const p of this.permisos) {
      if (!grupos.has(p.modulo)) grupos.set(p.modulo, []);
      grupos.get(p.modulo)!.push(p);
    }
    return Array.from(grupos.entries()).map(([modulo, permisos]) => ({ modulo, permisos }));
  }

  cargarRoles() { this.rolService.listar().subscribe(r => this.roles = r); }
  cargarPermisos() { this.permisoService.listar().subscribe(r => this.permisos = r); }

  abrirRolForm() {
    this.editandoRol = false; this.editandoRolId = null;
    this.rolForm = { codigo: '', nombre: '', descripcion: '' };
    this.rolFormVisible = true;
  }

  editarRol(r: Rol) {
    this.editandoRol = true; this.editandoRolId = r.idRol;
    this.rolForm = { codigo: r.codigo, nombre: r.nombre, descripcion: r.descripcion };
    this.rolFormVisible = true;
  }

  cancelarRol() { this.rolFormVisible = false; }

  guardarRol() {
    this.cargando = true;
    const req = this.editandoRol
      ? this.rolService.actualizar(this.editandoRolId!, this.rolForm)
      : this.rolService.crear(this.rolForm);
    req.subscribe({
      next: () => { this.cargando = false; this.rolFormVisible = false; this.cargarRoles(); },
      error: () => { this.cargando = false; }
    });
  }

  eliminarRol(r: Rol) {
    if (!confirm(`¿Desactivar rol ${r.nombre}?`)) return;
    this.rolService.eliminar(r.idRol).subscribe(() => this.cargarRoles());
  }

  gestionarPermisos(r: Rol) {
    this.rolSeleccionado = r;
    this.permisoSeleccionado = new Set(r.permisoIds || []);
    this.editandoPermisos = true;
    this.gestionarDefiniciones = false;
  }

  crearPermiso() {
    this.cargandoPermiso = true;
    this.permisoService.crear(this.permisoForm).subscribe({
      next: () => {
        this.cargandoPermiso = false;
        this.permisoForm = { codigo: '', nombre: '', modulo: '', tipoAcceso: 'LECTURA', descripcion: '' };
        this.cargarPermisos();
      },
      error: () => { this.cargandoPermiso = false; }
    });
  }

  eliminarPermiso(p: Permiso) {
    if (!confirm(`¿Desactivar permiso ${p.codigo}?`)) return;
    this.permisoService.eliminar(p.idPermiso).subscribe(() => this.cargarPermisos());
  }

  cancelarPermisos() {
    this.editandoPermisos = false;
    this.rolSeleccionado = null;
  }

  togglePermiso(id: number) {
    if (this.permisoSeleccionado.has(id)) this.permisoSeleccionado.delete(id);
    else this.permisoSeleccionado.add(id);
  }

  guardarPermisos() {
    if (!this.rolSeleccionado) return;
    this.cargando = true;
    this.rolService.actualizar(this.rolSeleccionado.idRol, {
      codigo: this.rolSeleccionado.codigo,
      nombre: this.rolSeleccionado.nombre,
      permisoIds: Array.from(this.permisoSeleccionado)
    }).subscribe({
      next: () => { this.cargando = false; this.editandoPermisos = false; this.cargarRoles(); },
      error: () => { this.cargando = false; }
    });
  }
}
