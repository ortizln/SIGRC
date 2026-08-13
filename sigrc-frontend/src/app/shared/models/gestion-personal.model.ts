export interface MovimientoPersonal {
  idMovimiento: number;
  idEmpleado: number;
  nombreEmpleado: string;
  tipoMovimiento: string;
  idAsignacionOrigen?: number;
  asignacionOrigenDescripcion?: string;
  idPuestoDestino?: number;
  puestoDestinoNombre?: string;
  idUnidadDestino?: number;
  unidadDestinoNombre?: string;
  fechaSolicitud?: string;
  fechaDesde?: string;
  fechaHasta?: string;
  motivo?: string;
  documentoRespaldoId?: number;
  estado: string;
  creadoPor?: number;
  aprobadoPor?: number;
}

export interface AccionPersonal {
  idAccion: number;
  numero: string;
  idEmpleado: number;
  nombreEmpleado: string;
  tipo: string;
  fechaEmision?: string;
  fechaVigenciaDesde?: string;
  fechaVigenciaHasta?: string;
  motivo?: string;
  situacionActual?: string;
  situacionPropuesta?: string;
  documentoId?: number;
  estado: string;
  elaboradoPor?: number;
  revisadoPor?: number;
  aprobadoPor?: number;
}

export interface SolicitudAusencia {
  idSolicitud: number;
  idEmpleado: number;
  nombreEmpleado: string;
  tipo: string;
  fechaDesde: string;
  fechaHasta: string;
  dias?: number;
  horas?: number;
  motivo?: string;
  documentoRespaldoId?: number;
  estado: string;
  jefeAprobador?: number;
  thAprobador?: number;
}

export const TIPOS_MOVIMIENTO = [
  { value: 'INGRESO', label: 'Ingreso' },
  { value: 'NOMBRAMIENTO', label: 'Nombramiento' },
  { value: 'CONTRATACION', label: 'Contratación' },
  { value: 'TRASLADO', label: 'Traslado' },
  { value: 'TRASPASO', label: 'Traspaso' },
  { value: 'CAMBIO_ADMINISTRATIVO', label: 'Cambio Administrativo' },
  { value: 'ENCARGO', label: 'Encargo' },
  { value: 'SUBROGACION', label: 'Subrogación' },
  { value: 'COMISION_SERVICIOS', label: 'Comisión de Servicios' },
  { value: 'LICENCIA', label: 'Licencia' },
  { value: 'VACACIONES', label: 'Vacaciones' },
  { value: 'REINTEGRO', label: 'Reintegro' },
  { value: 'DESVINCULACION', label: 'Desvinculación' },
  { value: 'JUBILACION', label: 'Jubilación' },
  { value: 'SUPRESION_PUESTO', label: 'Supresión de Puesto' },
  { value: 'OTRO', label: 'Otro' },
];

export const TIPOS_ACCION_PERSONAL = [
  { value: 'NOMBRAMIENTO', label: 'Nombramiento' },
  { value: 'TRASLADO', label: 'Traslado' },
  { value: 'ENCARGO', label: 'Encargo' },
  { value: 'REINTEGRO', label: 'Reintegro' },
  { value: 'DESVINCULACION', label: 'Desvinculación' },
  { value: 'OTRO', label: 'Otro' },
];

export const TIPOS_AUSENCIA = [
  { value: 'VACACION', label: 'Vacaciones' },
  { value: 'PERMISO', label: 'Permiso' },
  { value: 'LICENCIA', label: 'Licencia' },
  { value: 'CALAMIDAD', label: 'Calamidad' },
  { value: 'ENFERMEDAD', label: 'Enfermedad' },
  { value: 'MATERNIDAD', label: 'Maternidad' },
  { value: 'PATERNIDAD', label: 'Paternidad' },
  { value: 'COMISION', label: 'Comisión' },
  { value: 'OTRO', label: 'Otro' },
];

export const ESTADOS_MOVIMIENTO = ['BORRADOR', 'PENDIENTE', 'APROBADA', 'RECHAZADA', 'ANULADA'];
export const ESTADOS_ACCION = ['BORRADOR', 'EN_REVISION', 'APROBADA', 'RECHAZADA', 'ANULADA'];
export const ESTADOS_AUSENCIA = ['PENDIENTE_JEFE', 'PENDIENTE_TH', 'APROBADA', 'RECHAZADA', 'ANULADA'];
