import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Usuario } from '@shared/models/usuario.model';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private apiUrl = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Usuario[]> { return this.http.get<Usuario[]>(this.apiUrl); }
  obtener(id: number): Observable<Usuario> { return this.http.get<Usuario>(`${this.apiUrl}/${id}`); }
  crear(data: any): Observable<Usuario> { return this.http.post<Usuario>(this.apiUrl, data); }
  actualizar(id: number, data: any): Observable<Usuario> { return this.http.put<Usuario>(`${this.apiUrl}/${id}`, data); }
  eliminar(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/${id}`); }
}
