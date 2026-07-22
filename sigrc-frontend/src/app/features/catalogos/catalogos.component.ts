import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogoService } from '@core/services/catalogo.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-catalogos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogos.component.html',
  styleUrl: './catalogos.component.css'
})
export class CatalogosComponent implements OnInit {
  tabActivo = 'areas';
  areas: any[] = [];
  sistemas: any[] = [];
  categorias: any[] = [];
  subcategorias: any[] = [];
  form: any = {};
  formVisible = false;
  editando = false;
  editandoId: number | null = null;
  cargando = false;

  seedMsg = '';
  seedError = false;

  menuAbierto: number | null = null;

  constructor(private svc: CatalogoService) {}

  ngOnInit() {
    this.cargarAreas();
    this.cargarSistemas();
    this.cargarCategorias();
  }

  cargarAreas() { this.svc.getAreas().subscribe(r => this.areas = r); }
  cargarSistemas() { this.svc.getSistemas().subscribe(r => this.sistemas = r); }
  cargarCategorias() { this.svc.getCategorias().subscribe(r => this.categorias = r); }

  cargarSubcategorias() {
    if (this.form.idCategoria) {
      this.svc.getSubcategorias(this.form.idCategoria).subscribe(r => this.subcategorias = r);
    } else {
      this.subcategorias = [];
    }
  }

  onTab(tab: string) {
    this.tabActivo = tab;
    this.formVisible = false;
    this.menuAbierto = null;
    this.cancelar();
    if (tab === 'subcategorias') this.cargarSubcategorias();
  }

  toggleMenu(id: number) {
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  nuevo() {
    this.form = {};
    this.editando = false;
    this.editandoId = null;
    this.formVisible = true;
  }

  editar(item: any, tipo: string) {
    this.form = { ...item };
    this.editando = true;
    this.editandoId = item.idArea || item.idSistema || item.idCategoria || item.idSubcategoria;
    this.formVisible = true;
    this.menuAbierto = null;
    if (tipo === 'subcategoria') this.form.idCategoria = item.categoria?.idCategoria;
  }

  cancelar() {
    this.form = {};
    this.editando = false;
    this.editandoId = null;
    this.formVisible = false;
  }

  guardar() {
    this.cargando = true;
    const tab = this.tabActivo;
    const esEdicion = this.editando && this.editandoId;

    let payload = { ...this.form };
    if (tab === 'subcategorias') {
      if (payload.idCategoria) {
        payload.categoria = { idCategoria: payload.idCategoria };
      }
      delete payload.idCategoria;
    }

    let obs;
    if (tab === 'areas') {
      obs = esEdicion
        ? this.svc.actualizarArea(this.editandoId!, payload)
        : this.svc.crearArea(payload);
    } else if (tab === 'sistemas') {
      obs = esEdicion
        ? this.svc.actualizarSistema(this.editandoId!, payload)
        : this.svc.crearSistema(payload);
    } else if (tab === 'categorias') {
      obs = esEdicion
        ? this.svc.actualizarCategoria(this.editandoId!, payload)
        : this.svc.crearCategoria(payload);
    } else {
      obs = esEdicion
        ? this.svc.actualizarSubcategoria(this.editandoId!, payload)
        : this.svc.crearSubcategoria(payload);
    }

    obs.subscribe({
      next: () => {
        this.cargando = false;
        this.cancelar();
        if (tab === 'areas') this.cargarAreas();
        else if (tab === 'sistemas') this.cargarSistemas();
        else if (tab === 'categorias') this.cargarCategorias();
        else this.cargarSubcategorias();
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar. Verifique que el código no esté duplicado.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  seedData() {
    Swal.fire({
      title: '¿Poblar catálogos?',
      text: 'Se insertarán datos de ejemplo solo si están vacíos.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, poblar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.cargando = true;
      this.seedMsg = '';
      this.seedError = false;
      this.svc.seed().subscribe({
        next: (r: any) => {
          this.seedMsg = `Datos creados: ${r.areas} áreas, ${r.sistemas} sistemas, ${r.categorias} categorías, ${r.subcategorias} subcategorías.`;
          this.cargando = false;
          this.cargarAreas(); this.cargarSistemas(); this.cargarCategorias();
        },
        error: (e) => {
          this.seedError = true;
          this.seedMsg = e.error?.message || 'Error al poblar datos';
          this.cargando = false;
        }
      });
    });
  }

  desactivar(item: any, tipo: string) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `${item.nombre} quedará inactivo pero los datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      let obs;
      if (tipo === 'area') obs = this.svc.eliminarArea(item.idArea);
      else if (tipo === 'sistema') obs = this.svc.eliminarSistema(item.idSistema);
      else if (tipo === 'categoria') obs = this.svc.eliminarCategoria(item.idCategoria);
      else obs = this.svc.eliminarSubcategoria(item.idSubcategoria);
      obs.subscribe(() => {
        if (tipo === 'area') this.cargarAreas();
        else if (tipo === 'sistema') this.cargarSistemas();
        else if (tipo === 'categoria') this.cargarCategorias();
        else this.cargarSubcategorias();
      });
    });
  }

  eliminarFisico(item: any, tipo: string) {
    Swal.fire({
      title: '¿Eliminar permanentemente?',
      text: `${item.nombre} se eliminará de la base de datos. Esta acción no se puede deshacer.`,
      icon: 'error',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#dc3545'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      let obs;
      if (tipo === 'area') obs = this.svc.eliminarAreaHard(item.idArea);
      else if (tipo === 'sistema') obs = this.svc.eliminarSistemaHard(item.idSistema);
      else if (tipo === 'categoria') obs = this.svc.eliminarCategoriaHard(item.idCategoria);
      else obs = this.svc.eliminarSubcategoriaHard(item.idSubcategoria);
      obs.subscribe((res: any) => {
        if (res.success) {
          Swal.fire('Eliminado', 'Registro eliminado permanentemente.', 'success');
          if (tipo === 'area') this.cargarAreas();
          else if (tipo === 'sistema') this.cargarSistemas();
          else if (tipo === 'categoria') this.cargarCategorias();
          else this.cargarSubcategorias();
        } else {
          Swal.fire('No se puede eliminar', res.message || 'Este registro está siendo usado por otros datos.', 'warning');
        }
      });
    });
  }
}
