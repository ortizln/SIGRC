import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Permiso } from '@shared/models/permiso.model';

@Injectable({ providedIn: 'root' })
export class PermisoService {
  private apiUrl = `${environment.apiUrl}/permisos`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Permiso[]> { return this.http.get<Permiso[]>(this.apiUrl); }
  obtener(id: number): Observable<Permiso> { return this.http.get<Permiso>(`${this.apiUrl}/${id}`); }
  crear(data: Partial<Permiso>): Observable<Permiso> { return this.http.post<Permiso>(this.apiUrl, data); }
  actualizar(id: number, data: Partial<Permiso>): Observable<Permiso> { return this.http.put<Permiso>(`${this.apiUrl}/${id}`, data); }
  eliminar(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/${id}`); }
}
