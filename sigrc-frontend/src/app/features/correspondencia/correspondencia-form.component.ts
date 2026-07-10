import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CorrespondenciaService } from '@core/services/correspondencia.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { UsuarioService } from '@core/services/usuario.service';
import { TicketService } from '@core/services/ticket.service';
import { AuthService } from '@core/services/auth.service';
import { ESTADOS_CORRESPONDENCIA, PRIORIDADES, SENTIDOS } from '@shared/models/correspondencia.model';

@Component({
  selector: 'app-correspondencia-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './correspondencia-form.component.html',
  styleUrl: './correspondencia-form.component.css'
})
export class CorrespondenciaFormComponent implements OnInit {
  form: any = {
    asunto: '', resumenEjecutivo: '', codigoDocumento: '',
    idTipoDocumento: null, fechaDocumento: '', fechaRecepcion: '',
    horaRecepcion: '', personaEntrega: '', cargo: '', institucion: '',
    departamentoRemitente: '', responsables: [], prioridad: 'MEDIA',
    sentido: 'INGRESO',
    requiereRespuesta: false, fechaLimiteRespuesta: '',
    generaTicket: false, observaciones: '', areasEtiquetadas: [],
    idsReferencias: [], destinatariosSeleccionados: [],
    idTicketVinculado: null
  };
  tiposDocumento: any[] = [];
  areas: any[] = [];
  usuarios: any[] = [];
  responsablesDisponibles: any[] = [];
  destinatariosDisponibles: any[] = [];
  archivos: File[] = [];
  cargando = false;
  errorMessage = '';
  prioridades = PRIORIDADES;
  sentidos = SENTIDOS;
  busquedaReferencia = '';
  documentosReferencia: any[] = [];
  referenciasSeleccionadas: any[] = [];
  ticketsDisponibles: any[] = [];
  private timerBusqueda: any;

  constructor(
    private svc: CorrespondenciaService,
    private catSvc: CatalogoService,
    private usuarioSvc: UsuarioService,
    private ticketSvc: TicketService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.svc.getTiposDocumento().subscribe(r => this.tiposDocumento = r);
    this.catSvc.getAreas().subscribe(r => this.areas = r);
    this.usuarioSvc.listar().subscribe(r => {
      this.usuarios = r;
      this.armarDestinatarios();
    });
    const now = new Date();
    this.form.fechaRecepcion = now.toISOString().split('T')[0];
    this.form.horaRecepcion = now.toTimeString().slice(0, 5);
    this.form.fechaDocumento = now.toISOString().split('T')[0];
    const dentro10Dias = new Date(now);
    dentro10Dias.setDate(dentro10Dias.getDate() + 10);
    this.form.fechaLimiteRespuesta = dentro10Dias.toISOString().split('T')[0];
  }

  armarDestinatarios() {
    this.destinatariosDisponibles = [
      ...this.usuarios.map(u => ({ id: `u${u.idUsuario}`, label: `${u.nombres} ${u.apellidos}`, tipo: 'Usuario' })),
      ...this.areas.map(a => ({ id: `a${a.idArea}`, label: a.nombre, tipo: 'Área' }))
    ];
    this.responsablesDisponibles = this.usuarios.filter((u: any) => u.rolCodigo !== 'ADMIN');
  }

  busquedaResponsable = '';
  sugerenciasResponsable: any[] = [];

  filtrarResponsables() {
    const texto = this.busquedaResponsable?.toLowerCase().trim() || '';
    const idsAsignados = this.form.responsables.map((r: any) => r.idUsuario);
    this.sugerenciasResponsable = this.responsablesDisponibles.filter(
      u => !idsAsignados.includes(u.idUsuario)
        && (!texto || `${u.nombres} ${u.apellidos}`.toLowerCase().includes(texto))
    ).slice(0, 10);
  }

  agregarPrimerResponsable() {
    if (this.sugerenciasResponsable.length > 0) {
      this.agregarResponsable(this.sugerenciasResponsable[0]);
    }
  }

