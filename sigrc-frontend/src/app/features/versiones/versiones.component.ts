import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VersionService } from '@core/services/version.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { UsuarioService } from '@core/services/usuario.service';
import { AuthService } from '@core/services/auth.service';
import { Version } from '@shared/models/version.model';

@Component({
  selector: 'app-versiones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './versiones.component.html',
  styleUrl: './versiones.component.css'
})
export class VersionesComponent implements OnInit {
  versiones: Version[] = [];
  sistemas: any[] = [];
  usuarios: any[] = [];
  formVisible = false;
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};
  puedeEditar = false;

  constructor(
    private versionService: VersionService,
    private catalogoService: CatalogoService,
    private usuarioSvc: UsuarioService,
    public auth: AuthService
  ) {}

  ngOnInit() {
    this.puedeEditar = this.auth.hasRole('ADMIN') || this.auth.hasRole('JEFE_TI');
    this.cargar();
    this.catalogoService.getSistemas().subscribe(r => this.sistemas = r);
    this.usuarioSvc.listar().subscribe(r => this.usuarios = r);
  }

  cargar() { this.versionService.listar().subscribe(r => this.versiones = r); }

  estadoClass(estado: string): string {
    const map: Record<string, string> = {
      'PENDIENTE': 'bg-warning text-dark',
      'EN_DESARROLLO': 'bg-primary',
      'EN_PRUEBAS': 'bg-info text-dark',
      'DESPLEGADO': 'bg-success',
      'REVERTIDO': 'bg-danger'
    };
    return map[estado] || 'bg-secondary';
  }

  abrirFormulario() {
    this.editando = false; this.editandoId = null;
    this.form = { version: '', idSistema: null, tipo: '', estado: 'PENDIENTE', ambiente: 'PRODUCCION', idResponsable: null, descripcion: '', notasLiberacion: '', fechaDespliegue: null };
    this.formVisible = true;
  }

  editar(v: Version) {
    this.editando = true; this.editandoId = v.idVersion;
    this.form = { ...v };
    this.formVisible = true;
  }

  cancelar() { this.formVisible = false; }

  guardar() {
    this.cargando = true;
    const request = this.editando
      ? this.versionService.actualizar(this.editandoId!, this.form)
      : this.versionService.crear(this.form);
    request.subscribe({
      next: () => { this.cargando = false; this.formVisible = false; this.cargar(); },
      error: () => { this.cargando = false; }
    });
  }

  eliminar(v: Version) {
    if (!confirm(`¿Desactivar versión ${v.version}?`)) return;
    this.versionService.eliminar(v.idVersion).subscribe(() => this.cargar());
  }
}
