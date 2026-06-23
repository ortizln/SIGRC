import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '@core/services/usuario.service';
import { RolService } from '@core/services/rol.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { AuthService } from '@core/services/auth.service';
import { Usuario } from '@shared/models/usuario.model';

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

  constructor(
    private usuarioService: UsuarioService,
    private rolService: RolService,
    private catalogoService: CatalogoService,
    public auth: AuthService
  ) {}

  ngOnInit() {
    this.cargar();
    if (this.auth.hasRole('ADMIN')) {
      this.rolService.listar().subscribe(r => this.roles = r);
      this.catalogoService.getAreas().subscribe(r => this.areas = r);
    }
  }

  cargar() {
    this.usuarioService.listar().subscribe(r => this.usuarios = r);
  }

  abrirFormulario() {
    this.editando = false;
    this.editandoId = null;
    this.form = { username: '', nombres: '', apellidos: '', email: '', password: '', rolCodigo: '', idArea: null, cargo: '', telefono: '' };
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
    this.formVisible = true;
  }

  cancelar() { this.formVisible = false; }

  guardar() {
    this.cargando = true;
    const request = this.editando
      ? this.usuarioService.actualizar(this.editandoId!, this.form)
      : this.usuarioService.crear(this.form);
    request.subscribe({
      next: () => { this.cargando = false; this.formVisible = false; this.cargar(); },
      error: () => { this.cargando = false; }
    });
  }

  eliminar(u: Usuario) {
    if (!confirm(`¿Desactivar al usuario ${u.username}?`)) return;
    this.usuarioService.eliminar(u.idUsuario).subscribe(() => this.cargar());
  }
}
