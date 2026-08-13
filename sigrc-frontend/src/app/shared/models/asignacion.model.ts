export interface AsignacionPuesto {
  idAsignacion: number;
  idEmpleado: number;
  nombreEmpleado: string;
  idPuesto?: number;
  puestoCodigo?: string;
  puestoNombre?: string;
  idUnidad?: number;
  unidadNombre?: string;
  tipoAsignacion?: string;
  fechaInicio?: string;
  fechaFin?: string;
  esPrincipal?: boolean;
  estado?: string;
  observacion?: string;
}

export interface AsignacionRequest {
  idEmpleado: number;
  idPuesto: number;
  idUnidad?: number;
  tipoAsignacion?: string;
  fechaInicio?: string;
  fechaFin?: string;
  observacion?: string;
}

export interface JefeInfo {
  idJefe?: number;
  nombreJefe?: string;
  idPuesto?: number;
  puestoNombre?: string;
  idUnidad?: number;
  unidadNombre?: string;
  tipoAsignacion?: string;
  fechaInicio?: string;
}
