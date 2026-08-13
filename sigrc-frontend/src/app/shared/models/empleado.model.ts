export interface EmpleadoFormacion {
  idFormacion?: number;
  nivel?: string;
  titulo?: string;
  institucion?: string;
  pais?: string;
  fechaInicio?: string;
  fechaFin?: string;
  registroSenescyt?: string;
  documentoId?: number;
  verificado?: boolean;
}

export interface EmpleadoExperiencia {
  idExperiencia?: number;
  institucion?: string;
  cargo?: string;
  fechaInicio?: string;
  fechaFin?: string;
  descripcion?: string;
  documentoId?: number;
}

export interface EmpleadoCapacitacion {
  idCapacitacion?: number;
  nombre: string;
  institucion?: string;
  fechaInicio?: string;
  fechaFin?: string;
  horas?: number;
  tipo?: string;
  certificadoDocumentoId?: number;
}

export interface EmpleadoDocumento {
  idEmpleadoDocumento?: number;
  documentoId?: number;
  tipo?: string;
  fechaDocumento?: string;
  descripcion?: string;
  confidencial?: boolean;
  nivelAcceso?: string;
  nombreArchivo?: string;
  nombreFisico?: string;
  rutaArchivo?: string;
  mimeType?: string;
  tamanoBytes?: number;
  hashSha256?: string;
}

export const NIVELES_ACCESO_DOCUMENTO = [
  { valor: 'PUBLICO_INSTITUCIONAL', etiqueta: 'Público institucional' },
  { valor: 'INTERNO', etiqueta: 'Interno' },
  { valor: 'CONFIDENCIAL_RRHH', etiqueta: 'Confidencial RRHH' },
  { valor: 'RESTRINGIDO', etiqueta: 'Restringido' }
] as const;

export interface Empleado {
  idEmpleado: number;
  tipoIdentificacion?: string;
  identificacion: string;
  nombres: string;
  apellidos: string;
  nombreCompleto: string;
  fechaNacimiento?: string;
  sexo?: string;
  estadoCivil?: string;
  correoPersonal?: string;
  correoInstitucional?: string;
  telefono?: string;
  celular?: string;
  direccion?: string;
  fotoUrl?: string;
  tipoPersonal?: string;
  estadoLaboral?: string;
  fechaIngresoInstitucion?: string;
  fechaSalidaInstitucion?: string;
  observaciones?: string;
  activo?: boolean;
  formaciones?: EmpleadoFormacion[];
  experiencias?: EmpleadoExperiencia[];
  capacitaciones?: EmpleadoCapacitacion[];
  documentos?: EmpleadoDocumento[];
}

export interface EmpleadoRequest {
  tipoIdentificacion?: string;
  identificacion: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento?: string;
  sexo?: string;
  estadoCivil?: string;
  correoPersonal?: string;
  correoInstitucional?: string;
  telefono?: string;
  celular?: string;
  direccion?: string;
  fotoUrl?: string;
  tipoPersonal?: string;
  estadoLaboral?: string;
  fechaIngresoInstitucion?: string;
  fechaSalidaInstitucion?: string;
  observaciones?: string;
  formaciones?: EmpleadoFormacion[];
  experiencias?: EmpleadoExperiencia[];
  capacitaciones?: EmpleadoCapacitacion[];
  documentos?: EmpleadoDocumento[];
}
