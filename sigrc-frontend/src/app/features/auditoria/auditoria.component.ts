import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaService } from '@core/services/auditoria.service';
import { Auditoria } from '@shared/models/auditoria.model';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auditoria.component.html',
  styleUrl: './auditoria.component.css'
})
export class AuditoriaComponent implements OnInit {
  registros: Auditoria[] = [];
  pagina: any = { pagina: 0, totalPaginas: 0, totalElementos: 0, primera: true, ultima: false };
  filtros: any = { pagina: 0, tamanio: 50 };

  constructor(private svc: AuditoriaService) {}

  ngOnInit() { this.cargar(); }

  irPagina(p: number) {
    this.filtros.pagina = p;
    this.cargar();
  }

  private cargar() {
    this.svc.listar(this.filtros).subscribe(r => {
      this.registros = r.contenido;
      this.pagina = r;
    });
  }

  operacionClass(op: string): string {
    const map: Record<string, string> = {
      'CREATE': 'bg-success', 'UPDATE': 'bg-primary', 'DELETE': 'bg-danger',
      'READ': 'bg-info text-dark', 'LOGIN': 'bg-warning text-dark'
    };
    return map[op?.toUpperCase()] || 'bg-secondary';
  }
}
