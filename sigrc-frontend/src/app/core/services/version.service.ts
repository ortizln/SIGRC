import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Version } from '@shared/models/version.model';

@Injectable({ providedIn: 'root' })
export class VersionService {
  private apiUrl = `${environment.apiUrl}/versiones`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Version[]> { return this.http.get<Version[]>(this.apiUrl); }
  obtener(id: number): Observable<Version> { return this.http.get<Version>(`${this.apiUrl}/${id}`); }
  listarPorSistema(idSistema: number): Observable<Version[]> { return this.http.get<Version[]>(`${this.apiUrl}/sistema/${idSistema}`); }
  crear(data: Partial<Version>): Observable<Version> { return this.http.post<Version>(this.apiUrl, data); }
  actualizar(id: number, data: Partial<Version>): Observable<Version> { return this.http.put<Version>(`${this.apiUrl}/${id}`, data); }
  eliminar(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/${id}`); }
}
