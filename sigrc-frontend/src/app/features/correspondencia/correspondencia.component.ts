import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CorrespondenciaService } from '@core/services/correspondencia.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { AuthService } from '@core/services/auth.service';
import { ESTADOS_CORRESPONDENCIA, PRIORIDADES } from '@shared/models/correspondencia.model';

@Component({
  selector: 'app-correspondencia',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './correspondencia.component.html',
  styleUrl: './correspondencia.component.css'
})
export class CorrespondenciaListComponent implements OnInit {
  documentos: any[] = [];
  tiposDocumento: any[] = [];
  usuarios: any[] = [];
  filtros: any = {
    texto: '', estado: '', prioridad: '', idTipoDocumento: '',
    idResponsable: '', fechaDesde: '', fechaHasta: '', pagina: 0, tamanio: 20
  };
  pagina: any = { pagina: 0, totalPaginas: 0, totalElementos: 0, primera: true, ultima: false };
  estados = ESTADOS_CORRESPONDENCIA;
  prioridades = PRIORIDADES;

  constructor(
    private svc: CorrespondenciaService,
    private catSvc: CatalogoService,
    public auth: AuthService
  ) {}

  ngOnInit() {
    const now = new Date();
    this.filtros.fechaHasta = now.toISOString().split('T')[0];
    this.filtros.fechaDesde = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
    this.buscar();
    this.svc.getTiposDocumento().subscribe(r => this.tiposDocumento = r);
  }

  estadoBadge(estado: string): string {
    const map: Record<string, string> = {
      'RECIBIDO': 'nuevo',
      'EN_ANALISIS': 'en-analisis',
      'ASIGNADO': 'asignado',
      'EN_TRAMITE': 'en-desarrollo',
      'PENDIENTE_INFORMACION': 'pendiente-usuario',
      'RESPONDIDO': 'resuelto',
      'ARCHIVADO': 'cerrado'
    };
    return map[estado] || 'nuevo';
  }

  estadoLabel(estado: string): string {
    const e = this.estados.find(x => x.value === estado);
    return e ? e.label : estado;
  }

  prioridadClass(p: string): string {
    return p === 'ALTA' ? 'text-danger' : p === 'MEDIA' ? 'text-warning' : 'text-success';
  }

  buscar() { this.filtros.pagina = 0; this.cargar(); }

  irPagina(p: number) { this.filtros.pagina = p; this.cargar(); }

  private cargar() {
    this.svc.listar(this.filtros).subscribe(r => {
      this.documentos = r.contenido;
      this.pagina = r;
    });
  }
}
