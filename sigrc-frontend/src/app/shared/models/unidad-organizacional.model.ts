export interface UnidadOrganizacional {
  idUnidad: number;
  codigo: string;
  nombre: string;
  sigla: string;
  descripcion: string;
  tipoUnidad: string;
  idNivel: number;
  nivelNombre: string;
  idUnidadPadre: number;
  unidadPadreNombre: string;
  orden: number;
  activo: boolean;
}

export interface NodoOrganigrama {
  idUnidad: number;
  codigo: string;
  nombre: string;
  sigla: string;
  tipoUnidad: string;
  nivelNombre: string;
  orden: number;
  activo: boolean;
  responsable: string;
  puestoResponsable: string;
  plazas: number;
  plazasOcupadas: number;
  vacantes: number;
  hijos: NodoOrganigrama[];
}

export interface UnidadOrganizacionalRequest {
  codigo: string;
  nombre: string;
  sigla?: string;
  descripcion?: string;
  tipoUnidad?: string;
  idNivel?: number;
  idUnidadPadre?: number;
  orden?: number;
}
