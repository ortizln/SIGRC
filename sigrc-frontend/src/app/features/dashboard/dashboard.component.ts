import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '@core/services/dashboard.service';
import { NotificationService } from '@core/services/notification.service';
import { Dashboard } from '@shared/models/dashboard.model';
import { Subscription } from 'rxjs';

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

  constructor(private svc: DashboardService, private notifSvc: NotificationService) {}

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
    this.svc.obtener().subscribe({
      next: d => {
        this.data = d;
        const all = [...(d.ticketsPorEstado || []), ...(d.ticketsPorPrioridad || [])];
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
}
