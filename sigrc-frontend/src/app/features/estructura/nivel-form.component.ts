import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-nivel-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './nivel-form.component.html',
  styleUrl: './nivel-form.component.css'
})
export class NivelFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};

  constructor(
    private svc: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarNivel(this.editandoId);
    } else {
      this.form = { codigo: '', nombre: '', descripcion: '', orden: null, activo: true };
    }
  }

  cargarNivel(id: number) {
    this.svc.getNiveles().subscribe(lista => {
      const n = lista.find(x => x.idNivel === id);
      if (!n) {
        this.volver();
        return;
      }
      this.form = {
        idNivel: n.idNivel,
        codigo: n.codigo,
        nombre: n.nombre,
        descripcion: n.descripcion || '',
        orden: n.orden,
        activo: n.activo
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
      descripcion: this.form.descripcion || null,
      orden: this.form.orden || null
    };
    const obs = this.editando && this.editandoId
      ? this.svc.actualizarNivel(this.editandoId, payload)
      : this.svc.crearNivel(payload);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Nivel guardado correctamente.', 'success').then(() => this.router.navigate(['/talento-humano/estructura']));
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el nivel.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}