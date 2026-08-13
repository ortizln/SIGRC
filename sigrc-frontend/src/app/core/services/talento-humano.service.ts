import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { NivelOrganizacional } from '@shared/models/nivel-organizacional.model';
import { NodoOrganigrama, UnidadOrganizacional, UnidadOrganizacionalRequest } from '@shared/models/unidad-organizacional.model';
import { Puesto, PuestoRequest } from '@shared/models/puesto.model';
import { Empleado, EmpleadoRequest } from '@shared/models/empleado.model';
import { AsignacionPuesto, AsignacionRequest, JefeInfo } from '@shared/models/asignacion.model';
import { Distributivo, DashboardTH, MatrizPersonaPuesto } from '@shared/models/reportes-th.model';
import { ManualFunciones, VersionManual, VersionManualRequest } from '@shared/models/manual-funciones.model';
import { DelegacionFuncion, DelegacionFuncionRequest } from '@shared/models/delegaciones.model';

@Injectable({ providedIn: 'root' })
export class TalentoHumanoService {
  private apiUrl = `${environment.apiUrl}/talento-humano`;

  constructor(private http: HttpClient) {}

  getNiveles(): Observable<NivelOrganizacional[]> {
    return this.http.get<NivelOrganizacional[]>(`${this.apiUrl}/niveles-organizacionales`);
  }
  crearNivel(data: any): Observable<NivelOrganizacional> {
    return this.http.post<NivelOrganizacional>(`${this.apiUrl}/niveles-organizacionales`, data);
  }
  actualizarNivel(id: number, data: any): Observable<NivelOrganizacional> {
    return this.http.put<NivelOrganizacional>(`${this.apiUrl}/niveles-organizacionales/${id}`, data);
  }
  eliminarNivel(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/niveles-organizacionales/${id}`);
  }

  getUnidades(): Observable<UnidadOrganizacional[]> {
    return this.http.get<UnidadOrganizacional[]>(`${this.apiUrl}/unidades`);
  }
  getUnidad(id: number): Observable<UnidadOrganizacional> {
    return this.http.get<UnidadOrganizacional>(`${this.apiUrl}/unidades/${id}`);
  }
  crearUnidad(data: UnidadOrganizacionalRequest): Observable<UnidadOrganizacional> {
    return this.http.post<UnidadOrganizacional>(`${this.apiUrl}/unidades`, data);
  }
  actualizarUnidad(id: number, data: UnidadOrganizacionalRequest): Observable<UnidadOrganizacional> {
    return this.http.put<UnidadOrganizacional>(`${this.apiUrl}/unidades/${id}`, data);
  }
  eliminarUnidad(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/unidades/${id}`);
  }

  getOrganigrama(): Observable<NodoOrganigrama[]> {
    return this.http.get<NodoOrganigrama[]>(`${this.apiUrl}/organigrama`);
  }

  migrarUsuarios(dryRun: boolean): Observable<any> {
    return this.http.post(`${this.apiUrl}/migracion/usuarios`, { dryRun });
  }

  getPuestos(): Observable<Puesto[]> {
    return this.http.get<Puesto[]>(`${this.apiUrl}/puestos`);
  }
  getPuestoPerfil(id: number): Observable<Puesto> {
    return this.http.get<Puesto>(`${this.apiUrl}/puestos/${id}/perfil`);
  }
  crearPuesto(data: PuestoRequest): Observable<Puesto> {
    return this.http.post<Puesto>(`${this.apiUrl}/puestos`, data);
  }
  actualizarPuesto(id: number, data: PuestoRequest): Observable<Puesto> {
    return this.http.put<Puesto>(`${this.apiUrl}/puestos/${id}`, data);
  }
  eliminarPuesto(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/puestos/${id}`);
  }

  getEmpleados(): Observable<Empleado[]> {
    return this.http.get<Empleado[]>(`${this.apiUrl}/empleados`);
  }
  getEmpleadoExpediente(id: number): Observable<Empleado> {
    return this.http.get<Empleado>(`${this.apiUrl}/empleados/${id}/expediente`);
  }

  miExpediente(): Observable<Empleado> {
    return this.http.get<Empleado>(`${this.apiUrl}/mi-expediente`);
  }

  subirArchivoExpediente(idEmpleado: number, idDocumento: number, file: File): Observable<any> {
    const fd = new FormData();
    fd.append('file', file, file.name);
    return this.http.post(`${this.apiUrl}/empleados/${idEmpleado}/documentos/${idDocumento}/archivo`, fd);
  }

  descargarArchivoExpediente(idEmpleado: number, idDocumento: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/empleados/${idEmpleado}/documentos/${idDocumento}/descargar`, { responseType: 'blob' });
  }
  crearEmpleado(data: EmpleadoRequest): Observable<Empleado> {
    return this.http.post<Empleado>(`${this.apiUrl}/empleados`, data);
  }
  actualizarEmpleado(id: number, data: EmpleadoRequest): Observable<Empleado> {
    return this.http.put<Empleado>(`${this.apiUrl}/empleados/${id}`, data);
  }
  eliminarEmpleado(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/empleados/${id}`);
  }

  getAsignaciones(empleadoId: number): Observable<AsignacionPuesto[]> {
    return this.http.get<AsignacionPuesto[]>(`${this.apiUrl}/asignaciones`, { params: { empleadoId } });
  }
  getAsignacionActual(empleadoId: number): Observable<AsignacionPuesto> {
    return this.http.get<AsignacionPuesto>(`${this.apiUrl}/asignaciones/actual`, { params: { empleadoId } });
  }
  asignarPuesto(data: AsignacionRequest): Observable<AsignacionPuesto> {
    return this.http.post<AsignacionPuesto>(`${this.apiUrl}/asignaciones`, data);
  }
  finalizarAsignacion(id: number): Observable<AsignacionPuesto> {
    return this.http.post<AsignacionPuesto>(`${this.apiUrl}/asignaciones/${id}/finalizar`, {});
  }
  getJefeInmediato(idEmpleado: number): Observable<JefeInfo> {
    return this.http.get<JefeInfo>(`${this.apiUrl}/jefatura/${idEmpleado}`);
  }
  asignarResponsableUnidad(idUnidad: number, responsableAsignacionId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/unidades/${idUnidad}/responsable`, { responsableAsignacionId });
  }

  distributivo(filtros: any): Observable<Distributivo[]> {
    let params = new HttpParams();
    if (filtros?.idUnidad) params = params.set('idUnidad', filtros.idUnidad);
    if (filtros?.idPuesto) params = params.set('idPuesto', filtros.idPuesto);
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    if (filtros?.tipoPersonal) params = params.set('tipoPersonal', filtros.tipoPersonal);
    return this.http.get<Distributivo[]>(`${this.apiUrl}/distributivo`, { params });
  }

  exportarDistributivo(filtros: any, formato: 'excel' | 'pdf'): Observable<Blob> {
    let params = new HttpParams().set('formato', formato);
    if (filtros?.idUnidad) params = params.set('idUnidad', filtros.idUnidad);
    if (filtros?.idPuesto) params = params.set('idPuesto', filtros.idPuesto);
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    if (filtros?.tipoPersonal) params = params.set('tipoPersonal', filtros.tipoPersonal);
    return this.http.get(`${this.apiUrl}/distributivo/exportar`, { params, responseType: 'blob' });
  }

  dashboard(): Observable<DashboardTH> {
    return this.http.get<DashboardTH>(`${this.apiUrl}/dashboard`);
  }

  matrizPersonaPuesto(idEmpleado: number): Observable<MatrizPersonaPuesto> {
    return this.http.get<MatrizPersonaPuesto>(`${this.apiUrl}/matriz-persona-puesto/${idEmpleado}`);
  }

  getManualFunciones(): Observable<ManualFunciones> {
    return this.http.get<ManualFunciones>(`${this.apiUrl}/manual-funciones`);
  }
  getVersionesManual(): Observable<VersionManual[]> {
    return this.http.get<VersionManual[]>(`${this.apiUrl}/manual-funciones/versiones`);
  }
  crearVersionManual(data: VersionManualRequest): Observable<VersionManual> {
    return this.http.post<VersionManual>(`${this.apiUrl}/manual-funciones/versiones`, data);
  }
  actualizarVersionManual(id: number, data: VersionManualRequest): Observable<VersionManual> {
    return this.http.put<VersionManual>(`${this.apiUrl}/manual-funciones/versiones/${id}`, data);
  }
  aprobarVersionManual(id: number): Observable<VersionManual> {
    return this.http.post<VersionManual>(`${this.apiUrl}/manual-funciones/versiones/${id}/aprobar`, {});
  }
  derogarVersionManual(id: number): Observable<VersionManual> {
    return this.http.post<VersionManual>(`${this.apiUrl}/manual-funciones/versiones/${id}/derogar`, {});
  }

  getDelegaciones(): Observable<DelegacionFuncion[]> {
    return this.http.get<DelegacionFuncion[]>(`${this.apiUrl}/delegaciones`);
  }
  crearDelegacion(data: DelegacionFuncionRequest): Observable<DelegacionFuncion> {
    return this.http.post<DelegacionFuncion>(`${this.apiUrl}/delegaciones`, data);
  }
  cancelarDelegacion(id: number): Observable<DelegacionFuncion> {
    return this.http.post<DelegacionFuncion>(`${this.apiUrl}/delegaciones/${id}/cancelar`, {});
  }
  finalizarDelegacion(id: number): Observable<DelegacionFuncion> {
    return this.http.post<DelegacionFuncion>(`${this.apiUrl}/delegaciones/${id}/finalizar`, {});
  }
}
