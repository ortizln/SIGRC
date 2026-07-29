export interface Dashboard {
  ticketsAbiertos: number;
  ticketsCerrados: number;
  ticketsVencidos: number;
  ticketsSinAsignar: number;
  tiempoPromedioAtencionHoras: number;
  cumplimientoSLA: number;

  totalDocumentos: number;
  pendientesRespuesta: number;
  documentosVencidos: number;
  documentosQueGeneraronTicket: number;
  tiempoPromedioRespuestaHoras: number;

  cambiosSolicitados: number;
  cambiosAprobados: number;
  cambiosCompletados: number;

  versionActual: string;
  sistemaReciente: string;
  ultimoCambioDescripcion: string;
  fechaUltimoCambio: string;

  memosPendientes: MemoItem[];

  ticketsPorEstado: ChartItem[];
  ticketsPorPrioridad: ChartItem[];
  ticketsPorArea: ChartItem[];
  ticketsPorSistema: ChartItem[];
  ticketsPorTipo: ChartItem[];

  documentosPorEstado: ChartItem[];
  documentosPorPrioridad: ChartItem[];

  cambiosPorEstado: ChartItem[];
  cambiosPorImpacto: ChartItem[];

  tendenciasMensuales: ChartItem[];
}

export interface MemoItem {
  id: number;
  numeroInterno: string;
  asunto: string;
  prioridad: string;
  fechaLimite: string;
  codigoDocumento: string;
  departamentoRemitente: string;
}

export interface TicketItem {
  id: number;
  numeroTicket: string;
  asunto: string;
  estado: string;
  prioridad: string;
  tipo: string;
  creadoEn: string;
}

export interface DocumentoItem {
  id: number;
  numeroInterno: string;
  codigoDocumento: string;
  asunto: string;
  estado: string;
  prioridad: string;
  departamentoRemitente: string;
  fechaLimiteRespuesta: string;
  creadoEn: string;
}

export interface ChartItem {
  [key: string]: string | number;
}
