import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { CorrespondenciaService } from '@core/services/correspondencia.service';
import { TIPOS_DELEGACION } from '@shared/models/delegaciones.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-delegaciones',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './delegaciones.component.html',
  styleUrl: './delegaciones.component.css'
})
export class DelegacionesComponent implements OnInit {
  delegaciones: any[] = [];
  cargando = false;
  documentosPorDelegacion: Record<number, number> = {};

  tipos = TIPOS_DELEGACION;

  constructor(
    private svc: TalentoHumanoService,
    private correspSvc: CorrespondenciaService
  ) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando = true;
    this.svc.getDelegaciones().subscribe({
      next: r => {
        this.delegaciones = r;
        this.cargando = false;
        this.cargarDocumentosPorDelegacion();
      },
      error: () => this.cargando = false
    });
  }

  cargarDocumentosPorDelegacion() {
    this.correspSvc.getDocumentosPorDelegacion().subscribe({
      next: r => this.documentosPorDelegacion = r,
      error: () => {}
    });
  }

  cantidadDocumentos(d: any): number {
    return this.documentosPorDelegacion[d.idDelegacion] || 0;
  }

  cancelar(d: any) {
    Swal.fire({
      title: '¿Cancelar delegación?',
      text: `${d.empleadoOrigen || ''} ya no estará cubierto por ${d.empleadoDelegado || ''}.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, cancelar',
      cancelButtonText: 'Volver'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.cancelarDelegacion(d.idDelegacion).subscribe({
        next: () => this.cargar(),
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo cancelar.', 'error')
      });
    });
  }

  finalizar(d: any) {
    Swal.fire({
      title: '¿Finalizar delegación?',
      text: 'La delegación quedará marcada como finalizada.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, finalizar',
      cancelButtonText: 'Volver'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.finalizarDelegacion(d.idDelegacion).subscribe({
        next: () => this.cargar(),
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo finalizar.', 'error')
      });
    });
  }

  estadoBadge(estado: string): string {
    return estado === 'ACTIVA' ? 'badge-ok' : estado === 'CANCELADA' ? 'badge-no' : 'badge-otro';
  }

  activaHoy(d: any): boolean {
    if (d.estado !== 'ACTIVA') return false;
    const hoy = new Date();
    const ini = new Date(d.fechaInicio + 'T00:00:00');
    if (d.fechaFin) {
      const fin = new Date(d.fechaFin + 'T00:00:00');
      return ini <= hoy && fin >= hoy;
    }
    return ini <= hoy;
  }

  etiquetaTipo(tipo: string): string {
    const t = this.tipos.find(x => x.valor === tipo);
    return t ? t.etiqueta : (tipo || '—');
  }
}
