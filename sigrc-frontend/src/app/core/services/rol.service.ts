import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Rol } from '@shared/models/rol.model';

@Injectable({ providedIn: 'root' })
export class RolService {
  private apiUrl = `${environment.apiUrl}/roles`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Rol[]> { return this.http.get<Rol[]>(this.apiUrl); }
  obtener(id: number): Observable<Rol> { return this.http.get<Rol>(`${this.apiUrl}/${id}`); }
  crear(data: Partial<Rol>): Observable<Rol> { return this.http.post<Rol>(this.apiUrl, data); }
  actualizar(id: number, data: Partial<Rol>): Observable<Rol> { return this.http.put<Rol>(`${this.apiUrl}/${id}`, data); }
  eliminar(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/${id}`); }
}
