import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UsuarioService } from '@core/services/usuario.service';
import { RolService } from '@core/services/rol.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { AuthService } from '@core/services/auth.service';
import Swal from 'sweetalert2';

const MODULOS = [
  { clave: 'DASHBOARD', nombre: 'Dashboard' },
  { clave: 'CORRESPONDENCIA', nombre: 'Correspondencia' },
  { clave: 'TICKETS', nombre: 'Tickets' },
  { clave: 'CAMBIOS', nombre: 'Cambios' },
  { clave: 'VERSIONES', nombre: 'Versiones' },
  { clave: 'AUDITORIA', nombre: 'Auditoría' },
  { clave: 'USUARIOS', nombre: 'Usuarios' },
  { clave: 'ROLES', nombre: 'Roles' },
  { clave: 'CATALOGOS', nombre: 'Catálogos' },
  { clave: 'REPORTES', nombre: 'Reportes' },
  { clave: 'CONOCIMIENTO', nombre: 'Base Conocimiento' },
  { clave: 'TALENTO_HUMANO', nombre: 'Talento Humano' },
];

@Component({
  selector: 'app-usuarios-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './usuarios-form.component.html',
  styleUrl: './usuarios-form.component.css'
})
export class UsuariosFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};
  modulos = MODULOS;
  permisosModulo: { [modulo: string]: { lectura: boolean; escritura: boolean } } = {};
  esAdmin = false;
  roles: any[] = [];
  areas: any[] = [];

  constructor(
    private usuarioService: UsuarioService,
    private rolService: RolService,
    private catalogoService: CatalogoService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.esAdmin = this.auth.hasRole('ADMIN');
  }

  get permisosArray(): { modulo: string; lectura: boolean; escritura: boolean }[] {
    return Object.entries(this.permisosModulo).map(([modulo, val]) => ({
      modulo, lectura: val.lectura, escritura: val.escritura
    }));
  }

  ngOnInit() {
    if (this.esAdmin) {
      this.rolService.listar().subscribe(r => this.roles = r);
      this.catalogoService.getAreas().subscribe(r => this.areas = r);
    }
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarUsuario(this.editandoId);
    } else {
      this.form = { username: '', nombres: '', apellidos: '', email: '', password: '', rolCodigo: '', idArea: null, cargo: '', telefono: '' };
      this.initPermisos();
    }
  }

  private initPermisos() {
    this.permisosModulo = {};
    this.modulos.forEach(m => {
      this.permisosModulo[m.clave] = { lectura: false, escritura: false };
    });
  }

  cargarUsuario(id: number) {
    this.usuarioService.obtener(id).subscribe(u => {
      this.form = {
        username: u.username,
        nombres: u.nombres,
        apellidos: u.apellidos,
        email: u.email,
        password: '',
        rolCodigo: u.rolCodigo,
        idArea: u.idArea,
        cargo: u.cargo,
        telefono: u.telefono
      };
      this.initPermisos();
      if (this.esAdmin) {
        this.usuarioService.obtenerPermisos(id).subscribe(permisos => {
          permisos.forEach((p: any) => {
            if (this.permisosModulo[p.modulo]) {
              if (p.tipoAcceso === 'ESCRITURA') {
                this.permisosModulo[p.modulo].lectura = true;
                this.permisosModulo[p.modulo].escritura = true;
              } else {
                this.permisosModulo[p.modulo].lectura = true;
              }
            }
          });
        });
      }
    });
  }

  toggleLectura(clave: string) {
    this.permisosModulo[clave].lectura = !this.permisosModulo[clave].lectura;
    if (!this.permisosModulo[clave].lectura) this.permisosModulo[clave].escritura = false;
  }

  volver() {
    this.router.navigate(['/usuarios']);
  }

  guardar() {
    this.cargando = true;
    const body: any = {};

    if (this.esAdmin) {
      body.username = this.form.username;
      body.email = this.form.email;
      body.nombres = this.form.nombres;
      body.apellidos = this.form.apellidos;
      body.password = this.form.password || null;
      body.rolCodigo = this.form.rolCodigo;
      body.idArea = this.form.idArea || null;
      body.cargo = this.form.cargo;
      body.telefono = this.form.telefono;
    } else {
      body.nombres = this.form.nombres;
      body.apellidos = this.form.apellidos;
      body.email = this.form.email;
      body.password = this.form.password || null;
      body.telefono = this.form.telefono;
    }

    const request = this.editando && this.editandoId
      ? this.usuarioService.actualizar(this.editandoId!, body)
      : this.usuarioService.crear(body);

    const exito = () => {
      this.cargando = false;
      Swal.fire('Guardado', 'Usuario guardado correctamente.', 'success').then(() => this.volver());
    };

    request.subscribe({
      next: (user) => {
        if (this.esAdmin && this.editandoId) {
          const permisos = this.permisosArray.filter(p => p.lectura || p.escritura).map(p => ({
            modulo: p.modulo,
            tipoAcceso: p.escritura ? 'ESCRITURA' : 'LECTURA'
          }));
          this.usuarioService.guardarPermisos(this.editandoId, permisos).subscribe({
            next: () => exito(),
            error: (err) => {
              this.cargando = false;
              const msg = err.error?.error || 'Error al guardar el usuario.';
              Swal.fire({ icon: 'error', title: 'Error', text: msg });
            }
          });
        } else if (!this.esAdmin) {
          const userStorage = this.auth.getUsuario();
          if (userStorage) {
            userStorage.email = user.email;
            userStorage.nombres = user.nombres;
            userStorage.apellidos = user.apellidos;
            localStorage.setItem('sigrc_user', JSON.stringify(userStorage));
          }
          exito();
        } else {
          exito();
        }
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el usuario.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}
