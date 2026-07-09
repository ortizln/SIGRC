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

  descargar(idTicket: number, idAdjunto: number, nombre: string) {
    this.http.get(`${this.baseUrl}/${idTicket}/adjuntos/${idAdjunto}/descargar`, { responseType: 'blob' })
      .subscribe(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombre;
        a.click();
        window.URL.revokeObjectURL(url);
      });
  }

  obtenerBlob(idTicket: number, idAdjunto: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${idTicket}/adjuntos/${idAdjunto}/descargar`, { responseType: 'blob' });
  }

  eliminar(idTicket: number, idAdjunto: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${idTicket}/adjuntos/${idAdjunto}`);
  }
}
