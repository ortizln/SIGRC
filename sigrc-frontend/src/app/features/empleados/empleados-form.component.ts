import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { NIVELES_ACCESO_DOCUMENTO } from '@shared/models/empleado.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-empleados-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './empleados-form.component.html',
  styleUrl: './empleados-form.component.css'
})
export class EmpleadosFormComponent implements OnInit {
  editando = false;
  editandoId: number | null = null;
  cargando = false;
  form: any = {};

  nivelesAcceso = NIVELES_ACCESO_DOCUMENTO;

  constructor(
    private svc: TalentoHumanoService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editandoId = Number(id);
      this.editando = true;
      this.cargarEmpleado(this.editandoId);
    } else {
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
    }
  }

  cargarEmpleado(id: number) {
    this.svc.getEmpleadoExpediente(id).subscribe(r => {
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
    });
  }

  volver() {
    this.router.navigate(['/talento-humano/empleados']);
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
    const obs = this.editandoId
      ? this.svc.actualizarEmpleado(this.editandoId, payload)
      : this.svc.crearEmpleado(payload);
    obs.subscribe({
      next: () => {
        this.cargando = false;
        Swal.fire('Guardado', 'Empleado guardado correctamente.', 'success').then(() => this.volver());
      },
      error: (err) => {
        this.cargando = false;
        const msg = err.error?.error || 'Error al guardar el empleado.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
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
}