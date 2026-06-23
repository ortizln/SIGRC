export interface Version {
  idVersion: number;
  version: string;
  tipo: string;
  descripcion: string;
  notasLiberacion: string;
  estado: string;
  ambiente: string;
  fechaDespliegue: string;
  creadoEn: string;
  activo: boolean;
  idSistema: number;
  nombreSistema: string;
  idCambio: number;
  codigoCambio: string;
  idResponsable: number;
  nombreResponsable: string;
}
