import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Adjunto } from '@shared/models/adjunto.model';

@Injectable({ providedIn: 'root' })
export class AdjuntoService {
  private baseUrl = `${environment.apiUrl}/tickets`;

  constructor(private http: HttpClient) {}

  listar(idTicket: number): Observable<Adjunto[]> {
    return this.http.get<Adjunto[]>(`${this.baseUrl}/${idTicket}/adjuntos`);
  }

  subir(idTicket: number, idUsuario: number, file: File): Observable<Adjunto> {
    const formData = new FormData();
    formData.append('archivo', file);
    formData.append('idUsuario', idUsuario.toString());
    return this.http.post<Adjunto>(`${this.baseUrl}/${idTicket}/adjuntos`, formData);
  }

  descargarUrl(idTicket: number, idAdjunto: number): string {
    return `${this.baseUrl}/${idTicket}/adjuntos/${idAdjunto}/descargar`;
  }

  eliminar(idTicket: number, idAdjunto: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${idTicket}/adjuntos/${idAdjunto}`);
  }
}
