import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { DashboardService } from '@core/services/dashboard.service';
import { NotificationService } from '@core/services/notification.service';
import { Dashboard, TicketItem, DocumentoItem } from '@shared/models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  data?: Dashboard;
  private maxCount = 1;
  private sub?: Subscription;

  modalVisible = false;
  modalTitle = '';
  modalTipo: 'ticket' | 'documento' = 'ticket';
  modalItems: any[] = [];
  modalLoading = false;

  constructor(
    private svc: DashboardService,
    private notifSvc: NotificationService,
    private router: Router
  ) {}

  ngOnInit() {
    this.cargar();
    this.sub = this.notifSvc.asignacion$.subscribe(data => {
      if (data) this.cargar();
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  errorCarga = false;

  cargar() {
    this.errorCarga = false;
    this.maxCount = 1;
    this.svc.obtener().subscribe({
      next: d => {
        this.data = d;
        const all = [
          ...(d.ticketsPorEstado || []),
          ...(d.ticketsPorPrioridad || []),
          ...(d.documentosPorEstado || []),
          ...(d.documentosPorPrioridad || []),
          ...(d.cambiosPorEstado || []),
          ...(d.cambiosPorImpacto || []),
          ...(d.tendenciasMensuales || [])
        ];
        all.forEach((i: any) => { if (i.cantidad > this.maxCount) this.maxCount = i.cantidad; });
      },
      error: () => { this.errorCarga = true; }
    });
  }

  barPercent(val: any): number {
    return this.maxCount > 0 ? (Number(val) / this.maxCount) * 100 : 0;
  }

  areaPercent(val: any): string {
    if (!this.data?.ticketsPorArea?.length) return '0';
    const total = this.data.ticketsPorArea.reduce((s: number, i: any) => s + Number(i.cantidad), 0);
    return total > 0 ? ((Number(val) / total) * 100).toFixed(1) : '0';
  }

  // ─── Modal ───

  openModal(tipo: 'ticket' | 'documento', sub: string) {
    this.modalTipo = tipo;
    this.modalLoading = true;
    this.modalVisible = true;
    this.modalItems = [];

    const obs: Observable<any> = tipo === 'ticket'
      ? this.loadTickets(sub)
      : this.loadDocumentos(sub);

    obs.subscribe({
      next: (items: any) => {
        this.modalItems = items;
        this.modalLoading = false;
      },
      error: () => {
        this.modalItems = [];
        this.modalLoading = false;
      }
    });
  }

  private loadTickets(sub: string): Observable<any> {
    switch (sub) {
      case 'abiertos': this.modalTitle = 'Tickets Abiertos'; return this.svc.ticketsAbiertos();
      case 'cerrados': this.modalTitle = 'Tickets Cerrados'; return this.svc.ticketsCerrados();
      case 'vencidos': this.modalTitle = 'Tickets Vencidos'; return this.svc.ticketsVencidos();
      case 'sin-asignar': this.modalTitle = 'Tickets Sin Asignar'; return this.svc.ticketsSinAsignar();
      default: this.modalTitle = ''; return this.svc.ticketsAbiertos();
    }
  }

  private loadDocumentos(sub: string): Observable<any> {
    switch (sub) {
      case 'pendientes': this.modalTitle = 'Documentos Pendientes de Respuesta'; return this.svc.documentosPendientes();
      case 'vencidos': this.modalTitle = 'Documentos Vencidos'; return this.svc.documentosVencidos();
      case 'con-ticket': this.modalTitle = 'Documentos que Generaron Ticket'; return this.svc.documentosConTicket();
      default: this.modalTitle = ''; return this.svc.documentosPendientes();
    }
  }

  closeModal() {
    this.modalVisible = false;
  }

  irATicket(item: TicketItem) {
    this.closeModal();
    this.router.navigate(['/tickets', item.id]);
  }

  irADocumento(item: DocumentoItem) {
    this.closeModal();
    this.router.navigate(['/correspondencia', item.id]);
  }

  irAMemo(memo: any) {
    this.router.navigate(['/correspondencia', memo.id]);
  }

  irADetalle(item: any) {
    if (this.modalTipo === 'ticket') {
      this.irATicket(item);
    } else {
      this.irADocumento(item);
    }
  }

  trackById(_index: number, item: any): number {
    return item.id;
  }

  badgeClass(estado: any): string {
    return 'badge-' + ('' + (estado || '')).toLowerCase().replace(/_/g, '-');
  }
}