  cerrarSugerenciasResponsable() {
    setTimeout(() => this.sugerenciasResponsable = [], 200);
  }

  agregarResponsable(u: any) {
    const ids = this.form.responsables.map((r: any) => r.idUsuario);
    if (!ids.includes(u.idUsuario)) {
      this.form.responsables.push({ idUsuario: u.idUsuario, sumilla: '' });
    }
    this.busquedaResponsable = '';
    this.sugerenciasResponsable = [];
  }

  removerResponsable(id: number) {
    const idx = this.form.responsables.findIndex((r: any) => r.idUsuario === id);
    if (idx >= 0) this.form.responsables.splice(idx, 1);
  }

  getResponsableNombre(idUsuario: number): string {
    const u = this.responsablesDisponibles.find((x: any) => x.idUsuario === idUsuario);
    return u ? `${u.nombres} ${u.apellidos}` : '—';
  }

  onSentidoChange() {
    if (this.form.sentido === 'SALIDA') {
      this.form.idTicketVinculado = null;
      this.cargarTickets();
    } else {
      this.form.idsReferencias = [];
      this.form.destinatariosSeleccionados = [];
      this.form.personaEntrega = '';
      this.form.idTicketVinculado = null;
    }
  }

  cargarTickets() {
    this.ticketSvc.listar({ pagina: 0, tamanio: 100, sortBy: 'creado_en', sortDir: 'desc' })
      .subscribe(r => this.ticketsDisponibles = r.contenido);
  }

  buscarReferencias() {
    if (this.timerBusqueda) clearTimeout(this.timerBusqueda);
    const texto = this.busquedaReferencia?.trim();
    if (!texto || texto.length < 2) {
      this.documentosReferencia = [];
      return;
    }
    this.timerBusqueda = setTimeout(() => {
      this.svc.listar({ texto, pagina: 0, tamanio: 30, sortBy: 'fecha_recepcion', sortDir: 'desc' })
        .subscribe(r => {
          this.documentosReferencia = r.contenido;
        });
    }, 300);
  }

  toggleReferencia(doc: any) {
    const id = doc.idCorrespondencia;
    const idx = this.form.idsReferencias.indexOf(id);
    if (idx >= 0) {
      this.form.idsReferencias.splice(idx, 1);
      const oidx = this.referenciasSeleccionadas.findIndex(r => r.idCorrespondencia === id);
      if (oidx >= 0) this.referenciasSeleccionadas.splice(oidx, 1);
    } else {
      this.form.idsReferencias.push(id);
      this.referenciasSeleccionadas.push(doc);
    }
  }

  removerReferencia(id: number) {
    const idx = this.form.idsReferencias.indexOf(id);
    if (idx >= 0) this.form.idsReferencias.splice(idx, 1);
    const oidx = this.referenciasSeleccionadas.findIndex(r => r.idCorrespondencia === id);
    if (oidx >= 0) this.referenciasSeleccionadas.splice(oidx, 1);
  }

  busquedaDestinatario = '';
  sugerenciasFiltradas: any[] = [];

  filtrarDestinatarios() {
    const texto = this.busquedaDestinatario?.toLowerCase().trim() || '';
    if (!texto) {
      this.sugerenciasFiltradas = this.destinatariosDisponibles.filter(
        d => !this.form.destinatariosSeleccionados.includes(d.id)
      ).slice(0, 10);
    } else {
      this.sugerenciasFiltradas = this.destinatariosDisponibles.filter(
        d => !this.form.destinatariosSeleccionados.includes(d.id)
          && d.label.toLowerCase().includes(texto)
      ).slice(0, 10);
    }
  }

  agregarPrimerSugerencia() {
    if (this.sugerenciasFiltradas.length > 0) {
      this.agregarDestinatario(this.sugerenciasFiltradas[0]);
    }
  }

  cerrarSugerencias() {
    setTimeout(() => this.sugerenciasFiltradas = [], 200);
  }

