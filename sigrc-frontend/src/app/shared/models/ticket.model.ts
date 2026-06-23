export interface Ticket {
  idTicket: number;
  numeroTicket: string;
  tipo: string;
  estado: string;
  prioridad: string;
  asunto: string;
  descripcion: string;
  impacto: string;
  urgencia: string;
  origen: string;
  fechaLimite: string;
  fechaCierre: string;
  creadoEn: string;
  actualizadoEn: string;
  causaRaiz: string;
  solucion: string;
  esReabierto: boolean;
  numeroReaperturas: number;
  calificacion: number;
  comentarioCierre: string;
  idSolicitante: number;
  solicitanteUsername: string;
  solicitanteNombre: string;
  idArea: number;
  areaNombre: string;
  idSistema: number;
  sistemaNombre: string;
  idCategoria: number;
  categoriaNombre: string;
  idSubcategoria: number;
  subcategoriaNombre: string;
  idResponsable: number;
  responsableUsername: string;
  responsableNombre: string;
  idSla: number;
  slaNombre: string;
  estadoDisplay: string;
}

export interface TicketCrearRequest {
  tipo: string;
  prioridad: string;
  idSolicitante: number;
  idArea: number;
  idSistema?: number;
  idCategoria?: number;
  idSubcategoria?: number;
  asunto: string;
  descripcion: string;
  impacto?: string;
  urgencia?: string;
  origen?: string;
}
