export interface Rol {
  idRol: number;
  codigo: string;
  nombre: string;
  descripcion: string;
  activo: boolean;
  creadoEn: string;
  permisoIds: number[];
}
