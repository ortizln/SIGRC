export interface Auditoria {
  idAuditoria: number;
  username: string;
  accion: string;
  tipoOperacion: string;
  tablaAfectada: string;
  idRegistro: number;
  datosAnteriores: string;
  datosNuevos: string;
  direccionIp: string;
  userAgent: string;
  resultado: string;
  detalle: string;
  creadoEn: string;
}
