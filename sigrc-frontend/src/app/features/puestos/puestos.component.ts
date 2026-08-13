import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-puestos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './puestos.component.html',
  styleUrl: './puestos.component.css'
})
export class PuestosComponent implements OnInit {
  puestos: any[] = [];
  unidades: any[] = [];
  versiones: any[] = [];
  form: any = {};
  formVisible = false;
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  menuAbierto: number | null = null;

  constructor(private svc: TalentoHumanoService, private auth: AuthService) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.cargarPuestos();
    this.cargarUnidades();
    this.cargarVersiones();
  }

  cargarPuestos() { this.svc.getPuestos().subscribe(r => this.puestos = r); }
  cargarUnidades() { this.svc.getUnidades().subscribe(r => this.unidades = r); }
  cargarVersiones() { this.svc.getVersionesManual().subscribe(r => this.versiones = r); }

  toggleMenu(id: number) {
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  nuevo() {
    this.form = {
      esJefatura: false,
      esResponsableUnidad: false,
      numeroPlazas: 1,
      version: 1,
      idVersionManual: null,
      funciones: [],
      formaciones: [],
      experiencias: [],
      capacitaciones: [],
      productos: [],
      interfaces: []
    };
    this.editando = false;
    this.editandoId = null;
    this.formVisible = true;
  }

  editar(p: any) {
    this.svc.getPuestoPerfil(p.idPuesto).subscribe(r => {
      this.form = {
        codigo: r.codigo,
        nombre: r.nombre,
        idUnidad: r.idUnidad || null,
        rolFuncional: r.rolFuncional || '',
        eje: r.eje || '',
        grupoOcupacional: r.grupoOcupacional || '',
        objetivo: r.objetivo || '',
        nivelInstruccion: r.nivelInstruccion || '',
        experienciaMeses: r.experienciaMeses || null,
        esJefatura: !!r.esJefatura,
        esResponsableUnidad: !!r.esResponsableUnidad,
        numeroPlazas: r.numeroPlazas ?? 1,
        vigenteDesde: r.vigenteDesde || '',
        vigenteHasta: r.vigenteHasta || '',
        version: r.version ?? 1,
        idVersionManual: r.idVersionManual || null,
        funciones: r.funciones || [],
        formaciones: r.formaciones || [],
        experiencias: r.experiencias || [],
        capacitaciones: r.capacitaciones || [],
        productos: r.productos || [],
        interfaces: r.interfaces || []
      };
      this.editando = true;
      this.editandoId = p.idPuesto;
      this.formVisible = true;
      this.menuAbierto = null;
    });
  }

  cancelar() {
    this.form = {};
    this.editando = false;
    this.editandoId = null;
    this.formVisible = false;
  }

  agregar(lista: string) {
    if (!this.form[lista]) this.form[lista] = [];
    if (lista === 'funciones') this.form[lista].push({ descripcion: '', tipo: 'ESENCIAL', orden: this.form[lista].length + 1, activo: true });
    else if (lista === 'formaciones') this.form[lista].push({ nivelInstruccion: '', tituloArea: '', detalle: '', obligatorio: true });
    else if (lista === 'experiencias') this.form[lista].push({ tiempoMeses: null, especificidad: '', obligatorio: true });
    else if (lista === 'capacitaciones') this.form[lista].push({ nombre: '', descripcion: '', horasRequeridas: null, obligatorio: true });
    else if (lista === 'productos') this.form[lista].push({ descripcion: '', orden: this.form[lista].length + 1, activo: true });
    else if (lista === 'interfaces') this.form[lista].push({ unidadRelacionadaId: null, descripcion: '', tipoInterfaz: 'INTERNA' });
  }

  quitar(lista: string, idx: number) {
    this.form[lista].splice(idx, 1);
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
      idUnidad: this.form.idUnidad || null,
      rolFuncional: this.form.rolFuncional || null,
      eje: this.form.eje || null,
      grupoOcupacional: this.form.grupoOcupacional || null,
      objetivo: this.form.objetivo || null,
      nivelInstruccion: this.form.nivelInstruccion || null,
      experienciaMeses: this.form.experienciaMeses || null,
      esJefatura: !!this.form.esJefatura,
      esResponsableUnidad: !!this.form.esResponsableUnidad,
      numeroPlazas: this.form.numeroPlazas || 1,
      vigenteDesde: this.form.vigenteDesde || null,
      vigenteHasta: this.form.vigenteHasta || null,
      version: this.form.version || 1,
      idVersionManual: this.form.idVersionManual || null,
      funciones: this.form.funciones || [],
      formaciones: this.form.formaciones || [],
      experiencias: this.form.experiencias || [],
      capacitaciones: this.form.capacitaciones || [],
      productos: this.form.productos || [],
      interfaces: this.form.interfaces || []
    };
    const obs = this.editando && this.editandoId
      ? this.svc.actualizarPuesto(this.editandoId, payload)
      : this.svc.crearPuesto(payload);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        this.cancelar();
        this.cargarPuestos();
        Swal.fire('Guardado', 'Puesto guardado correctamente.', 'success');
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el puesto.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  desactivar(p: any) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `${p.nombre} quedará inactivo pero sus datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      this.svc.eliminarPuesto(p.idPuesto).subscribe({
        next: () => this.cargarPuestos(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar el puesto.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }
}
