import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

@Injectable({ providedIn: 'root' })
export class AppMovilService {
  private apiUrl = `${environment.apiUrl}/app-movil`;

  constructor(private http: HttpClient) {}

  listar(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  obtenerPorId(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  subir(version: string, descripcion: string, archivo: File): Observable<any> {
    const formData = new FormData();
    formData.append('version', version);
    if (descripcion) formData.append('descripcion', descripcion);
    formData.append('archivo', archivo);
    return this.http.post(this.apiUrl, formData);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  descargar(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/descargar/${id}`, { responseType: 'blob' });
  }

  obtenerUltimo(): Observable<any> {
    return this.http.get(`${this.apiUrl}/ultimo`);
  }

  descargarUltimo(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/ultimo/descargar`, { responseType: 'blob' });
  }
}
