import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { CorrespondenciaService } from '@core/services/correspondencia.service';
import { AuthService } from '@core/services/auth.service';
import { UsuarioService } from '@core/services/usuario.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { ESTADOS_CORRESPONDENCIA, DelegacionResuelta } from '@shared/models/correspondencia.model';
import { SafeUrlPipe } from '@shared/pipes/safe-url.pipe';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-correspondencia-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, SafeUrlPipe],
  templateUrl: './correspondencia-detail.component.html',
  styleUrl: './correspondencia-detail.component.css'
})
export class CorrespondenciaDetailComponent implements OnInit, OnDestroy {
  doc?: any;
  adjuntos: any[] = [];
  historial: any[] = [];
  respuestas: any[] = [];
  tickets: any[] = [];
  nuevoEstado = '';
  archivos: File[] = [];
  estados = ESTADOS_CORRESPONDENCIA;
  formRespuesta: any = { fechaRespuesta: new Date().toISOString().split('T')[0], idResponsable: null, observaciones: '' };
  tiposDocumento: any[] = [];
  usuarios: any[] = [];
  showRespuestaForm = false;
  delegacionesVigentes: DelegacionResuelta[] = [];

  private user: any;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private svc: CorrespondenciaService,
    private usuarioSvc: UsuarioService,
    private thSvc: TalentoHumanoService,
    public auth: AuthService
  ) {
    this.user = this.auth.getUsuario();
  }

  ngOnInit() {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = Number(params.get('id'));
      if (id) this.cargarDocumento(id);
    });
    this.svc.getTiposDocumento().subscribe(r => this.tiposDocumento = r);
    this.usuarioSvc.listar().subscribe(r => this.usuarios = r.filter(u => u.rolCodigo !== 'ADMIN'));
    this.thSvc.getPuestos().subscribe(r => this.puestos = r);
    this.thSvc.getUnidades().subscribe(r => this.unidades = r);
    this.svc.getDelegacionesVigentes().subscribe(r => this.delegacionesVigentes = r);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private cargarDocumento(id: number) {
    this.svc.marcarLeido(id).subscribe({
      next: r => this.aplicarDTO(r),
      error: () => this.svc.obtener(id).subscribe(r => this.aplicarDTO(r))
    });
  }

  docModal: any = null;

  verDocumento(ref: any) {
    this.svc.obtener(ref.idCorrespondencia).subscribe(r => {
      this.docModal = r;
    });
  }

  cerrarDocModal() {
    this.docModal = null;
  }

  irADocumento() {
    if (!this.docModal) return;
    this.router.navigate(['/correspondencia', this.docModal.idCorrespondencia]);
    this.docModal = null;
  }

  docDestinatariosNombres(): string {
    return this.docModal?.destinatarios?.map((d: any) => d.nombre).join(', ') || '—';
  }

  docResponsablesNombres(): string {
    return this.docModal?.responsables?.map((r: any) => r.nombre).join(', ') || 'Sin asignar';
  }

  tieneDelegacion(usuarioId: number): boolean {
    return this.delegacionesVigentes.some(d => d.idUsuarioDelegado === usuarioId);
  }

  delegacionDe(usuarioId: number): DelegacionResuelta | undefined {
    return this.delegacionesVigentes.find(d => d.idUsuarioDelegado === usuarioId);
  }

  esDelegadoDe(usuarioId: number): boolean {
    return this.delegacionesVigentes.some(d => d.idUsuarioOriginal === usuarioId);
  }

  delegacionHistorialDetalle(h: any): string {
    if (!h.delegacionAplicada) return '';
    const u = this.usuarios.find(x => x.idUsuario === h.idUsuario);
    const nombre = u ? `${u.nombres} ${u.apellidos}` : h.usuarioNombre;
    return ` [Delegación de ${nombre}]`;
  }

  usuarioOriginalDelDoc(): string {
    if (!this.doc?.responsables) return '';
    const user = this.auth.getUsuario();
    if (!user) return '';
    const miDelegacion = this.delegacionesVigentes.find(d => d.idUsuarioDelegado === user.idUsuario);
    if (!miDelegacion) return '';
    return miDelegacion.nombreOriginal;
  }

  private aplicarDTO(r: any) {
    this.doc = r;
    this.adjuntos = r.adjuntos || [];
    this.historial = r.historial || [];
    this.respuestas = r.respuestas || [];
    this.tickets = r.ticketsVinculados || [];
    this.cerrarPreview();
  }

  estadoBadge(estado: string): string {
    const map: Record<string, string> = {
      'RECIBIDO': 'nuevo', 'EN_ANALISIS': 'en-analisis', 'ASIGNADO': 'asignado',
      'EN_TRAMITE': 'en-desarrollo', 'PENDIENTE_INFORMACION': 'pendiente-usuario',
      'RESPONDIDO': 'resuelto', 'ARCHIVADO': 'cerrado'
    };
    return map[estado] || 'nuevo';
  }

  estadoLabel(estado: string): string {
    const e = this.estados.find(x => x.value === estado);
    return e ? e.label : estado;
  }

  prioridadClass(p: string): string {
    return p === 'ALTA' ? 'text-danger' : p === 'MEDIA' ? 'text-warning' : 'text-success';
  }

  esImagen(adj: any): boolean {
    return adj.tipoMime?.startsWith('image/');
  }

  esPdf(adj: any): boolean {
    return adj.tipoMime === 'application/pdf';
  }

  descargarAdjunto(adj: any) {
    this.svc.descargar(this.doc.idCorrespondencia, adj.idAdjunto, adj.nombreOriginal);
  }

  previewAdjunto: any = null;
  previewUrl: string | null = null;

  abrirPreview(adj: any) {
    this.previewAdjunto = adj;
    this.svc.obtenerBlob(this.doc.idCorrespondencia, adj.idAdjunto).subscribe(blob => {
      this.previewUrl = window.URL.createObjectURL(blob);
    });
  }

  cerrarPreview() {
    if (this.previewUrl) window.URL.revokeObjectURL(this.previewUrl);
    this.previewUrl = null;
    this.previewAdjunto = null;
  }

  descargarDesdePreview() {
    if (!this.previewAdjunto || !this.previewUrl) return;
    const a = document.createElement('a');
    a.href = this.previewUrl;
    a.download = this.previewAdjunto.nombreOriginal;
    a.click();
  }

  tamanoFormateado(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  onArchivosSeleccionados(event: any) {
    this.archivos = Array.from(event.target.files || []);
  }

  quitarArchivo(i: number) {
    this.archivos.splice(i, 1);
  }

  subirAdjuntos() {
    for (const f of this.archivos) {
      this.svc.subirAdjunto(this.doc.idCorrespondencia, f, 'ANEXO').subscribe(r => {
        this.adjuntos.push(r);
      });
    }
    this.archivos = [];
  }

  cambiarEstado() {
    if (!this.doc || !this.nuevoEstado) return;
    this.svc.cambiarEstado(this.doc.idCorrespondencia, this.nuevoEstado).subscribe(r => {
      this.doc = r;
      this.nuevoEstado = '';
      this.svc.obtenerHistorial(this.doc.idCorrespondencia).subscribe(h => this.historial = h);
    });
  }

  irAResponder() {
    if (!this.doc) return;
    this.router.navigate(['/correspondencia/nuevo'], {
      queryParams: { respondeA: this.doc.idCorrespondencia }
    });
  }

  registrarRespuesta() {
    if (!this.doc || !this.formRespuesta.idResponsable) return;
    this.formRespuesta.idCorrespondencia = this.doc.idCorrespondencia;
    this.svc.registrarRespuesta(this.doc.idCorrespondencia, this.formRespuesta).subscribe(r => {
      this.respuestas.push(r);
      this.showRespuestaForm = false;
      this.formRespuesta = { fechaRespuesta: new Date().toISOString().split('T')[0], idResponsable: null, observaciones: '' };
      if (this.doc.estado !== 'ARCHIVADO') {
        this.doc.estado = 'RESPONDIDO';
      }
    });
  }

  nuevaSumilla = '';
  usuariosSeleccionados: number[] = [];
  busquedaSumillaUsuarios = '';
  sugerenciasSumilla: any[] = [];

  get usuariosDisponibles(): any[] {
    const asignados = new Set((this.doc?.responsables || []).map((r: any) => r.idUsuario));
    return this.usuarios.filter(u => !asignados.has(u.idUsuario));
  }

  filtrarSumillaUsuarios() {
    const texto = this.busquedaSumillaUsuarios?.toLowerCase().trim() || '';
    this.sugerenciasSumilla = this.usuariosDisponibles
      .filter(u => !this.usuariosSeleccionados.includes(u.idUsuario))
      .filter(u => !texto || `${u.nombres} ${u.apellidos}`.toLowerCase().includes(texto))
      .slice(0, 8);
  }

  cerrarSugerenciasSumilla() {
    setTimeout(() => this.sugerenciasSumilla = [], 200);
  }

  agregarUsuarioSumilla(u: any) {
    if (!this.usuariosSeleccionados.includes(u.idUsuario)) {
      this.usuariosSeleccionados.push(u.idUsuario);
    }
    this.busquedaSumillaUsuarios = '';
    this.sugerenciasSumilla = [];
  }

  quitarUsuarioSumilla(idUsuario: number) {
    const i = this.usuariosSeleccionados.indexOf(idUsuario);
    if (i >= 0) this.usuariosSeleccionados.splice(i, 1);
  }

  nombreUsuarioSumilla(idUsuario: number): string {
    const u = this.usuarios.find(x => x.idUsuario === idUsuario);
    return u ? `${u.nombres} ${u.apellidos}` : '—';
  }

  toggleUsuarioSeleccionado(idUsuario: number) {
    const i = this.usuariosSeleccionados.indexOf(idUsuario);
    if (i >= 0) this.usuariosSeleccionados.splice(i, 1);
    else this.usuariosSeleccionados.push(idUsuario);
  }

  estaSeleccionado(idUsuario: number): boolean {
    return this.usuariosSeleccionados.includes(idUsuario);
  }

  asignarResponsables() {
    if (!this.doc || this.usuariosSeleccionados.length === 0) return;
    const ids = [...this.usuariosSeleccionados];
    const sumilla = this.nuevaSumilla;
    let pendientes = ids.length;
    let resultado: any = null;
    ids.forEach((id, idx) => {
      this.svc.asignarResponsable(this.doc.idCorrespondencia, id, sumilla).subscribe(r => {
        resultado = r;
        pendientes--;
        if (pendientes === 0) {
          this.doc = resultado;
          this.usuariosSeleccionados = [];
          this.nuevaSumilla = '';
          this.busquedaSumillaUsuarios = '';
          Swal.fire('Sumillado', `Documento sumillado y derivado a ${ids.length} usuario(s).`, 'success');
        }
      });
    });
  }

  asignarResponsable(idResponsable: number) {
    if (!this.doc || !idResponsable) return;
    this.svc.asignarResponsable(this.doc.idCorrespondencia, idResponsable, this.nuevaSumilla).subscribe(r => {
      this.doc = r;
      this.nuevaSumilla = '';
    });
  }

  puestos: any[] = [];
  unidades: any[] = [];
  showDerivarInstitucional = false;
  derivarSumilla = '';
  derivarTipo = '';
  derivarIdDestino: number | null = null;
  derivandoInstitucional = false;

  toggleDerivarInstitucional() {
    this.showDerivarInstitucional = !this.showDerivarInstitucional;
  }

  derivarInstitucional() {
    if (!this.doc || !this.derivarTipo || this.derivandoInstitucional) return;
    const destinos = [{
      tipo: this.derivarTipo,
      idDestino: this.derivarIdDestino
    }];
    this.derivandoInstitucional = true;
    this.svc.derivarInstitucional(this.doc.idCorrespondencia, this.derivarSumilla, destinos).subscribe({
      next: r => {
        this.doc = r;
        this.derivandoInstitucional = false;
        this.derivarSumilla = '';
        this.derivarIdDestino = null;
        this.showDerivarInstitucional = false;
        Swal.fire('Derivado', 'El documento fue derivado según la estructura institucional.', 'success');
      },
      error: () => {
        this.derivandoInstitucional = false;
        Swal.fire('Error', 'No se pudo derivar el documento.', 'error');
      }
    });
  }

  get responsablesNombres(): string {
    return this.doc?.responsables?.map((r: any) => r.nombre).join(', ') || 'Sin asignar';
  }

  get esDestinatario(): boolean {
    const u = this.auth.getUsuario();
    if (!u || !this.doc?.destinatarios) return false;
    return this.doc.destinatarios.some((d: any) =>
      d.tipo === 'USUARIO' && d.idDestinatario === u.idUsuario);
  }

  get miRegistroDestinatario(): any {
    const u = this.auth.getUsuario();
    if (!u || !this.doc?.destinatarios) return null;
    return this.doc.destinatarios.find((d: any) =>
      d.tipo === 'USUARIO' && d.idDestinatario === u.idUsuario) || null;
  }

  get esDestinatarioPendiente(): boolean {
    const r = this.miRegistroDestinatario;
    return !!r && !r.recibido;
  }

  sentidoPercibido(): string {
    const s = this.doc?.sentido;
    if (s !== 'SALIDA') return s || '';
    const u = this.auth.getUsuario();
    if (!u) return s;
    if (this.esDestinatario && this.doc?.creadoPor !== u.idUsuario) return 'INGRESO';
    return s;
  }

  marcarRecibido() {
    if (!this.doc) return;
    this.svc.marcarRecibido(this.doc.idCorrespondencia).subscribe(r => {
      this.doc = r;
      Swal.fire('Recibido', 'Has marcado el documento como recibido.', 'success');
    });
  }

  generarTicket() {
    if (!this.doc) return;
    this.svc.generarTicket(this.doc.idCorrespondencia).subscribe(r => {
      this.tickets.push(r);
      this.doc.generaTicket = true;
    });
  }

  eliminarDocumento() {
    if (!this.doc) return;
    Swal.fire({
      title: '¿Anular documento?',
      text: `El documento ${this.doc.numeroInterno} quedará inactivo pero se conservará como evidencia.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, anular',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.svc.eliminar(this.doc.idCorrespondencia).subscribe(() => {
          Swal.fire('Anulado', 'El documento ha sido anulado correctamente.', 'success');
          this.router.navigate(['/correspondencia']);
        });
      }
    });
  }

  eliminarAdjunto(adj: any) {
    if (!confirm(`¿Eliminar ${adj.nombreOriginal}?`)) return;
    this.svc.eliminarAdjunto(this.doc.idCorrespondencia, adj.idAdjunto).subscribe(() => {
      this.adjuntos = this.adjuntos.filter(a => a.idAdjunto !== adj.idAdjunto);
    });
  }
}
