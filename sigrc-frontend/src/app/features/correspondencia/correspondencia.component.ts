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
  grupos: any[] = [];
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
  filtrosExpandidos = false;
  bandeja: 'mis' | 'puesto' | 'unidad' | 'pendientes' = 'mis';
  cargandoBandeja = false;
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
      this.filtros.idResponsable = user.idUsuario;
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

  buscar() {
    if (this.bandeja !== 'mis') this.bandeja = 'mis';
    this.filtros.pagina = 0;
    this.cargar();
  }

  irPagina(p: number) { this.filtros.pagina = p; this.cargar(); }

  cambiarTamanio() {
    this.filtros.pagina = 0;
    this.cargar();
  }

  seleccionarBandeja(b: 'mis' | 'puesto' | 'unidad' | 'pendientes') {
    if (this.bandeja === b) return;
    this.bandeja = b;
    if (b === 'mis') {
      this.filtros.pagina = 0;
      this.cargar();
      return;
    }
    this.cargandoBandeja = true;
    const obs = b === 'unidad' ? this.svc.bandejaUnidad()
      : b === 'puesto' ? this.svc.bandejaPuesto()
      : this.svc.pendientes();
    obs.subscribe({
      next: r => {
        this.documentos = r;
        this.cargandoBandeja = false;
        this.pagina = { pagina: 0, totalPaginas: 1, totalElementos: r.length, primera: true, ultima: true };
        this.construirGrupos();
      },
      error: () => {
        this.cargandoBandeja = false;
        this.bandeja = 'mis';
      }
    });
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
    this.filtros.prioridad = '';
    this.filtros.sentido = '';
    this.filtros.idTipoDocumento = '';
    this.filtros.fechaDesde = '';
    this.filtros.fechaHasta = '';
    this.filtros.texto = '';
    this.buscar();
  }

  filtrosActivos(): boolean {
    return !!(this.filtros.estado || this.filtros.prioridad || this.filtros.sentido
      || this.filtros.idTipoDocumento || this.filtros.fechaDesde || this.filtros.fechaHasta);
  }

  filtrosContador(): number {
    let count = 0;
    if (this.filtros.estado) count++;
    if (this.filtros.prioridad) count++;
    if (this.filtros.sentido) count++;
    if (this.filtros.idTipoDocumento) count++;
    if (this.filtros.fechaDesde) count++;
    if (this.filtros.fechaHasta) count++;
    return count;
  }

  responsablesNombres(d: any): string {
    if (!d.responsables || d.responsables.length === 0) return '—';
    return d.responsables.map((r: any) => {
      let nombre = r.nombre;
      if (r.delegacionAplicada && r.usuarioOriginalNombre) {
        nombre += ' [Deleg. de ' + r.usuarioOriginalNombre + ']';
      }
      return nombre;
    }).join(', ');
  }

  esDestinatarioDe(d: any): boolean {
    const user = this.auth.getUsuario();
    if (!user || !d?.destinatarios) return false;
    return d.destinatarios.some((x: any) =>
      x.tipo === 'USUARIO' && x.idDestinatario === user.idUsuario);
  }

  esResponsableDe(d: any): boolean {
    const user = this.auth.getUsuario();
    if (!user || !d?.responsables) return false;
    return d.responsables.some((r: any) => r.idUsuario === user.idUsuario);
  }

  sentidoPercibido(d: any): string {
    const s = d?.sentido;
    if (s !== 'SALIDA') return s || '';
    const user = this.auth.getUsuario();
    if (!user) return s;
    if (d.creadoPor !== user.idUsuario && (this.esDestinatarioDe(d) || this.esResponsableDe(d))) return 'INGRESO';
    return s;
  }

  tieneDelegacion(d: any): boolean {
    return d.responsables?.some((r: any) => r.delegacionAplicada) || false;
  }

  toggleGrupo(g: any) {
    g.expandido = !g.expandido;
  }

  filaClase(d: any): string {
    if (!d?.requiereRespuesta || !d?.fechaLimiteRespuesta) return '';
    if (d.estado === 'RESPONDIDO' || d.estado === 'ARCHIVADO') return '';
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const limite = new Date(d.fechaLimiteRespuesta + 'T00:00:00');
    const diffDias = Math.ceil((limite.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDias < 0) return 'fila-caducada';
    if (diffDias <= 5) return 'fila-proxima';
    return '';
  }

  private cargar() {
    this.svc.listar(this.filtros).subscribe(r => {
      this.documentos = r.contenido;
      this.pagina = r;
      this.construirGrupos();
    });
  }

  private construirGrupos() {
    const docs = this.documentos;
    const idSet = new Set<number>(docs.map(d => d.idCorrespondencia));

    const parent = new Map<number, number>();
    const find = (x: number): number => {
      if (!parent.has(x)) parent.set(x, x);
      while (parent.get(x) !== x) {
        parent.set(x, parent.get(parent.get(x)!)!);
        x = parent.get(x)!;
      }
      return x;
    };
    const union = (a: number, b: number) => {
      const ra = find(a), rb = find(b);
      if (ra !== rb) parent.set(ra, rb);
    };

    for (const d of docs) {
      parent.set(d.idCorrespondencia, d.idCorrespondencia);
      for (const r of d.referencias || []) {
        if (idSet.has(r.idCorrespondencia)) union(d.idCorrespondencia, r.idCorrespondencia);
      }
    }

    const gruposMap = new Map<number, any[]>();
    for (const d of docs) {
      const root = find(d.idCorrespondencia);
      if (!gruposMap.has(root)) gruposMap.set(root, []);
      gruposMap.get(root)!.push(d);
    }

    this.grupos = [];
    gruposMap.forEach(miembros => {
      if (miembros.length === 1) {
        this.grupos.push({ main: miembros[0], hijos: [], expandido: false });
        return;
      }
      const ids = new Set<number>(miembros.map(m => m.idCorrespondencia));
      const referenciadoPor = new Map<number, number>();
      for (const m of miembros) {
        for (const r of m.referencias || []) {
          if (ids.has(r.idCorrespondencia)) {
            referenciadoPor.set(r.idCorrespondencia, (referenciadoPor.get(r.idCorrespondencia) || 0) + 1);
          }
        }
      }
      const main = miembros.find(m =>
        referenciadoPor.has(m.idCorrespondencia) &&
        !(m.referencias || []).some((r: any) => ids.has(r.idCorrespondencia))
      ) || [...miembros].sort((a, b) => (a.fechaRecepcion || '').localeCompare(b.fechaRecepcion || ''))[0];
      const hijos = miembros.filter(m => m.idCorrespondencia !== main.idCorrespondencia);
      this.grupos.push({ main, hijos, expandido: false });
    });

    const orden = new Map<number, number>();
    docs.forEach((d, i) => orden.set(d.idCorrespondencia, i));
    this.grupos.sort((a, b) => (orden.get(a.main.idCorrespondencia) || 0) - (orden.get(b.main.idCorrespondencia) || 0));
  }
}
