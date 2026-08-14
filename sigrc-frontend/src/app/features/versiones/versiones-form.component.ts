import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { VersionService } from '@core/services/version.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { UsuarioService } from '@core/services/usuario.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-versiones-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './versiones-form.component.html',
  styleUrl: './versiones-form.component.css'
})
export class VersionesFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};
  sistemas: any[] = [];
  usuarios: any[] = [];

  constructor(
    private versionService: VersionService,
    private catalogoService: CatalogoService,
    private usuarioSvc: UsuarioService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.cargarSistemas();
    this.cargarUsuarios();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarVersion(this.editandoId);
    } else {
      this.form = { version: '', idSistema: null, tipo: '', estado: 'PENDIENTE', ambiente: 'PRODUCCION', idResponsable: null, descripcion: '', notasLiberacion: '', fechaDespliegue: null };
    }
  }

  cargarSistemas() { this.catalogoService.getSistemas().subscribe(r => this.sistemas = r); }
  cargarUsuarios() { this.usuarioSvc.listar().subscribe(r => this.usuarios = r); }

  cargarVersion(id: number) {
    this.versionService.obtener(id).subscribe(r => {
      this.form = { ...r };
    });
  }

  volver() {
    this.router.navigate(['/versiones']);
  }

  guardar() {
    this.cargando = true;
    const request = this.editandoId
      ? this.versionService.actualizar(this.editandoId, this.form)
      : this.versionService.crear(this.form);
    request.subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Versión guardada correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar la versión.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}
