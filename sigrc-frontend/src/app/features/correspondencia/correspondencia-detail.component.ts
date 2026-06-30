import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CorrespondenciaService } from '@core/services/correspondencia.service';
import { AuthService } from '@core/services/auth.service';
import { UsuarioService } from '@core/services/usuario.service';
import { ESTADOS_CORRESPONDENCIA } from '@shared/models/correspondencia.model';

@Component({
  selector: 'app-correspondencia-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './correspondencia-detail.component.html',
  styleUrl: './correspondencia-detail.component.css'
})
export class CorrespondenciaDetailComponent implements OnInit {
  doc?: any;
  adjuntos: any[] = [];
  historial: any[] = [];
  respuestas: any[] = [];
  tickets: any[] = [];
  nuevoEstado = '';
  archivos: File[] = [];
  estados = ESTADOS_CORRESPONDENCIA;
  formRespuesta: any = { fechaRespuesta: '', numeroDocumento: '', idTipoDocumento: null, idResponsable: null, observaciones: '' };
  tiposDocumento: any[] = [];
  usuarios: any[] = [];
  showRespuestaForm = false;

  private user: any;

  constructor(
    private route: ActivatedRoute,
    private svc: CorrespondenciaService,
    private usuarioSvc: UsuarioService,
    public auth: AuthService
  ) {
    this.user = this.auth.getUsuario();
  }

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.svc.obtener(id).subscribe(r => {
        this.doc = r;
        this.adjuntos = r.adjuntos || [];
        this.historial = r.historial || [];
        this.respuestas = r.respuestas || [];
        this.tickets = r.ticketsVinculados || [];
      });
      this.svc.getTiposDocumento().subscribe(r => this.tiposDocumento = r);
      this.usuarioSvc.listar().subscribe(r => this.usuarios = r.filter(u => u.rolCodigo !== 'ADMIN'));
    }
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

  descargarUrl(adj: any): string {
    return this.svc.descargarUrl(this.doc.idCorrespondencia, adj.idAdjunto);
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

  registrarRespuesta() {
    if (!this.doc) return;
    this.formRespuesta.idCorrespondencia = this.doc.idCorrespondencia;
    this.svc.registrarRespuesta(this.doc.idCorrespondencia, this.formRespuesta).subscribe(r => {
      this.respuestas.push(r);
      this.showRespuestaForm = false;
      this.formRespuesta = { fechaRespuesta: '', numeroDocumento: '', idTipoDocumento: null, idResponsable: null, observaciones: '' };
      if (this.doc.estado !== 'ARCHIVADO') {
        this.doc.estado = 'RESPONDIDO';
      }
    });
  }

  asignarResponsable(idResponsable: number) {
    if (!this.doc || !idResponsable) return;
    this.svc.asignarResponsable(this.doc.idCorrespondencia, idResponsable).subscribe(r => {
      this.doc = r;
    });
  }

  generarTicket() {
    if (!this.doc) return;
    this.svc.generarTicket(this.doc.idCorrespondencia).subscribe(r => {
      this.tickets.push(r);
      this.doc.generaTicket = true;
    });
  }

  eliminarAdjunto(adj: any) {
    if (!confirm(`¿Eliminar ${adj.nombreOriginal}?`)) return;
    this.svc.eliminarAdjunto(this.doc.idCorrespondencia, adj.idAdjunto).subscribe(() => {
      this.adjuntos = this.adjuntos.filter(a => a.idAdjunto !== adj.idAdjunto);
    });
  }
}