  agregarDestinatario(d: any) {
    if (!this.form.destinatariosSeleccionados.includes(d.id)) {
      this.form.destinatariosSeleccionados.push(d.id);
    }
    this.busquedaDestinatario = '';
    this.sugerenciasFiltradas = [];
    this.actualizarPersonaEntrega();
  }

  removerDestinatario(id: string) {
    const idx = this.form.destinatariosSeleccionados.indexOf(id);
    if (idx >= 0) this.form.destinatariosSeleccionados.splice(idx, 1);
    this.actualizarPersonaEntrega();
  }

  getDestinatariosSeleccionados(): any[] {
    return this.form.destinatariosSeleccionados.map((id: string) =>
      this.destinatariosDisponibles.find(d => d.id === id)
    ).filter(Boolean);
  }

  private actualizarPersonaEntrega() {
    this.form.personaEntrega = this.form.destinatariosSeleccionados
      .map((id: string) => {
        const d = this.destinatariosDisponibles.find(x => x.id === id);
        return d ? d.label : '';
      })
      .filter((n: string) => n)
      .join(', ');
  }

  onArchivosSeleccionados(event: any) {
    this.archivos = Array.from(event.target.files || []);
  }

  quitarArchivo(i: number) {
    this.archivos.splice(i, 1);
  }

  toggleArea(id: number) {
    const idx = this.form.areasEtiquetadas.indexOf(id);
    if (idx >= 0) this.form.areasEtiquetadas.splice(idx, 1);
    else this.form.areasEtiquetadas.push(id);
  }

  private buildDestinatarios(): any[] {
    return this.form.destinatariosSeleccionados.map((id: string) => {
      const dest = this.destinatariosDisponibles.find((d: any) => d.id === id);
      const isUser = id.startsWith('u');
      return {
        tipo: isUser ? 'USUARIO' : 'AREA',
        idDestinatario: parseInt(id.substring(1), 10),
        nombre: dest?.label || ''
      };
    });
  }

  guardar() {
    if (this.cargando) return;
    this.cargando = true;
    this.errorMessage = '';

    const body: any = {
      asunto: this.form.asunto,
      resumenEjecutivo: this.form.resumenEjecutivo || null,
      codigoDocumento: this.form.codigoDocumento,
      idTipoDocumento: this.form.idTipoDocumento,
      fechaDocumento: this.form.fechaDocumento,
      fechaRecepcion: this.form.fechaRecepcion,
      horaRecepcion: this.form.horaRecepcion,
      personaEntrega: this.form.personaEntrega || '',
      cargo: this.form.cargo || null,
      institucion: this.form.institucion || null,
      departamentoRemitente: this.form.departamentoRemitente || null,
      responsables: this.form.responsables,
      prioridad: this.form.prioridad,
      sentido: this.form.sentido,
      requiereRespuesta: this.form.requiereRespuesta,
      fechaLimiteRespuesta: this.form.fechaLimiteRespuesta || null,
      generaTicket: this.form.sentido !== 'SALIDA' ? this.form.generaTicket : false,
      observaciones: this.form.observaciones || null,
      areasEtiquetadas: this.form.areasEtiquetadas,
      idsReferencias: this.form.idsReferencias,
      destinatarios: this.form.sentido === 'SALIDA' ? this.buildDestinatarios() : null
    };

    this.svc.crear(body).subscribe({
      next: async r => {
        const id = r.idCorrespondencia;

        for (const f of this.archivos) {
          try { await this.svc.subirAdjunto(id, f, 'ANEXO').toPromise(); } catch (_) {}
        }

        if (this.form.sentido === 'SALIDA' && this.form.idTicketVinculado) {
          this.svc.vincularTicket(id, this.form.idTicketVinculado).subscribe();
        }

        this.router.navigate(['/correspondencia', id]);
      },
      error: (err) => {
        this.cargando = false;
        if (err.error?.errores) {
          this.errorMessage = Object.values(err.error.errores).join('. ');
        } else if (err.error?.mensaje) {
          this.errorMessage = err.error.mensaje;
        } else {
          this.errorMessage = 'Error al guardar el documento. Verifique los datos e intente nuevamente.';
        }
      }
    });
  }
}
