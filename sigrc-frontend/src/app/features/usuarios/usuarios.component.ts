import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '@core/services/usuario.service';
import { RolService } from '@core/services/rol.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { AuthService } from '@core/services/auth.service';
import { Usuario } from '@shared/models/usuario.model';

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
];

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent implements OnInit {
  usuarios: Usuario[] = [];
  roles: any[] = [];
  areas: any[] = [];
  formVisible = false;
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};
  modulos = MODULOS;
  permisosModulo: { [modulo: string]: { lectura: boolean; escritura: boolean } } = {};
  esAdmin = false;

  // Para perfil no-admin
  currentUser: Usuario | null = null;

  constructor(
    private usuarioService: UsuarioService,
    private rolService: RolService,
    private catalogoService: CatalogoService,
    public auth: AuthService
  ) {
    this.esAdmin = this.auth.hasRole('ADMIN');
  }

  ngOnInit() {
    const user = this.auth.getUsuario();
    if (this.esAdmin) {
      this.cargar();
      this.rolService.listar().subscribe(r => this.roles = r);
      this.catalogoService.getAreas().subscribe(r => this.areas = r);
    } else if (user?.idUsuario) {
      this.usuarioService.obtener(user.idUsuario).subscribe(u => {
        this.currentUser = u;
        this.abrirMiPerfil();
      });
    }
  }

  get permisosArray(): { modulo: string; lectura: boolean; escritura: boolean }[] {
    return Object.entries(this.permisosModulo).map(([modulo, val]) => ({
      modulo, lectura: val.lectura, escritura: val.escritura
    }));
  }

  cargar() {
    this.usuarioService.listar().subscribe(r => this.usuarios = r);
  }

  private initPermisos() {
    this.permisosModulo = {};
    this.modulos.forEach(m => {
      this.permisosModulo[m.clave] = { lectura: false, escritura: false };
    });
  }

  abrirFormulario() {
    this.editando = false;
    this.editandoId = null;
    this.form = { username: '', nombres: '', apellidos: '', email: '', password: '', rolCodigo: '', idArea: null, cargo: '', telefono: '' };
    this.initPermisos();
    this.formVisible = true;
  }

  editar(u: Usuario) {
    this.editando = true;
    this.editandoId = u.idUsuario;
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
      this.usuarioService.obtenerPermisos(u.idUsuario).subscribe(permisos => {
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
    this.formVisible = true;
  }

  abrirMiPerfil() {
    if (!this.currentUser) return;
    this.editando = true;
    this.editandoId = this.currentUser.idUsuario;
    this.form = {
      nombres: this.currentUser.nombres,
      apellidos: this.currentUser.apellidos,
      email: this.currentUser.email,
      password: '',
      telefono: this.currentUser.telefono
    };
    this.formVisible = true;
  }

  cancelar() { this.formVisible = false; }

  toggleLectura(clave: string) {
    this.permisosModulo[clave].lectura = !this.permisosModulo[clave].lectura;
    if (!this.permisosModulo[clave].lectura) this.permisosModulo[clave].escritura = false;
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

    request.subscribe({
      next: (user) => {
        if (this.esAdmin && this.editandoId) {
          const permisos = this.permisosArray.filter(p => p.lectura || p.escritura).map(p => ({
            modulo: p.modulo,
            tipoAcceso: p.escritura ? 'ESCRITURA' : 'LECTURA'
          }));
          this.usuarioService.guardarPermisos(this.editandoId, permisos).subscribe(() => {
            this.cargando = false;
            this.formVisible = false;
            this.cargar();
          });
        } else if (!this.esAdmin) {
          this.cargando = false;
          this.formVisible = false;
          this.usuarioService.obtener(this.editandoId!).subscribe(u => this.currentUser = u);
          const userStorage = this.auth.getUsuario();
          userStorage.email = user.email;
          userStorage.nombres = user.nombres;
          userStorage.apellidos = user.apellidos;
          localStorage.setItem('sigrc_user', JSON.stringify(userStorage));
        } else {
          this.cargando = false;
          this.formVisible = false;
          this.cargar();
        }
      },
      error: () => { this.cargando = false; }
    });
  }

  eliminar(u: Usuario) {
    if (!confirm(`¿Desactivar al usuario ${u.username}?`)) return;
    this.usuarioService.eliminar(u.idUsuario).subscribe(() => this.cargar());
  }
}
