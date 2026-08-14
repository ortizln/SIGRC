import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-unidad-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './unidad-form.component.html',
  styleUrl: './unidad-form.component.css'
})
export class UnidadFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};
  niveles: any[] = [];
  unidades: any[] = [];

  constructor(
    private svc: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.cargarNiveles();
    this.cargarUnidades();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarUnidad(this.editandoId);
    } else {
      this.form = { tipoUnidad: 'UNIDAD', idNivel: null, idUnidadPadre: null, activo: true };
    }
  }

  cargarNiveles() { this.svc.getNiveles().subscribe(r => this.niveles = r); }
  cargarUnidades() { this.svc.getUnidades().subscribe(r => this.unidades = r); }

  cargarUnidad(id: number) {
    this.svc.getUnidad(id).subscribe(r => {
      this.form = {
        codigo: r.codigo,
        nombre: r.nombre,
        sigla: r.sigla || '',
        descripcion: r.descripcion || '',
        tipoUnidad: r.tipoUnidad || 'UNIDAD',
        idNivel: r.idNivel || null,
        idUnidadPadre: r.idUnidadPadre || null,
        orden: r.orden,
        activo: r.activo
      };
    });
  }

  volver() {
    this.router.navigate(['/talento-humano/estructura']);
  }

  guardar() {
    if (!this.form.codigo || !this.form.nombre) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Código y nombre son obligatorios.' });
      return;
    }
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
        Swal.fire('Guardado', 'Unidad guardada correctamente.', 'success').then(() => this.router.navigate(['/talento-humano/estructura']));
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar la unidad.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}