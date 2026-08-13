export interface DelegacionFuncion {
  idDelegacion: number;
  idAsignacionOrigen: number;
  idEmpleadoOrigen?: number;
  empleadoOrigen?: string;
  puestoOrigen?: string;
  unidadOrigen?: string;
  idAsignacionDelegada: number;
  idEmpleadoDelegado?: number;
  empleadoDelegado?: string;
  puestoDelegado?: string;
  unidadDelegada?: string;
  fechaInicio: string;
  fechaFin?: string;
  tipo?: string;
  alcance?: string;
  documentoRespaldoId?: number;
  estado: 'ACTIVA' | 'CANCELADA' | 'FINALIZADA';
  observacion?: string;
  creadoPor?: number;
}

export interface DelegacionFuncionRequest {
  idAsignacionOrigen: number | null;
  idAsignacionDelegada: number | null;
  fechaInicio: string | null;
  fechaFin?: string | null;
  tipo?: string | null;
  alcance?: string | null;
  documentoRespaldoId?: number | null;
  observacion?: string | null;
}

export const TIPOS_DELEGACION = [
  { valor: 'VACACIONES', etiqueta: 'Vacaciones' },
  { valor: 'PERMISO', etiqueta: 'Permiso' },
  { valor: 'LICENCIA', etiqueta: 'Licencia' },
  { valor: 'ENCARGO', etiqueta: 'Encargo' },
  { valor: 'COMISION', etiqueta: 'Comisión de servicios' },
  { valor: 'AUSENCIA', etiqueta: 'Ausencia' },
  { valor: 'OTRO', etiqueta: 'Otro' }
];

export const ALCANCES_DELEGACION = [
  { valor: 'TOTAL', etiqueta: 'Total' },
  { valor: 'PARCIAL', etiqueta: 'Parcial' }
];
