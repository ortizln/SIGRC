import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { DireccionManual, UnidadManual, PuestoManual, VersionManual } from '@shared/models/manual-funciones.model';
import { PuestoFuncion } from '@shared/models/puesto.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-manual-funciones',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './manual-funciones.component.html',
  styleUrl: './manual-funciones.component.css'
})
export class ManualFuncionesComponent implements OnInit {
  manual: any = null;
  direccionSel: DireccionManual | null = null;
  unidadSel: UnidadManual | null = null;
  puestoSel: PuestoManual | null = null;

  versiones: VersionManual[] = [];
  cargando = false;

  constructor(private svc: TalentoHumanoService, private auth: AuthService) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  get versionVigente(): VersionManual | null { return this.manual?.version || null; }

  ngOnInit() {
    this.cargar();
    if (this.isAdmin) this.cargarVersiones();
  }

  cargar() {
    this.cargando = true;
    this.svc.getManualFunciones().subscribe({
      next: r => {
        this.manual = r;
        this.cargando = false;
        if (r.direcciones?.length) {
          this.direccionSel = r.direcciones[0];
          if (this.direccionSel.unidades?.length) this.unidadSel = this.direccionSel.unidades[0];
          this.primeraFicha();
        }
      },
      error: () => this.cargando = false
    });
  }

  cargarVersiones() {
    this.svc.getVersionesManual().subscribe(r => this.versiones = r);
  }

  puestosActuales(): PuestoManual[] {
    if (this.unidadSel) return this.unidadSel.puestos || [];
    if (this.direccionSel) return this.direccionSel.puestos || [];
    return [];
  }

  primeraFicha() {
    const lista = this.puestosActuales();
    if (lista.length) this.puestoSel = lista[0];
  }

  seleccionarDireccion(d: DireccionManual) {
    this.direccionSel = d;
    this.unidadSel = d.unidades?.length ? d.unidades[0] : null;
    const lista = this.puestosActuales();
    this.puestoSel = lista.length ? lista[0] : null;
  }

  seleccionarUnidad(u: UnidadManual) {
    this.unidadSel = u;
    this.puestoSel = u.puestos?.length ? u.puestos[0] : null;
  }

  seleccionarPuesto(p: PuestoManual) {
    this.puestoSel = p;
  }

  funcionesOrdenadas(p: PuestoManual | null): PuestoFuncion[] {
    const orden = { ESENCIAL: 0, COMPLEMENTARIA: 1, CONTROL: 2 };
    return (p?.funciones || []).slice().sort((a, b) =>
      ((orden as any)[a.tipo ?? ''] ?? 9) - ((orden as any)[b.tipo ?? ''] ?? 9));
  }

  funcionesPorTipo(p: PuestoManual | null, tipo: string): PuestoFuncion[] {
    return this.funcionesOrdenadas(p).filter(f => (f.tipo || 'ESENCIAL') === tipo);
  }

  etiquetaTipo(tipo: string): string {
    if (tipo === 'ESENCIAL') return 'Esenciales';
    if (tipo === 'COMPLEMENTARIA') return 'Complementarias';
    if (tipo === 'CONTROL') return 'Control';
    return 'Funciones';
  }

  estadoBadge(estado: string): string {
    return estado === 'VIGENTE' ? 'badge-ok' : estado === 'DEROGADO' ? 'badge-no' : 'badge-otro';
  }

  // ─────────── Agregar dirección (unidad raíz) ───────────

