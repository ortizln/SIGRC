export interface Distributivo {
  idEmpleado: number;
  identificacion: string;
  funcionario: string;
  idUnidad?: number;
  unidad: string;
  idPuesto?: number;
  puesto: string;
  grupoOcupacional?: string;
  tipoRelacion?: string;
  fechaIngreso?: string;
  estadoLaboral?: string;
  tipoPersonal?: string;
}

export interface DashboardTH {
  totalEmpleados: number;
  activos: number;
  desvinculados: number;
  puestosOcupados: number;
  puestosVacantes: number;
  personalVacaciones: number;
  personalLicencia: number;
  capacitacionesRegistradas: number;
  movimientosDelMes: number;
  porUnidad: ItemCount[];
  porPuesto: ItemCount[];
  porGrupoOcupacional: ItemCount[];
  porTipoPersonal: ItemCount[];
  porEstadoLaboral: ItemCount[];
}

export interface ItemCount {
  label: string;
  cantidad: number;
}

export interface MatrizPersonaPuesto {
  idEmpleado: number;
  funcionario: string;
  idPuesto?: number;
  puesto: string;
  unidad?: string;
  grupoOcupacional?: string;
  criterios: CriterioMatriz[];
  cumplidos: number;
  parciales: number;
  noCumplidos: number;
}

export interface CriterioMatriz {
  criterio: string;
  requerido: string;
  encontrado: string;
  estado: 'CUMPLE' | 'PARCIAL' | 'NO_CUMPLE';
}
