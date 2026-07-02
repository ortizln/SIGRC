import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Correspondencia, CorrespondenciaCrearRequest, CorrespondenciaAdjunto, CorrespondenciaHistorial, CorrespondenciaRespuesta, TicketVinculado, Paginacion, CorrespondenciaDashboard } from '@shared/models/correspondencia.model';

@Injectable({ providedIn: 'root' })
export class CorrespondenciaService {
  private apiUrl = `${environment.apiUrl}/correspondencia`;

  constructor(private http: HttpClient) {}

  listar(filtros: any): Observable<Paginacion<Correspondencia>> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') params = params.set(k, String(v));
    });
    return this.http.get<Paginacion<Correspondencia>>(this.apiUrl, { params });
  }

  obtener(id: number): Observable<Correspondencia> {
    return this.http.get<Correspondencia>(`${this.apiUrl}/${id}`);
  }

  crear(data: CorrespondenciaCrearRequest): Observable<Correspondencia> {
    return this.http.post<Correspondencia>(this.apiUrl, data);
  }

  actualizar(id: number, data: any): Observable<Correspondencia> {
    return this.http.put<Correspondencia>(`${this.apiUrl}/${id}`, data);
  }

  cambiarEstado(id: number, estado: string, detalle?: string): Observable<Correspondencia> {
    let params = new HttpParams().set('estado', estado);
    if (detalle) params = params.set('detalle', detalle);
    return this.http.patch<Correspondencia>(`${this.apiUrl}/${id}/estado`, null, { params });
  }

  asignarResponsable(id: number, idResponsable: number): Observable<Correspondencia> {
    const params = new HttpParams().set('idResponsable', idResponsable);
    return this.http.patch<Correspondencia>(`${this.apiUrl}/${id}/asignar`, null, { params });
  }

  registrarRespuesta(id: number, data: any): Observable<CorrespondenciaRespuesta> {
    return this.http.post<CorrespondenciaRespuesta>(`${this.apiUrl}/${id}/respuesta`, data);
  }

  listarAdjuntos(id: number): Observable<CorrespondenciaAdjunto[]> {
    return this.http.get<CorrespondenciaAdjunto[]>(`${this.apiUrl}/${id}/adjuntos`);
  }

  subirAdjunto(id: number, file: File, tipo: string): Observable<CorrespondenciaAdjunto> {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('tipo', tipo);
    return this.http.post<CorrespondenciaAdjunto>(`${this.apiUrl}/${id}/adjuntos`, fd);
  }

  descargar(id: number, idAdjunto: number, nombre: string) {
    this.http.get(`${this.apiUrl}/${id}/adjuntos/${idAdjunto}/descargar`, { responseType: 'blob' })
      .subscribe(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombre;
        a.click();
        window.URL.revokeObjectURL(url);
      });
  }

  eliminarAdjunto(id: number, idAdjunto: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/adjuntos/${idAdjunto}`);
  }

  obtenerHistorial(id: number): Observable<CorrespondenciaHistorial[]> {
    return this.http.get<CorrespondenciaHistorial[]>(`${this.apiUrl}/${id}/historial`);
  }

  obtenerTickets(id: number): Observable<TicketVinculado[]> {
    return this.http.get<TicketVinculado[]>(`${this.apiUrl}/${id}/tickets`);
  }

  generarTicket(id: number): Observable<TicketVinculado> {
    return this.http.post<TicketVinculado>(`${this.apiUrl}/${id}/generar-ticket`, null);
  }

  vincularTicket(id: number, idTicket: number): Observable<TicketVinculado> {
    const params = new HttpParams().set('idTicket', idTicket);
    return this.http.post<TicketVinculado>(`${this.apiUrl}/${id}/vincular-ticket`, null, { params });
  }

  getTiposDocumento(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tipos-documento`);
  }

  dashboard(): Observable<CorrespondenciaDashboard> {
    return this.http.get<CorrespondenciaDashboard>(`${this.apiUrl}/dashboard`);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
