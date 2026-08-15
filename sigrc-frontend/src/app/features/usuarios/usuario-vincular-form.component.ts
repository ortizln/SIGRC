import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UsuarioService } from '@core/services/usuario.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-usuario-vincular-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './usuario-vincular-form.component.html',
  styleUrl: './usuario-vincular-form.component.css'
})
export class UsuarioVincularFormComponent implements OnInit {
  usuario: any = null;
  empleados: any[] = [];
  idEmpleadoSeleccionado: number | null = null;
  cargando = false;

  constructor(
    private usuarioService: UsuarioService,
    private talentoHumanoService: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.volver();
      return;
    }
    this.usuarioService.obtener(Number(id)).subscribe({
      next: u => {
        this.usuario = u;
        this.idEmpleadoSeleccionado = u.idEmpleado ?? null;
        this.talentoHumanoService.getEmpleados().subscribe({
          next: (emps) => {
            this.empleados = emps;
            if (this.idEmpleadoSeleccionado) {
              const actual = emps.find((e: any) => e.idEmpleado === this.idEmpleadoSeleccionado);
              if (!actual) {
                this.empleados = [...emps, {
                  idEmpleado: this.idEmpleadoSeleccionado,
                  nombreCompleto: u.empleadoNombre || 'Empleado vinculado',
                }];
              }
            }
          },
          error: () => { this.empleados = []; }
        });
      },
      error: () => this.volver()
    });
  }

  volver() {
    this.router.navigate(['/usuarios']);
  }

  guardar() {
    if (!this.usuario) return;
    if (this.usuario.idEmpleado && this.idEmpleadoSeleccionado === null) {
      Swal.fire({
        title: '¿Desvincular empleado?',
        text: `Se quitará el empleado vinculado de "${this.usuario.username}".`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sí, desvincular',
        cancelButtonText: 'Cancelar'
      }).then(r => {
        if (!r.isConfirmed) return;
        this.guardarVinculacion();
      });
      return;
    }
    this.guardarVinculacion();
  }

  guardarVinculacion() {
    this.cargando = true;
    this.usuarioService.vincularEmpleado(this.usuario.idUsuario, this.idEmpleadoSeleccionado)
      .subscribe({
        next: () => {
          this.cargando = false;
          Swal.fire('Guardado', 'Vinculación actualizada correctamente.', 'success').then(() => this.volver());
        },
        error: (err) => {
          this.cargando = false;
          const msg = err.error?.error || 'No se pudo actualizar la vinculación.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
  }
}