  agregarDireccion() {
    Swal.fire({
      title: 'Nueva Dirección',
      html:
        '<div class="text-start">' +
        '<label class="form-label fw-bold">Código <span class="text-danger">*</span></label>' +
        '<input id="sw-codigo" class="form-control mb-2" placeholder="Ej. DIR-001">' +
        '<label class="form-label fw-bold">Nombre <span class="text-danger">*</span></label>' +
        '<input id="sw-nombre" class="form-control mb-2" placeholder="Nombre de la dirección">' +
        '<label class="form-label fw-bold">Sigla</label>' +
        '<input id="sw-sigla" class="form-control mb-2" placeholder="Sigla (opcional)">' +
        '<label class="form-label fw-bold">Tipo</label>' +
        '<select id="sw-tipo" class="form-select">' +
        '<option value="DIRECCION">Dirección</option>' +
        '<option value="GERENCIA">Gerencia</option>' +
        '<option value="COORDINACION">Coordinación</option>' +
        '<option value="UNIDAD">Unidad</option>' +
        '</select>' +
        '</div>',
      showCancelButton: true,
      confirmButtonText: 'Crear',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#4a7c59',
      preConfirm: () => {
        const codigo = (document.getElementById('sw-codigo') as HTMLInputElement)?.value?.trim();
        const nombre = (document.getElementById('sw-nombre') as HTMLInputElement)?.value?.trim();
        const sigla = (document.getElementById('sw-sigla') as HTMLInputElement)?.value?.trim();
        const tipo = (document.getElementById('sw-tipo') as HTMLSelectElement)?.value;
        if (!codigo || !nombre) { Swal.showValidationMessage('Código y nombre son obligatorios'); return null; }
        return { codigo, nombre, sigla: sigla || null, tipoUnidad: tipo };
      }
    }).then(r => {
      if (!r.isConfirmed || !r.value) return;
      this.svc.crearUnidad({
        codigo: r.value.codigo,
        nombre: r.value.nombre,
        sigla: r.value.sigla,
        tipoUnidad: r.value.tipoUnidad,
        idNivel: undefined,
        idUnidadPadre: undefined
      }).subscribe({
        next: () => { Swal.fire('Creado', 'Dirección creada correctamente.', 'success'); this.cargar(); },
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo crear la dirección.', 'error')
      });
    });
  }

  // ─────────── Agregar unidad hija ───────────

  agregarUnidad() {
    if (!this.direccionSel) return;
    Swal.fire({
      title: 'Nueva Unidad',
      html:
        '<div class="text-start">' +
        '<label class="form-label fw-bold">Código <span class="text-danger">*</span></label>' +
        '<input id="sw-codigo" class="form-control mb-2" placeholder="Ej. UNI-001">' +
        '<label class="form-label fw-bold">Nombre <span class="text-danger">*</span></label>' +
        '<input id="sw-nombre" class="form-control mb-2" placeholder="Nombre de la unidad">' +
        '<label class="form-label fw-bold">Sigla</label>' +
        '<input id="sw-sigla" class="form-control mb-2" placeholder="Sigla (opcional)">' +
        '<label class="form-label fw-bold">Tipo</label>' +
        '<select id="sw-tipo" class="form-select">' +
        '<option value="UNIDAD">Unidad</option>' +
        '<option value="JEFATURA">Jefatura</option>' +
        '<option value="COORDINACION">Coordinación</option>' +
        '<option value="SECRETARIA">Secretaría</option>' +
        '<option value="SUBPROCESO">Subproceso</option>' +
        '</select>' +
        '</div>',
      showCancelButton: true,
      confirmButtonText: 'Crear',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#4a7c59',
      preConfirm: () => {
        const codigo = (document.getElementById('sw-codigo') as HTMLInputElement)?.value?.trim();
        const nombre = (document.getElementById('sw-nombre') as HTMLInputElement)?.value?.trim();
        const sigla = (document.getElementById('sw-sigla') as HTMLInputElement)?.value?.trim();
        const tipo = (document.getElementById('sw-tipo') as HTMLSelectElement)?.value;
        if (!codigo || !nombre) { Swal.showValidationMessage('Código y nombre son obligatorios'); return null; }
        return { codigo, nombre, sigla: sigla || null, tipoUnidad: tipo };
      }
    }).then(r => {
      if (!r.isConfirmed || !r.value) return;
      this.svc.crearUnidad({
        codigo: r.value.codigo,
        nombre: r.value.nombre,
        sigla: r.value.sigla,
        tipoUnidad: r.value.tipoUnidad,
        idNivel: undefined,
        idUnidadPadre: this.direccionSel!.idUnidad
      }).subscribe({
        next: () => { Swal.fire('Creado', 'Unidad creada correctamente.', 'success'); this.cargar(); },
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo crear la unidad.', 'error')
      });
    });
  }

