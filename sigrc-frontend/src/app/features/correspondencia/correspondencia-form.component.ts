import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CorrespondenciaService } from '@core/services/correspondencia.service';
import { CatalogoService } from '@core/services/catalogo.service';
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
    departamentoRemitente: '', idResponsable: null, prioridad: 'MEDIA',
    sentido: 'INGRESO',
    requiereRespuesta: false, fechaLimiteRespuesta: '',
    generaTicket: false, observaciones: '', areasEtiquetadas: [],
    idsReferencias: []
  };
  tiposDocumento: any[] = [];
  areas: any[] = [];
  usuarios: any[] = [];
  archivos: File[] = [];
  cargando = false;
  errorMessage = '';
  prioridades = PRIORIDADES;
  sentidos = SENTIDOS;
  documentosIngreso: any[] = [];

  constructor(
    private svc: CorrespondenciaService,
    private catSvc: CatalogoService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.svc.getTiposDocumento().subscribe(r => this.tiposDocumento = r);
    this.catSvc.getAreas().subscribe(r => this.areas = r);
    const now = new Date();
    this.form.fechaRecepcion = now.toISOString().split('T')[0];
    this.form.horaRecepcion = now.toTimeString().slice(0, 5);
    this.form.fechaDocumento = now.toISOString().split('T')[0];
    const dentro10Dias = new Date(now);
    dentro10Dias.setDate(dentro10Dias.getDate() + 10);
    this.form.fechaLimiteRespuesta = dentro10Dias.toISOString().split('T')[0];
  }

  onSentidoChange() {
    if (this.form.sentido === 'SALIDA') {
      this.cargarDocumentosIngreso();
    } else {
      this.documentosIngreso = [];
      this.form.idsReferencias = [];
    }
  }

  cargarDocumentosIngreso() {
    this.svc.listar({ sentido: 'INGRESO', pagina: 0, tamanio: 200, sortBy: 'fecha_recepcion', sortDir: 'desc' })
      .subscribe(r => this.documentosIngreso = r.contenido);
  }

  toggleReferencia(id: number) {
    const idx = this.form.idsReferencias.indexOf(id);
    if (idx >= 0) this.form.idsReferencias.splice(idx, 1);
    else this.form.idsReferencias.push(id);
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

  guardar() {
    if (this.cargando) return;
    this.cargando = true;
    this.errorMessage = '';
    this.svc.crear(this.form).subscribe({
      next: async r => {
        const id = r.idCorrespondencia;
        for (const f of this.archivos) {
          try { await this.svc.subirAdjunto(id, f, 'ANEXO').toPromise(); } catch (_) {}
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
