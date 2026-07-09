import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TicketService } from '@core/services/ticket.service';
import { Ticket } from '@shared/models/ticket.model';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './tickets.component.html',
  styleUrl: './tickets.component.css'
})
export class TicketsComponent implements OnInit {
  tickets: Ticket[] = [];
  filtros: any = { texto: '', estado: '', tipo: '', prioridad: '', pagina: 0, tamanio: 20 };
  pagina: any = { pagina: 0, totalPaginas: 0, totalElementos: 0, primera: true, ultima: false };
  tamanios = [10, 20, 50];
  filtrosExpandidos = false;

  constructor(private svc: TicketService) {}

  ngOnInit() { this.buscar(); }

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

  buscar() {
    this.filtros.pagina = 0;
    this.cargar();
  }

  irPagina(p: number) {
    this.filtros.pagina = p;
    this.cargar();
  }

  cambiarTamanio() {
    this.filtros.pagina = 0;
    this.cargar();
  }

  get paginasVisibles(): number[] {
    const total = this.pagina.totalPaginas;
    const actual = this.pagina.pagina;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const paginas: number[] = [0];
    const inicio = Math.max(1, actual - 1);
    const fin = Math.min(total - 2, actual + 1);
    if (inicio > 1) paginas.push(-1);
    for (let i = inicio; i <= fin; i++) paginas.push(i);
    if (fin < total - 2) paginas.push(-1);
    paginas.push(total - 1);
    return paginas;
  }

  limpiarFiltros() {
    this.filtros.estado = '';
    this.filtros.tipo = '';
    this.filtros.prioridad = '';
    this.filtros.texto = '';
    this.buscar();
  }

  filtrosActivos(): boolean {
    return !!(this.filtros.estado || this.filtros.tipo || this.filtros.prioridad);
  }

  filtrosContador(): number {
    let count = 0;
    if (this.filtros.estado) count++;
    if (this.filtros.tipo) count++;
    if (this.filtros.prioridad) count++;
    return count;
  }

  private cargar() {
    this.svc.listar(this.filtros).subscribe(r => {
      this.tickets = r.contenido;
      this.pagina = r;
    });
  }
}
