import { Puesto, PuestoFuncion, PuestoFormacion, PuestoExperiencia, PuestoCapacitacion, PuestoProducto, PuestoInterfaz } from './puesto.model';

export interface VersionManual {
  idVersionManual: number;
  nombre: string;
  version: string;
  fechaAprobacion?: string;
  fechaVigencia?: string;
  documentoId?: number;
  estado: 'BORRADOR' | 'VIGENTE' | 'DEROGADO';
  observaciones?: string;
}

export interface VersionManualRequest {
  nombre: string;
  version: string;
  fechaAprobacion?: string | null;
  fechaVigencia?: string | null;
  documentoId?: number | null;
  observaciones?: string | null;
}

export interface ManualFunciones {
  version?: VersionManual | null;
  direcciones: DireccionManual[];
}

export interface DireccionManual {
  idUnidad: number;
  nombre: string;
  sigla?: string;
  tipoUnidad?: string;
  nivelOrganizacional?: string;
  unidades: UnidadManual[];
  puestos: PuestoManual[];
}

export interface UnidadManual {
  idUnidad: number;
  nombre: string;
  sigla?: string;
  tipoUnidad?: string;
  puestos: PuestoManual[];
}

export interface PuestoManual {
  idPuesto: number;
  codigo: string;
  nombre: string;
  rolFuncional?: string;
  eje?: string;
  grupoOcupacional?: string;
  objetivo?: string;
  nivelInstruccion?: string;
  experienciaMeses?: number;
  esJefatura?: boolean;
  esResponsableUnidad?: boolean;
  numeroPlazas?: number;
  vigenteDesde?: string;
  vigenteHasta?: string;
  versionPuesto?: number;
  funciones?: PuestoFuncion[];
  formaciones?: PuestoFormacion[];
  experiencias?: PuestoExperiencia[];
  capacitaciones?: PuestoCapacitacion[];
  productos?: PuestoProducto[];
  interfaces?: PuestoInterfaz[];
}

export { Puesto };
