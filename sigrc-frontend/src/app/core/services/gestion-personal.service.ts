import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { MovimientoPersonal, AccionPersonal, SolicitudAusencia } from '@shared/models/gestion-personal.model';

@Injectable({ providedIn: 'root' })
export class GestionPersonalService {
  private apiUrl = `${environment.apiUrl}/talento-humano`;

  constructor(private http: HttpClient) {}

  // ───── Movimientos ─────

  listarMovimientos(idEmpleado?: number | null, estado?: string): Observable<MovimientoPersonal[]> {
    let params = new HttpParams();
    if (idEmpleado) params = params.set('idEmpleado', idEmpleado);
    if (estado) params = params.set('estado', estado);
    return this.http.get<MovimientoPersonal[]>(`${this.apiUrl}/movimientos`, { params });
  }

  crearMovimiento(data: any): Observable<MovimientoPersonal> {
    return this.http.post<MovimientoPersonal>(`${this.apiUrl}/movimientos`, data);
  }

  actualizarMovimiento(id: number, data: any): Observable<MovimientoPersonal> {
    return this.http.put<MovimientoPersonal>(`${this.apiUrl}/movimientos/${id}`, data);
  }

  enviarMovimiento(id: number): Observable<MovimientoPersonal> {
    return this.http.post<MovimientoPersonal>(`${this.apiUrl}/movimientos/${id}/enviar`, null);
  }

  aprobarMovimiento(id: number): Observable<MovimientoPersonal> {
    return this.http.post<MovimientoPersonal>(`${this.apiUrl}/movimientos/${id}/aprobar`, null);
  }

  rechazarMovimiento(id: number): Observable<MovimientoPersonal> {
    return this.http.post<MovimientoPersonal>(`${this.apiUrl}/movimientos/${id}/rechazar`, null);
  }

  anularMovimiento(id: number): Observable<MovimientoPersonal> {
    return this.http.post<MovimientoPersonal>(`${this.apiUrl}/movimientos/${id}/anular`, null);
  }

  ejecutarMovimiento(id: number): Observable<MovimientoPersonal> {
    return this.http.post<MovimientoPersonal>(`${this.apiUrl}/movimientos/${id}/ejecutar`, null);
  }

  // ───── Acciones de personal ─────

  listarAcciones(idEmpleado?: number | null, estado?: string): Observable<AccionPersonal[]> {
    let params = new HttpParams();
    if (idEmpleado) params = params.set('idEmpleado', idEmpleado);
    if (estado) params = params.set('estado', estado);
    return this.http.get<AccionPersonal[]>(`${this.apiUrl}/acciones-personal`, { params });
  }

  crearAccion(data: any): Observable<AccionPersonal> {
    return this.http.post<AccionPersonal>(`${this.apiUrl}/acciones-personal`, data);
  }

  actualizarAccion(id: number, data: any): Observable<AccionPersonal> {
    return this.http.put<AccionPersonal>(`${this.apiUrl}/acciones-personal/${id}`, data);
  }

  enviarRevisionAccion(id: number): Observable<AccionPersonal> {
    return this.http.post<AccionPersonal>(`${this.apiUrl}/acciones-personal/${id}/enviar-revision`, null);
  }

  aprobarAccion(id: number): Observable<AccionPersonal> {
    return this.http.post<AccionPersonal>(`${this.apiUrl}/acciones-personal/${id}/aprobar`, null);
  }

  rechazarAccion(id: number): Observable<AccionPersonal> {
    return this.http.post<AccionPersonal>(`${this.apiUrl}/acciones-personal/${id}/rechazar`, null);
  }

  anularAccion(id: number): Observable<AccionPersonal> {
    return this.http.post<AccionPersonal>(`${this.apiUrl}/acciones-personal/${id}/anular`, null);
  }

  // ───── Vacaciones / permisos / licencias ─────

  listarAusencias(idEmpleado?: number | null, estado?: string): Observable<SolicitudAusencia[]> {
    let params = new HttpParams();
    if (idEmpleado) params = params.set('idEmpleado', idEmpleado);
    if (estado) params = params.set('estado', estado);
    return this.http.get<SolicitudAusencia[]>(`${this.apiUrl}/ausencias`, { params });
  }

  crearAusencia(data: any): Observable<SolicitudAusencia> {
    return this.http.post<SolicitudAusencia>(`${this.apiUrl}/ausencias`, data);
  }

  aprobarJefeAusencia(id: number): Observable<SolicitudAusencia> {
    return this.http.post<SolicitudAusencia>(`${this.apiUrl}/ausencias/${id}/aprobar-jefe`, null);
  }

  aprobarThAusencia(id: number): Observable<SolicitudAusencia> {
    return this.http.post<SolicitudAusencia>(`${this.apiUrl}/ausencias/${id}/aprobar-th`, null);
  }

  rechazarAusencia(id: number): Observable<SolicitudAusencia> {
    return this.http.post<SolicitudAusencia>(`${this.apiUrl}/ausencias/${id}/rechazar`, null);
  }

  anularAusencia(id: number): Observable<SolicitudAusencia> {
    return this.http.post<SolicitudAusencia>(`${this.apiUrl}/ausencias/${id}/anular`, null);
  }
}
