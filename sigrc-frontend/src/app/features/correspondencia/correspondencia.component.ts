import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CorrespondenciaService } from '@core/services/correspondencia.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { AuthService } from '@core/services/auth.service';
import { ESTADOS_CORRESPONDENCIA, PRIORIDADES, SENTIDOS } from '@shared/models/correspondencia.model';

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
    idResponsable: '', fechaDesde: '', fechaHasta: '', pagina: 0, tamanio: 20,
    sortBy: 'creado_en', sortDir: 'desc'
  };
  pagina: any = { pagina: 0, totalPaginas: 0, totalElementos: 0, primera: true, ultima: false };
  tamanios = [10, 20, 50];
  estados = ESTADOS_CORRESPONDENCIA;
  prioridades = PRIORIDADES;
  sentidos = SENTIDOS;
  columnas = [
    { key: 'sentido', label: 'Tipo' },
    { key: 'numero_interno', label: 'N° Interno' },
    { key: 'codigo_documento', label: 'Código' },
    { key: 'id_tipo_documento', label: 'Doc.' },
    { key: 'asunto', label: 'Asunto' },
    { key: 'persona_entrega', label: 'Remitente/Dest.' },
    { key: 'prioridad', label: 'Prioridad' },
    { key: 'estado', label: 'Estado' },
    { key: 'id_responsable', label: 'Responsable' },
    { key: 'fecha_recepcion', label: 'Fec. Rec/Env' },
  ];

  constructor(
    private svc: CorrespondenciaService,
    private catSvc: CatalogoService,
    public auth: AuthService
  ) {}

  ngOnInit() {
    const now = new Date();
    this.filtros.fechaHasta = now.toISOString().split('T')[0];
    this.filtros.fechaDesde = new Date(now.getFullYear(), now.getMonth() - 2, 1).toISOString().split('T')[0];
    const user = this.auth.getUsuario();
    if (user && user.rolCodigo !== 'ADMIN') {
      this.filtros.idUsuario = user.idUsuario;
    }
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

  ordenar(columna: string) {
    if (this.filtros.sortBy === columna) {
      this.filtros.sortDir = this.filtros.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.filtros.sortBy = columna;
      this.filtros.sortDir = 'asc';
    }
    this.buscar();
  }

  sortIcon(columna: string): string {
    if (this.filtros.sortBy !== columna) return 'pi pi-sort-alt';
    return this.filtros.sortDir === 'asc' ? 'pi pi-sort-up' : 'pi pi-sort-down';
  }

  buscar() { this.filtros.pagina = 0; this.cargar(); }

  irPagina(p: number) { this.filtros.pagina = p; this.cargar(); }

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

  private cargar() {
    this.svc.listar(this.filtros).subscribe(r => {
      this.documentos = r.contenido;
      this.pagina = r;
    });
  }
}
