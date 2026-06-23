import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Ticket, TicketCrearRequest } from '@shared/models/ticket.model';
import { Paginacion } from '@shared/models/paginacion.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private apiUrl = `${environment.apiUrl}/tickets`;

  constructor(private http: HttpClient) {}

  listar(filtros: any): Observable<Paginacion<Ticket>> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') {
        params = params.set(k, v.toString());
      }
    });
    return this.http.get<Paginacion<Ticket>>(this.apiUrl, { params });
  }

  obtener(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.apiUrl}/${id}`);
  }

  crear(data: TicketCrearRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.apiUrl, data);
  }

  cambiarEstado(id: number, estado: string, idUsuario: number, observacion?: string): Observable<Ticket> {
    let params = new HttpParams().set('estado', estado).set('idUsuario', idUsuario);
    if (observacion) params = params.set('observacion', observacion);
    return this.http.patch<Ticket>(`${this.apiUrl}/${id}/estado`, null, { params });
  }

  asignar(id: number, idResponsable: number, idUsuario: number): Observable<Ticket> {
    const params = new HttpParams().set('idResponsable', idResponsable).set('idUsuario', idUsuario);
    return this.http.patch<Ticket>(`${this.apiUrl}/${id}/asignar`, null, { params });
  }

  getComentarios(id: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${id}/comentarios`);
  }

  addComentario(id: number, idUsuario: number, comentario: string, esInterno: boolean): Observable<any> {
    const params = new HttpParams().set('idUsuario', idUsuario).set('comentario', comentario).set('esInterno', esInterno);
    return this.http.post(`${this.apiUrl}/${id}/comentarios`, null, { params });
  }
}
