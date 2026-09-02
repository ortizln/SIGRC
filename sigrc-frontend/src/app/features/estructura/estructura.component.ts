import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-estructura',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './estructura.component.html',
  styleUrl: './estructura.component.css'
})
export class EstructuraComponent implements OnInit {
  tabActivo = 'unidades';
  niveles: any[] = [];
  unidades: any[] = [];
  organigrama: any[] = [];

  menuAbierto: number | null = null;

  expandidos = new Set<number>();
  detalle: any = null;

  migracionVisible = false;
  migracionCargando = false;
  migracionResultado: any = null;

  dragUnidadId: number | null = null;

  constructor(private svc: TalentoHumanoService, private auth: AuthService, private router: Router) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.cargarNiveles();
    this.cargarUnidades();
    this.cargarOrganigrama();
  }

  cargarNiveles() { this.svc.getNiveles().subscribe(r => this.niveles = r); }
  cargarUnidades() { this.svc.getUnidades().subscribe(r => this.unidades = r); }
  cargarOrganigrama() {
    this.svc.getOrganigrama().subscribe(r => {
      this.organigrama = r;
      this.expandidos = new Set(this.coleccionarIds(r));
    });
  }

  coleccionarIds(nodos: any[]): number[] {
    const ids: number[] = [];
    for (const n of nodos) {
      ids.push(n.idUnidad);
      if (n.hijos && n.hijos.length) ids.push(...this.coleccionarIds(n.hijos));
    }
    return ids;
  }

  toggleExpandir(id: number) {
    if (this.expandidos.has(id)) this.expandidos.delete(id);
    else this.expandidos.add(id);
  }

  expandirTodo() {
    this.expandidos = new Set(this.coleccionarIds(this.organigrama));
  }

  colapsarTodo() {
    this.expandidos = new Set(this.organigrama.map(n => n.idUnidad));
  }

  verPerfil(unidad: any) {
    this.detalle = unidad;
  }

  cerrarDetalle() {
    this.detalle = null;
  }

  editarDesdeDetalle() {
    if (!this.detalle) return;
    this.router.navigate([`/talento-humano/estructura/unidad/editar/${this.detalle.idUnidad}`]);
  }

  estadoPuesto(p: any): { clase: string; etiqueta: string } {
    if (!p.numeroPlazas) return { clase: 'org-puesto-ok', etiqueta: 'Completo' };
    if (p.ocupados <= 0) return { clase: 'org-puesto-vacante', etiqueta: 'Vacante' };
    if (p.vacantes > 0) return { clase: 'org-puesto-parcial', etiqueta: 'Parcial' };
    return { clase: 'org-puesto-ok', etiqueta: 'Completo' };
  }

  ejecutarMigracion(dryRun: boolean) {
    this.migracionCargando = true;
    this.svc.migrarUsuarios(dryRun).subscribe({
      next: (r) => {
        this.migracionCargando = false;
        this.migracionResultado = r;
        if (!dryRun) {
          this.cargarUnidades();
          this.cargarOrganigrama();
        }
      },
      error: (err) => {
        this.migracionCargando = false;
        const msg = err.error?.error || 'Error al ejecutar la migración.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  onTab(tab: string) {
    this.tabActivo = tab;
    this.menuAbierto = null;
  }

  toggleMenu(id: number) {
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  desactivar(unidad: any) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `${unidad.nombre} quedará inactiva pero sus datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      this.svc.eliminarUnidad(unidad.idUnidad).subscribe({
        next: () => { this.cargarUnidades(); this.cargarOrganigrama(); },
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar la unidad.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }

  desactivarNivel(n: any) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `${n.nombre} quedará inactivo pero sus datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      this.svc.eliminarNivel(n.idNivel).subscribe({
        next: () => this.cargarNiveles(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar el nivel.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }

  // ─────────── Drag & Drop ───────────

  onDragStart(event: DragEvent, idUnidad: number) {
    if (!this.isAdmin) return;
    this.dragUnidadId = idUnidad;
    event.dataTransfer!.effectAllowed = 'move';
    event.dataTransfer!.setData('text/plain', String(idUnidad));
    (event.target as HTMLElement).classList.add('org-dragging');
  }

  onDragEnd(event: DragEvent) {
    this.dragUnidadId = null;
    (event.target as HTMLElement).classList.remove('org-dragging');
  }

  onDragOver(event: DragEvent) {
    if (!this.isAdmin || !this.dragUnidadId) return;
    event.preventDefault();
    event.dataTransfer!.dropEffect = 'move';
    (event.currentTarget as HTMLElement).classList.add('org-drop-target');
  }

  onDragLeave(event: DragEvent) {
    (event.currentTarget as HTMLElement).classList.remove('org-drop-target');
  }

  onDrop(event: DragEvent, idNuevoPadre: number) {
    event.preventDefault();
    (event.currentTarget as HTMLElement).classList.remove('org-drop-target');
    if (!this.isAdmin || !this.dragUnidadId) return;
    const idArrastrada = this.dragUnidadId;
    this.dragUnidadId = null;
    if (idArrastrada === idNuevoPadre) return;

    this.svc.moverUnidad(idArrastrada, idNuevoPadre).subscribe({
      next: () => {
        this.cargarOrganigrama();
        this.cargarUnidades();
      },
      error: (err) => {
        const msg = err.error?.error || 'No se pudo mover la unidad.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  onDropRaiz(event: DragEvent) {
    event.preventDefault();
    (event.currentTarget as HTMLElement).classList.remove('org-drop-target');
    if (!this.isAdmin || !this.dragUnidadId) return;
    const idArrastrada = this.dragUnidadId;
    this.dragUnidadId = null;

    this.svc.moverUnidad(idArrastrada, null).subscribe({
      next: () => {
        this.cargarOrganigrama();
        this.cargarUnidades();
      },
      error: (err) => {
        const msg = err.error?.error || 'No se pudo mover la unidad.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}