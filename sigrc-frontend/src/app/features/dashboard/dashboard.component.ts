import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '@core/services/dashboard.service';
import { Dashboard } from '@shared/models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  data?: Dashboard;
  private maxCount = 1;

  constructor(private svc: DashboardService) {}

  ngOnInit() {
    this.svc.obtener().subscribe(d => {
      this.data = d;
      const all = [...(d.ticketsPorEstado || []), ...(d.ticketsPorPrioridad || [])];
      all.forEach((i: any) => { if (i.cantidad > this.maxCount) this.maxCount = i.cantidad; });
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
