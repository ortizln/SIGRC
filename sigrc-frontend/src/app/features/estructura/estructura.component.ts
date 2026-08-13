import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-estructura',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './estructura.component.html',
  styleUrl: './estructura.component.css'
})
export class EstructuraComponent implements OnInit {
  tabActivo = 'unidades';
  niveles: any[] = [];
  unidades: any[] = [];
  organigrama: any[] = [];

  form: any = {};
  formVisible = false;
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  menuAbierto: number | null = null;

  expandidos = new Set<number>();

  migracionVisible = false;
  migracionCargando = false;
  migracionResultado: any = null;

  constructor(private svc: TalentoHumanoService, private auth: AuthService) {}

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
    this.tabActivo = 'unidades';
    this.cancelar();
    this.editar(unidad);
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
    this.formVisible = false;
    this.menuAbierto = null;
    this.cancelar();
  }

  toggleMenu(id: number) {
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  nuevo() {
    this.form = { tipoUnidad: 'UNIDAD', idNivel: this.niveles[0]?.idNivel || null };
    this.editando = false;
    this.editandoId = null;
    this.formVisible = true;
  }

  editar(unidad: any) {
    this.form = {
      codigo: unidad.codigo,
      nombre: unidad.nombre,
      sigla: unidad.sigla || '',
      descripcion: unidad.descripcion || '',
      tipoUnidad: unidad.tipoUnidad || 'UNIDAD',
      idNivel: unidad.idNivel || null,
      idUnidadPadre: unidad.idUnidadPadre || null,
      orden: unidad.orden
    };
    this.editando = true;
    this.editandoId = unidad.idUnidad;
    this.formVisible = true;
    this.menuAbierto = null;
  }

  cancelar() {
    this.form = {};
    this.editando = false;
    this.editandoId = null;
    this.formVisible = false;
  }

  guardar() {
    this.cargando = true;
    const payload = {
      codigo: this.form.codigo,
      nombre: this.form.nombre,
      sigla: this.form.sigla || null,
      descripcion: this.form.descripcion || null,
      tipoUnidad: this.form.tipoUnidad || null,
      idNivel: this.form.idNivel || null,
      idUnidadPadre: this.form.idUnidadPadre || null,
      orden: this.form.orden || null
    };
    const obs = this.editando && this.editandoId
      ? this.svc.actualizarUnidad(this.editandoId, payload)
      : this.svc.crearUnidad(payload);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        this.cancelar();
        this.cargarUnidades();
        this.cargarOrganigrama();
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar. Verifique los datos.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
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

  nuevoNivel() {
    this.form = { codigo: '', nombre: '', descripcion: '', orden: this.niveles.length + 1 };
  }

  guardarNivel() {
    if (!this.form.codigo || !this.form.nombre) return;    this.cargando = true;
    const obs = this.form.idNivel
      ? this.svc.actualizarNivel(this.form.idNivel, {
          codigo: this.form.codigo, nombre: this.form.nombre,
          descripcion: this.form.descripcion || null, orden: this.form.orden || null
        })
      : this.svc.crearNivel({
          codigo: this.form.codigo, nombre: this.form.nombre,
          descripcion: this.form.descripcion || null, orden: this.form.orden || null
        });
    obs.subscribe({
      next: () => { this.cargando = false; this.form = {}; this.cargarNiveles(); },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el nivel.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  editarNivel(n: any) {
    this.form = { idNivel: n.idNivel, codigo: n.codigo, nombre: n.nombre, descripcion: n.descripcion || '', orden: n.orden };
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
}
