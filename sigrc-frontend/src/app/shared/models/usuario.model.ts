export interface UsuarioPermiso {
  modulo: string;
  tipoAcceso: string;
}

export interface Usuario {
  idUsuario: number;
  username: string;
  email: string;
  nombres: string;
  apellidos: string;
  nombreCompleto: string;
  cargo: string;
  areaNombre: string;
  idArea: number;
  rolCodigo: string;
  rolNombre: string;
  telefono: string;
  activo: boolean;
  debeCambiarPassword: boolean;
  bloqueado: boolean;
  permisos: UsuarioPermiso[];
}
