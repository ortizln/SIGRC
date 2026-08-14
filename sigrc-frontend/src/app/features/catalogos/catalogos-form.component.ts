import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CatalogoService } from '@core/services/catalogo.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-catalogos-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './catalogos-form.component.html',
  styleUrl: './catalogos-form.component.css'
})
export class CatalogosFormComponent implements OnInit {
  tipo = '';
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = { nombre: '', codigo: '', descripcion: '', idCategoria: null };
  categorias: any[] = [];

  constructor(
    private svc: CatalogoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  get tab(): string {
    switch (this.tipo) {
      case 'area': return 'areas';
      case 'sistema': return 'sistemas';
      case 'categoria': return 'categorias';
      default: return 'subcategorias';
    }
  }

  get titulo(): string {
    if (this.editando) {
      switch (this.tipo) {
        case 'area': return 'Editar Áreas';
        case 'sistema': return 'Editar Sistemas';
        case 'categoria': return 'Editar Categorías';
        default: return 'Editar Subcategorías';
      }
    }
    switch (this.tipo) {
      case 'area': return 'Nueva Área';
      case 'sistema': return 'Nuevo Sistema';
      case 'categoria': return 'Nueva Categoría';
      default: return 'Nueva Subcategoría';
    }
  }

  get etiqueta(): string {
    switch (this.tipo) {
      case 'area': return 'Área';
      case 'sistema': return 'Sistema';
      case 'categoria': return 'Categoría';
      default: return 'Subcategoría';
    }
  }

  ngOnInit() {
    this.tipo = this.route.snapshot.paramMap.get('tipo') || 'area';
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarRegistro();
    } else {
      this.form = { nombre: '', codigo: '', descripcion: '', idCategoria: null };
      if (this.tipo === 'subcategoria') this.cargarCategorias();
    }
  }

  cargarCategorias() { this.svc.getCategorias().subscribe(r => this.categorias = r); }

  cargarRegistro() {
    switch (this.tipo) {
      case 'area':
        this.svc.getAreas().subscribe(list => {
          const r = list.find(x => x.idArea === this.editandoId);
          if (r) this.rellenarForm(r);
        });
        break;
      case 'sistema':
        this.svc.getSistemas().subscribe(list => {
          const r = list.find(x => x.idSistema === this.editandoId);
          if (r) this.rellenarForm(r);
        });
        break;
      case 'categoria':
        this.svc.getCategorias().subscribe(list => {
          const r = list.find(x => x.idCategoria === this.editandoId);
          if (r) this.rellenarForm(r);
        });
        break;
      default:
        this.cargarSubcategoriasParaEdicion();
    }
  }

  cargarSubcategoriasParaEdicion() {
    this.svc.getCategorias().subscribe(cats => {
      this.categorias = cats;
      if (cats.length === 0) return;
      forkJoin(cats.map(c => this.svc.getSubcategorias(c.idCategoria))).subscribe(results => {
        const r = results.flat().find(x => x.idSubcategoria === this.editandoId);
        if (r) this.rellenarForm(r);
      });
    });
  }

  private rellenarForm(r: any) {
    this.form = {
      nombre: r.nombre || '',
      codigo: r.codigo || '',
      descripcion: r.descripcion || '',
      idCategoria: r.idCategoria ?? r.categoria?.idCategoria ?? null
    };
  }

  volver() {
    this.router.navigate(['/catalogos']);
  }

  guardar() {
    if (!this.form.nombre || !this.form.codigo) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Código y nombre son obligatorios.' });
      return;
    }
    if (this.tipo === 'subcategoria' && !this.form.idCategoria) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Seleccione una categoría.' });
      return;
    }
    this.cargando = true;
    const tab = this.tab;
    const esEdicion = this.editandoId != null;

    let payload = { ...this.form };
    if (tab === 'subcategorias') {
      if (payload.idCategoria) payload.categoria = { idCategoria: payload.idCategoria };
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
        Swal.fire('Guardado', 'Guardado correctamente.', 'success').then(() => this.router.navigate(['/catalogos']));
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar. Verifique que el código no esté duplicado.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}