import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-dashboard-th',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard-th.component.html',
  styleUrl: './dashboard-th.component.css'
})
export class DashboardThComponent implements OnInit {
  tab: 'dashboard' | 'matriz' = 'dashboard';
  data: any = null;
  cargando = false;

  empleados: any[] = [];
  idEmpleado: number | null = null;
  matriz: any = null;
  cargandoMatriz = false;

  constructor(private svc: TalentoHumanoService) {}

  ngOnInit() {
    this.cargar();
    this.svc.getEmpleados().subscribe(r => this.empleados = r);
  }

  cambiarTab(t: 'dashboard' | 'matriz') {
    this.tab = t;
  }

  cargar() {
    this.cargando = true;
    this.svc.dashboard().subscribe({
      next: r => {
        this.data = r;
        this.cargando = false;
      },
      error: () => this.cargando = false
    });
  }

  matrizEstado(estado: string): string {
    return estado === 'CUMPLE' ? 'ok' : estado === 'PARCIAL' ? 'parcial' : 'no';
  }

  totalGrupo(grupo: any[]): number {
    return (grupo || []).reduce((s, g) => s + (g.cantidad || 0), 0);
  }

  pct(cantidad: number, total: number): number {
    return total > 0 ? Math.round((cantidad / total) * 100) : 0;
  }

  consultarMatriz() {
    if (!this.idEmpleado) return;
    this.cargandoMatriz = true;
    this.matriz = null;
    this.svc.matrizPersonaPuesto(this.idEmpleado).subscribe({
      next: r => {
        this.matriz = r;
        this.cargandoMatriz = false;
      },
      error: (e) => {
        this.cargandoMatriz = false;
        Swal.fire('Error', e.error?.error || 'No se pudo evaluar la matriz.', 'error');
      }
    });
  }
}
