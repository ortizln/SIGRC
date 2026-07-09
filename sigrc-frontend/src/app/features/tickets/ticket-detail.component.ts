import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '@core/services/usuario.service';
import { TicketService } from '@core/services/ticket.service';
import { AdjuntoService } from '@core/services/adjunto.service';
import { AuthService } from '@core/services/auth.service';
import { Ticket } from '@shared/models/ticket.model';
import { Adjunto } from '@shared/models/adjunto.model';
import { SafeUrlPipe } from '@shared/pipes/safe-url.pipe';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, SafeUrlPipe],
  templateUrl: './ticket-detail.component.html',
  styleUrl: './ticket-detail.component.css'
})
export class TicketDetailComponent implements OnInit {
  ticket?: Ticket;
  comentarios: any[] = [];
  adjuntos: Adjunto[] = [];
  nuevoEstado = '';
  nuevoComentario = '';
  esInterno = false;
  usuarios: any[] = [];
  idResponsableSeleccionado: number | null = null;

  estados = [
    { value: 'EN_ANALISIS', label: 'En Análisis' },
    { value: 'EN_DESARROLLO', label: 'En Desarrollo' },
    { value: 'EN_PRUEBAS', label: 'En Pruebas' },
    { value: 'PENDIENTE_USUARIO', label: 'Pendiente Usuario' },
    { value: 'RESUELTO', label: 'Resuelto' },
    { value: 'CERRADO', label: 'Cerrado' },
    { value: 'RECHAZADO', label: 'Rechazado' },
  ];

  user: any;

  constructor(
    private route: ActivatedRoute,
    private svc: TicketService,
    private adjuntoSvc: AdjuntoService,
    private auth: AuthService,
    private usuarioSvc: UsuarioService
  ) {
    this.user = this.auth.getUsuario();
  }

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.svc.obtener(id).subscribe(r => { this.ticket = r; });
      this.svc.getComentarios(id).subscribe(r => this.comentarios = r);
      this.adjuntoSvc.listar(id).subscribe(r => this.adjuntos = r);
      this.usuarioSvc.listar().subscribe(r => this.usuarios = r.filter(u => u.rolCodigo !== 'ADMIN'));
    }
  }

  estadoColor(estado: string): string {
    const map: Record<string, string> = {
      'NUEVO': 'primary',
      'ASIGNADO': 'warning',
      'EN_ANALISIS': 'info',
      'EN_DESARROLLO': 'secondary',
      'EN_PRUEBAS': 'danger',
      'PENDIENTE_USUARIO': 'warning',
      'RESUELTO': 'success',
      'CERRADO': 'dark',
      'RECHAZADO': 'danger'
    };
    return map[estado] || 'secondary';
  }

  puedeCambiarEstado(): boolean {
    return this.user && (this.user.rolCodigo === 'ADMIN' || this.user.rolCodigo === 'JEFE_TI' || this.user.rolCodigo === 'TECNICO');
  }

  asignarResponsable() {
    if (!this.ticket || !this.idResponsableSeleccionado) return;
    this.svc.asignar(this.ticket.idTicket, this.idResponsableSeleccionado, this.user.idUsuario)
      .subscribe((r: any) => { this.ticket = r; this.idResponsableSeleccionado = null; });
  }

  actualizarEstado() {
    if (!this.ticket || !this.nuevoEstado) return;
    this.svc.cambiarEstado(this.ticket.idTicket, this.nuevoEstado, this.user.idUsuario)
      .subscribe(r => { this.ticket = r; this.nuevoEstado = ''; });
  }

  esImagen(adj: Adjunto): boolean {
    return adj.tipoMime.startsWith('image/');
  }

  esPdf(adj: Adjunto): boolean {
    return adj.tipoMime === 'application/pdf';
  }

  descargarAdjunto(adj: Adjunto) {
    this.adjuntoSvc.descargar(this.ticket!.idTicket, adj.idAdjunto, adj.nombreArchivo);
  }

  previewAdjunto: any = null;
  previewUrl: string | null = null;

  abrirPreview(adj: Adjunto) {
    this.previewAdjunto = adj;
    this.adjuntoSvc.obtenerBlob(this.ticket!.idTicket, adj.idAdjunto).subscribe(blob => {
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
    a.download = this.previewAdjunto.nombreArchivo;
    a.click();
  }

  tamanoFormateado(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  enviarComentario() {
    if (!this.ticket || !this.nuevoComentario.trim()) return;
    this.svc.addComentario(this.ticket.idTicket, this.user.idUsuario, this.nuevoComentario, this.esInterno)
      .subscribe(r => {
        this.comentarios.unshift(r);
        this.nuevoComentario = '';
        this.esInterno = false;
      });
  }
}
