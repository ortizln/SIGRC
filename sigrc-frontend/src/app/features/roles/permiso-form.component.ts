import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PermisoService } from '@core/services/permiso.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-permiso-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './permiso-form.component.html',
  styleUrl: './permiso-form.component.css'
})
export class PermisoFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {
    codigo: '',
    nombre: '',
    modulo: '',
    tipoAcceso: 'LECTURA',
    descripcion: ''
  };

  constructor(
    private permisoService: PermisoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarPermiso(this.editandoId);
    }
  }

  cargarPermiso(id: number) {
    this.permisoService.obtener(id).subscribe(r => {
      this.form = {
        codigo: r.codigo,
        nombre: r.nombre,
        modulo: r.modulo,
        tipoAcceso: r.tipoAcceso || 'LECTURA',
        descripcion: r.descripcion || ''
      };
    });
  }

  volver() {
    this.router.navigate(['/roles']);
  }

  guardar() {
    if (!this.form.codigo || !this.form.nombre || !this.form.modulo) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Código, nombre y módulo son obligatorios.' });
      return;
    }
    this.cargando = true;
    const payload = {
      codigo: this.form.codigo,
      nombre: this.form.nombre,
      modulo: this.form.modulo,
      tipoAcceso: this.form.tipoAcceso || 'LECTURA',
      descripcion: this.form.descripcion || null
    };
    const obs = this.editandoId
      ? this.permisoService.actualizar(this.editandoId, payload)
      : this.permisoService.crear(payload);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Permiso guardado correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el permiso.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}
