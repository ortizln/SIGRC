import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UsuarioService } from '@core/services/usuario.service';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { Usuario } from '@shared/models/usuario.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent implements OnInit {
  usuarios: Usuario[] = [];
  esAdmin = false;

  // Paginación
  pagina = 0;
  tamanio = 10;
  totalElementos = 0;
  totalPaginas = 0;
  textoBusqueda = '';

  // Para perfil no-admin
  currentUser: Usuario | null = null;

  // Vinculación Usuario <-> Empleado
  vinculoVisible = false;
  vinculoUsuario: Usuario | null = null;
  empleados: any[] = [];
  idEmpleadoSeleccionado: number | null = null;
  cargandoVinculo = false;

  constructor(
    private usuarioService: UsuarioService,
    private talentoHumanoService: TalentoHumanoService,
    public auth: AuthService
  ) {
    this.esAdmin = this.auth.hasRole('ADMIN');
  }

  ngOnInit() {
    const user = this.auth.getUsuario();
    if (this.esAdmin) {
      this.cargar();
    } else if (user?.idUsuario) {
      this.usuarioService.obtener(user.idUsuario).subscribe(u => {
        this.currentUser = u;
      });
    }
  }

  cargar() {
    this.usuarioService.listarPaginado(this.pagina, this.tamanio, this.textoBusqueda || undefined)
      .subscribe(r => {
        this.usuarios = r.contenido;
        this.totalElementos = r.totalElementos;
        this.totalPaginas = r.totalPaginas;
        this.pagina = r.pagina;
      });
  }

  buscar() { this.pagina = 0; this.cargar(); }

  cambiarPagina(p: number) {
    if (p < 0 || p >= this.totalPaginas || p === this.pagina) return;
    this.pagina = p;
    this.cargar();
  }

  get rangoActual(): string {
    if (this.totalElementos === 0) return '0 - 0 de 0';
    const desde = this.pagina * this.tamanio + 1;
    const hasta = Math.min((this.pagina + 1) * this.tamanio, this.totalElementos);
    return `${desde} - ${hasta} de ${this.totalElementos}`;
  }

  get paginasVisibles(): number[] {
    const total = Math.max(this.totalPaginas, 1);
    const inicio = Math.max(0, Math.min(this.pagina - 2, total - 5));
    const fin = Math.min(total, inicio + 5);
    const arr: number[] = [];
    for (let i = inicio; i < fin; i++) arr.push(i);
    return arr;
  }

  eliminar(u: Usuario) {
    Swal.fire({
      title: '¿Desactivar usuario?',
      text: `"${u.username}" quedará inactivo pero sus datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.usuarioService.eliminar(u.idUsuario).subscribe({
        next: () => this.cargar(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar el usuario.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }

  abrirVinculacion(u: Usuario) {
    this.vinculoUsuario = u;
    this.idEmpleadoSeleccionado = u.idEmpleado ?? null;
    this.vinculoVisible = true;
    this.talentoHumanoService.getEmpleados().subscribe({
      next: (emps) => {
        this.empleados = emps;
        if (this.idEmpleadoSeleccionado) {
          const actual = emps.find(e => e.idEmpleado === this.idEmpleadoSeleccionado);
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
  }

  guardarVinculacion() {
    if (!this.vinculoUsuario) return;
    this.cargandoVinculo = true;
    this.usuarioService.vincularEmpleado(this.vinculoUsuario.idUsuario, this.idEmpleadoSeleccionado)
      .subscribe({
        next: (usuarioActualizado) => {
          this.cargandoVinculo = false;
          this.vinculoVisible = false;
          this.vinculoUsuario = null;
          this.cargar();
        },
        error: () => { this.cargandoVinculo = false; }
      });
  }

  desvincular(u: Usuario) {
    Swal.fire({
      title: '¿Desvincular empleado?',
      text: `Se quitará el empleado vinculado de "${u.username}".`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desvincular',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.usuarioService.vincularEmpleado(u.idUsuario, null).subscribe({
        next: () => this.cargar(),
        error: () => {
          Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo desvincular el empleado.' });
        }
      });
    });
  }
}