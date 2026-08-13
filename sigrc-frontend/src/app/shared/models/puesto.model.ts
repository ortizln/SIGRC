export interface PuestoFuncion {
  idFuncion?: number;
  descripcion: string;
  tipo?: string;
  orden?: number;
  activo?: boolean;
}

export interface PuestoFormacion {
  idFormacion?: number;
  nivelInstruccion?: string;
  tituloArea?: string;
  detalle?: string;
  obligatorio?: boolean;
}

export interface PuestoExperiencia {
  idExperiencia?: number;
  tiempoMeses?: number;
  especificidad?: string;
  obligatorio?: boolean;
}

export interface PuestoCapacitacion {
  idCapacitacion?: number;
  nombre: string;
  descripcion?: string;
  horasRequeridas?: number;
  obligatorio?: boolean;
}

export interface PuestoProducto {
  idProducto?: number;
  descripcion: string;
  orden?: number;
  activo?: boolean;
}

export interface PuestoInterfaz {
  idInterfaz?: number;
  unidadRelacionadaId?: number;
  descripcion?: string;
  tipoInterfaz?: string;
}

export interface Puesto {
  idPuesto: number;
  codigo: string;
  nombre: string;
  idUnidad?: number;
  unidadNombre?: string;
  rolFuncional?: string;
  eje?: string;
  grupoOcupacional?: string;
  nivelInstruccion?: string;
  experienciaMeses?: number;
  esJefatura?: boolean;
  esResponsableUnidad?: boolean;
  numeroPlazas?: number;
  activo?: boolean;
  vigenteDesde?: string;
  vigenteHasta?: string;
  version?: number;
  idVersionManual?: number;
  objetivo?: string;
  funciones?: PuestoFuncion[];
  formaciones?: PuestoFormacion[];
  experiencias?: PuestoExperiencia[];
  capacitaciones?: PuestoCapacitacion[];
  productos?: PuestoProducto[];
  interfaces?: PuestoInterfaz[];
}

export interface PuestoRequest {
  codigo: string;
  nombre: string;
  idUnidad?: number;
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
  version?: number;
  idVersionManual?: number;
  funciones?: PuestoFuncion[];
  formaciones?: PuestoFormacion[];
  experiencias?: PuestoExperiencia[];
  capacitaciones?: PuestoCapacitacion[];
  productos?: PuestoProducto[];
  interfaces?: PuestoInterfaz[];
}
