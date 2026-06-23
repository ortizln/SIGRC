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

  private cargar() {
    this.svc.listar(this.filtros).subscribe(r => {
      this.tickets = r.contenido;
      this.pagina = r;
    });
  }
}
