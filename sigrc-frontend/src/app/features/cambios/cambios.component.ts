import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CambioService } from '@core/services/cambio.service';
import { Cambio } from '@shared/models/cambio.model';

@Component({
  selector: 'app-cambios',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cambios.component.html',
  styleUrl: './cambios.component.css'
})
export class CambiosComponent implements OnInit {
  cambios: Cambio[] = [];

  constructor(private svc: CambioService) {}

  ngOnInit() {
    this.svc.listar().subscribe(r => this.cambios = r);
  }

  impactoClass(nivel: string): string {
    const map: Record<string, string> = { 'ALTO': 'bg-danger', 'MEDIO': 'bg-warning text-dark', 'BAJO': 'bg-success' };
    return map[nivel?.toUpperCase()] || 'bg-secondary';
  }

  riesgoClass(nivel: string): string {
    const map: Record<string, string> = { 'ALTO': 'bg-danger', 'MEDIO': 'bg-warning text-dark', 'BAJO': 'bg-success' };
    return map[nivel?.toUpperCase()] || 'bg-secondary';
  }
}