  // ─────────── Agregar puesto ───────────

  agregarPuesto() {
    const idUnidad = this.unidadSel?.idUnidad || this.direccionSel?.idUnidad;
    if (!idUnidad) return;
    Swal.fire({
      title: 'Nuevo Puesto',
      html:
        '<div class="text-start">' +
        '<label class="form-label fw-bold">Código <span class="text-danger">*</span></label>' +
        '<input id="sw-codigo" class="form-control mb-2" placeholder="Ej. PTO-001">' +
        '<label class="form-label fw-bold">Nombre <span class="text-danger">*</span></label>' +
        '<input id="sw-nombre" class="form-control mb-2" placeholder="Nombre del puesto">' +
        '<label class="form-label fw-bold">Rol funcional</label>' +
        '<input id="sw-rol" class="form-control mb-2" placeholder="Rol funcional (opcional)">' +
        '<label class="form-label fw-bold">Nº plazas</label>' +
        '<input id="sw-plazas" class="form-control mb-2" type="number" min="1" value="1">' +
        '<div class="form-check mb-1"><input class="form-check-input" type="checkbox" id="sw-jefatura"><label class="form-check-label" for="sw-jefatura"> Jefatura</label></div>' +
        '<div class="form-check"><input class="form-check-input" type="checkbox" id="sw-resp"><label class="form-check-label" for="sw-resp"> Responsable de unidad</label></div>' +
        '</div>',
      showCancelButton: true,
      confirmButtonText: 'Crear',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#4a7c59',
      preConfirm: () => {
        const codigo = (document.getElementById('sw-codigo') as HTMLInputElement)?.value?.trim();
        const nombre = (document.getElementById('sw-nombre') as HTMLInputElement)?.value?.trim();
        const rol = (document.getElementById('sw-rol') as HTMLInputElement)?.value?.trim();
        const plazas = parseInt((document.getElementById('sw-plazas') as HTMLInputElement)?.value || '1', 10);
        const jefatura = (document.getElementById('sw-jefatura') as HTMLInputElement)?.checked;
        const resp = (document.getElementById('sw-resp') as HTMLInputElement)?.checked;
        if (!codigo || !nombre) { Swal.showValidationMessage('Código y nombre son obligatorios'); return null; }
        return { codigo, nombre, rolFuncional: rol || null, numeroPlazas: plazas, esJefatura: jefatura, esResponsableUnidad: resp };
      }
    }).then(r => {
      if (!r.isConfirmed || !r.value) return;
      this.svc.crearPuesto({
        codigo: r.value.codigo,
        nombre: r.value.nombre,
        idUnidad,
        rolFuncional: r.value.rolFuncional,
        esJefatura: r.value.esJefatura,
        esResponsableUnidad: r.value.esResponsableUnidad,
        numeroPlazas: r.value.numeroPlazas,
        funciones: [],
        formaciones: [],
        experiencias: [],
        capacitaciones: [],
        productos: [],
        interfaces: []
      }).subscribe({
        next: () => { Swal.fire('Creado', 'Puesto creado correctamente.', 'success'); this.cargar(); },
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo crear el puesto.', 'error')
      });
    });
  }

  // ─────────── Gestión de versiones ───────────

  aprobar(v: VersionManual) {
    Swal.fire({
      title: '¿Aprobar esta versión?',
      text: `Quedará VIGENTE y la versión vigente actual pasará a DEROGADO.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, aprobar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.aprobarVersionManual(v.idVersionManual).subscribe({
        next: () => { this.cargarVersiones(); this.cargar(); },
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo aprobar.', 'error')
      });
    });
  }

  derogar(v: VersionManual) {
    Swal.fire({
      title: '¿Derogar esta versión?',
      text: 'Dejará de estar vigente pero su información se conserva.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, derogar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.derogarVersionManual(v.idVersionManual).subscribe({
        next: () => { this.cargarVersiones(); this.cargar(); },
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo derogar.', 'error')
      });
    });
  }
}