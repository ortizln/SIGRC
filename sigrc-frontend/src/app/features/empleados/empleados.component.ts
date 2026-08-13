import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { NIVELES_ACCESO_DOCUMENTO } from '@shared/models/empleado.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-empleados',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './empleados.component.html',
  styleUrl: './empleados.component.css'
})
export class EmpleadosComponent implements OnInit {
  empleados: any[] = [];
  form: any = {};
  formVisible = false;
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  menuAbierto: number | null = null;

  ver = false;
  expediente: any = null;
  asignacionActual: any = null;
  asignaciones: any[] = [];
  jefe: any = null;
  movimientos: any[] = [];
  acciones: any[] = [];
  ausencias: any[] = [];

  nivelesAcceso = NIVELES_ACCESO_DOCUMENTO;

  constructor(private svc: TalentoHumanoService,
              private gp: GestionPersonalService,
              private auth: AuthService) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }
  get puedeVerExpediente(): boolean {
    return this.isAdmin || this.auth.canModulo('TALENTO_HUMANO', 'LECTURA');
  }
  get columnas(): number { return this.puedeVerExpediente ? 6 : 5; }

  ngOnInit() { this.cargarEmpleados(); }

  cargarEmpleados() { this.svc.getEmpleados().subscribe(r => this.empleados = r); }

  toggleMenu(id: number) {
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  nuevo() {
    this.form = {
      tipoIdentificacion: 'CEDULA',
      sexo: 'M',
      estadoLaboral: 'ACTIVO',
      tipoPersonal: 'SERVIDOR_PUBLICO',
      formaciones: [],
      experiencias: [],
      capacitaciones: [],
      documentos: []
    };
    this.editando = false;
    this.editandoId = null;
    this.formVisible = true;
  }

  editar(e: any) {
    this.svc.getEmpleadoExpediente(e.idEmpleado).subscribe(r => {
      this.form = {
        tipoIdentificacion: r.tipoIdentificacion || 'CEDULA',
        identificacion: r.identificacion,
        nombres: r.nombres,
        apellidos: r.apellidos,
        fechaNacimiento: r.fechaNacimiento || '',
        sexo: r.sexo || 'M',
        estadoCivil: r.estadoCivil || '',
        correoPersonal: r.correoPersonal || '',
        correoInstitucional: r.correoInstitucional || '',
        telefono: r.telefono || '',
        celular: r.celular || '',
        direccion: r.direccion || '',
        tipoPersonal: r.tipoPersonal || 'SERVIDOR_PUBLICO',
        estadoLaboral: r.estadoLaboral || 'ACTIVO',
        fechaIngresoInstitucion: r.fechaIngresoInstitucion || '',
        fechaSalidaInstitucion: r.fechaSalidaInstitucion || '',
        observaciones: r.observaciones || '',
        formaciones: r.formaciones || [],
        experiencias: (r.experiencias || []).map((x: any) => ({
          ...x,
          actualmenteTrabajando: !x.fechaFin
        })),
        capacitaciones: r.capacitaciones || [],
        documentos: (r.documentos || []).map((d: any) => ({
          ...d,
          nivelAcceso: d.nivelAcceso || (d.confidencial ? 'CONFIDENCIAL_RRHH' : 'PUBLICO_INSTITUCIONAL')
        }))
      };
      this.editando = true;
      this.editandoId = e.idEmpleado;
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
    if (lista === 'formaciones') this.form[lista].push({ nivel: '', titulo: '', institucion: '', pais: 'ECUADOR', registroSenescyt: '', fechaInicio: '', fechaFin: '', verificado: false });
    else if (lista === 'experiencias') this.form[lista].push({ institucion: '', cargo: '', descripcion: '', fechaInicio: '', fechaFin: '', actualmenteTrabajando: false });
    else if (lista === 'capacitaciones') this.form[lista].push({ nombre: '', institucion: '', horas: null, tipo: 'CURSO', fechaInicio: '', fechaFin: '' });
    else if (lista === 'documentos') this.form[lista].push({ tipo: 'OTRO', confidencial: false, nivelAcceso: 'PUBLICO_INSTITUCIONAL' });
  }

  quitar(lista: string, idx: number) {
    this.form[lista].splice(idx, 1);
  }

  guardar() {
    if (!this.form.identificacion || !this.form.nombres || !this.form.apellidos) {
      Swal.fire({ icon: 'warning', title: 'Datos incompletos', text: 'Identificación, nombres y apellidos son obligatorios.' });
      return;
    }
    this.cargando = true;
    const payload = {
      tipoIdentificacion: this.form.tipoIdentificacion || 'CEDULA',
      identificacion: this.form.identificacion,
      nombres: this.form.nombres,
      apellidos: this.form.apellidos,
      fechaNacimiento: this.form.fechaNacimiento || null,
      sexo: this.form.sexo || null,
      estadoCivil: this.form.estadoCivil || null,
      correoPersonal: this.form.correoPersonal || null,
      correoInstitucional: this.form.correoInstitucional || null,
      telefono: this.form.telefono || null,
      celular: this.form.celular || null,
      direccion: this.form.direccion || null,
      tipoPersonal: this.form.tipoPersonal || null,
      estadoLaboral: this.form.estadoLaboral || 'ACTIVO',
      fechaIngresoInstitucion: this.form.fechaIngresoInstitucion || null,
      fechaSalidaInstitucion: this.form.fechaSalidaInstitucion || null,
      observaciones: this.form.observaciones || null,
      formaciones: (this.form.formaciones || []).map((f: any) => ({
        ...f,
        fechaInicio: f.fechaInicio || null,
        fechaFin: f.fechaFin || null
      })),
      experiencias: (this.form.experiencias || []).map(({ actualmenteTrabajando, ...e }: any) => ({
        ...e,
        fechaInicio: e.fechaInicio || null,
        fechaFin: actualmenteTrabajando ? null : (e.fechaFin || null)
      })),
      capacitaciones: (this.form.capacitaciones || []).map((c: any) => ({
        ...c,
        fechaInicio: c.fechaInicio || null,
        fechaFin: c.fechaFin || null
      })),
      documentos: this.form.documentos || []
    };
    const obs = this.editando && this.editandoId
      ? this.svc.actualizarEmpleado(this.editandoId, payload)
      : this.svc.crearEmpleado(payload);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        this.cancelar();
        this.cargarEmpleados();
        Swal.fire('Guardado', 'Empleado guardado correctamente.', 'success');
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el empleado.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  desactivar(e: any) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `${e.nombreCompleto} quedará inactivo pero su expediente se conserva.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      this.svc.eliminarEmpleado(e.idEmpleado).subscribe({
        next: () => this.cargarEmpleados(),
        error: (err) => {
          const msg = err.error?.error || 'No se pudo desactivar el empleado.';
          Swal.fire({ icon: 'error', title: 'Error', text: msg });
        }
      });
    });
  }

  subirArchivo(d: any, input: HTMLInputElement) {
    const file = input.files && input.files[0];
    if (!file || !this.editandoId || !d.idEmpleadoDocumento) return;
    this.svc.subirArchivoExpediente(this.editandoId, d.idEmpleadoDocumento, file).subscribe({
      next: () => {
        Swal.fire('Archivo cargado', 'Documento adjuntado al expediente.', 'success');
        if (input) input.value = '';
        this.svc.getEmpleadoExpediente(this.editandoId!).subscribe(r => {
          this.form.documentos = r.documentos?.map((x: any) => ({
            ...x,
            nivelAcceso: x.nivelAcceso || (x.confidencial ? 'CONFIDENCIAL_RRHH' : 'PUBLICO_INSTITUCIONAL')
          })) || [];
        });
      },
      error: (err) => {
        const msg = err.error?.error || 'No se pudo cargar el archivo.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  descargarArchivo(d: any) {
    if (!this.editandoId || !d.idEmpleadoDocumento) return;
    this.svc.descargarArchivoExpediente(this.editandoId, d.idEmpleadoDocumento).subscribe({
      next: blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = d.nombreArchivo || `documento-${d.idEmpleadoDocumento}`;
        a.click();
        URL.revokeObjectURL(a.href);
      },
      error: (err) => {
        const msg = err.error?.error || 'No se pudo descargar el archivo.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  tamanoLegible(bytes?: number): string {
    if (!bytes) return '';
    const unidades = ['B', 'KB', 'MB', 'GB'];
    let i = 0;
    let v = bytes;
    while (v >= 1024 && i < unidades.length - 1) { v /= 1024; i++; }
    return `${v.toFixed(i === 0 ? 0 : 1)} ${unidades[i]}`;
  }

  // ---------- Expediente único (§8) ----------

  verExpediente(e: any) {
    this.ver = true;
    this.menuAbierto = null;
    const id = e.idEmpleado;
    forkJoin({
      expediente: this.svc.getEmpleadoExpediente(id),
      asignacionActual: this.svc.getAsignacionActual(id),
      asignaciones: this.svc.getAsignaciones(id),
      jefe: this.svc.getJefeInmediato(id),
      movimientos: this.gp.listarMovimientos(id),
      acciones: this.gp.listarAcciones(id),
      ausencias: this.gp.listarAusencias(id)
    }).subscribe({
      next: r => {
        this.expediente = r.expediente;
        this.asignacionActual = r.asignacionActual;
        this.asignaciones = r.asignaciones || [];
        this.jefe = r.jefe;
        this.movimientos = r.movimientos || [];
        this.acciones = r.acciones || [];
        this.ausencias = r.ausencias || [];
      },
      error: (err) => {
        this.ver = false;
        const msg = err.error?.error || 'No se pudo cargar el expediente.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  cerrarExpediente() {
    this.ver = false;
    this.expediente = null;
    this.asignacionActual = null;
    this.asignaciones = [];
    this.jefe = null;
    this.movimientos = [];
    this.acciones = [];
    this.ausencias = [];
  }

  editarDesdeExpediente() {
    if (this.expediente) {
      this.editar(this.expediente);
      this.cerrarExpediente();
    }
  }

  tieneDatos(coleccion: any[] | undefined): boolean {
    return !!coleccion && coleccion.length > 0;
  }

  badgeEstado(estado?: string): string {
    const e = (estado || '').toUpperCase();
    if (['ACTIVA', 'APROBADA'].includes(e)) return 'badge-ok';
    if (['PENDIENTE', 'EN_REVISION', 'PENDIENTE_JEFE', 'PENDIENTE_TH', 'BORRADOR'].includes(e)) return 'badge-warn';
    if (['RECHAZADA', 'ANULADA'].includes(e)) return 'badge-danger';
    return 'badge-neutral';
  }
}
