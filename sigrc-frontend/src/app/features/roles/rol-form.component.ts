import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RolService } from '@core/services/rol.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-rol-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './rol-form.component.html',
  styleUrl: './rol-form.component.css'
})
export class RolFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};

  constructor(
    private rolService: RolService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarRol(this.editandoId);
    } else {
      this.form = { codigo: '', nombre: '', descripcion: '' };
    }
  }

  cargarRol(id: number) {
    this.rolService.obtener(id).subscribe(r => {
      this.form = { codigo: r.codigo, nombre: r.nombre, descripcion: r.descripcion };
    });
  }

  volver() {
    this.router.navigate(['/roles']);
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
      descripcion: this.form.descripcion || null
    };
    const obs = this.editandoId
      ? this.rolService.actualizar(this.editandoId, payload)
      : this.rolService.crear(payload);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Rol guardado correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el rol.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}
