export interface Correspondencia {
  idCorrespondencia: number;
  numeroInterno: string;
  codigoDocumento: string;
  idTipoDocumento: number;
  tipoDocumentoNombre: string;
  tipoDocumentoCodigo: string;
  asunto: string;
  resumenEjecutivo: string;
  fechaDocumento: string;
  fechaRecepcion: string;
  horaRecepcion: string;
  personaEntrega: string;
  cargo: string;
  institucion: string;
  departamentoRemitente: string;
  idResponsable: number;
  responsableNombre: string;
  prioridad: string;
  estado: string;
  sentido: string;
  requiereRespuesta: boolean;
  fechaLimiteRespuesta: string;
  generaTicket: boolean;
  observaciones: string;
  activo: boolean;
  creadoEn: string;
  creadoPor: number;
  creadoPorNombre: string;
  areasEtiquetadas: number[];
  areasEtiquetadasNombre: string[];
  adjuntos: CorrespondenciaAdjunto[];
  historial: CorrespondenciaHistorial[];
  respuestas: CorrespondenciaRespuesta[];
  ticketsVinculados: TicketVinculado[];
  referencias: CorrespondenciaReferencia[];
}

export interface CorrespondenciaCrearRequest {
  asunto: string;
  resumenEjecutivo?: string;
  codigoDocumento: string;
  idTipoDocumento: number;
  fechaDocumento: string;
  fechaRecepcion: string;
  horaRecepcion: string;
  personaEntrega: string;
  cargo?: string;
  institucion?: string;
  departamentoRemitente?: string;
  idResponsable?: number;
  prioridad: string;
  sentido?: string;
  requiereRespuesta?: boolean;
  fechaLimiteRespuesta?: string;
  generaTicket?: boolean;
  observaciones?: string;
  areasEtiquetadas?: number[];
  idsReferencias?: number[];
}

export interface CorrespondenciaReferencia {
  idCorrespondencia: number;
  numeroInterno: string;
  asunto: string;
  codigoDocumento: string;
}

export interface CorrespondenciaAdjunto {
  idAdjunto: number;
  idCorrespondencia: number;
  tipo: string;
  nombreOriginal: string;
  nombreArchivo: string;
  tipoMime: string;
  tamanoBytes: number;
  hashSha256: string;
  idUsuario: number;
  usuarioNombre: string;
  creadoEn: string;
}

export interface CorrespondenciaHistorial {
  idHistorial: number;
  idCorrespondencia: number;
  estadoAnterior: string;
  estadoNuevo: string;
  accion: string;
  idUsuario: number;
  usuarioNombre: string;
  detalle: string;
  creadoEn: string;
}

export interface CorrespondenciaRespuesta {
  idRespuesta: number;
  idCorrespondencia: number;
  fechaRespuesta: string;
  numeroDocumento: string;
  idTipoDocumento: number;
  tipoDocumentoNombre: string;
  idResponsable: number;
  responsableNombre: string;
  observaciones: string;
  creadoEn: string;
}

export interface TicketVinculado {
  idTicket: number;
  numeroTicket: string;
  asunto: string;
  estado: string;
}

export interface Paginacion<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  primera: boolean;
  ultima: boolean;
}

export interface CorrespondenciaDashboard {
  totalDocumentos: number;
  pendientesRespuesta: number;
  respondidos: number;
  vencidos: number;
  queGeneraronTicket: number;
  tiempoPromedioRespuestaHoras: number;
  porEstado: ItemCount[];
  porPrioridad: ItemCount[];
  porTipoDocumento: ItemCount[];
  porDepartamentoRemitente: ItemCount[];
  tendenciasMensuales: TendenciaMensual[];
}

export interface ItemCount {
  label: string;
  cantidad: number;
}

export interface TendenciaMensual {
  mes: string;
  cantidad: number;
}

export const SENTIDOS = [
  { value: 'INGRESO', label: 'Ingreso' },
  { value: 'SALIDA', label: 'Salida' },
];

export const ESTADOS_CORRESPONDENCIA = [
  { value: 'RECIBIDO', label: 'Recibido' },
  { value: 'EN_ANALISIS', label: 'En Análisis' },
  { value: 'ASIGNADO', label: 'Asignado' },
  { value: 'EN_TRAMITE', label: 'En Trámite' },
  { value: 'PENDIENTE_INFORMACION', label: 'Pendiente Información' },
  { value: 'RESPONDIDO', label: 'Respondido' },
  { value: 'ARCHIVADO', label: 'Archivado' },
];

export const PRIORIDADES = [
  { value: 'ALTA', label: 'Alta' },
  { value: 'MEDIA', label: 'Media' },
  { value: 'BAJA', label: 'Baja' },
];

export const TIPOS_ADJUNTO = [
  { value: 'PRINCIPAL', label: 'Documento Principal' },
  { value: 'ANEXO', label: 'Anexo' },
  { value: 'RESPUESTA', label: 'Documento de Respuesta' },
];
