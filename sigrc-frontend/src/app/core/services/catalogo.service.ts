import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private apiUrl = `${environment.apiUrl}/catalogos`;

  constructor(private http: HttpClient) {}

  getAreas(): Observable<any[]> { return this.http.get<any[]>(`${this.apiUrl}/areas`); }
  getSistemas(): Observable<any[]> { return this.http.get<any[]>(`${this.apiUrl}/sistemas`); }
  getCategorias(): Observable<any[]> { return this.http.get<any[]>(`${this.apiUrl}/categorias`); }
  getSubcategorias(idCategoria: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/subcategorias/${idCategoria}`);
  }

  crearArea(data: any): Observable<any> { return this.http.post(`${this.apiUrl}/areas`, data); }
  actualizarArea(id: number, data: any): Observable<any> { return this.http.put(`${this.apiUrl}/areas/${id}`, data); }
  eliminarArea(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/areas/${id}`); }
  eliminarAreaHard(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/areas/${id}/hard`); }

  crearSistema(data: any): Observable<any> { return this.http.post(`${this.apiUrl}/sistemas`, data); }
  actualizarSistema(id: number, data: any): Observable<any> { return this.http.put(`${this.apiUrl}/sistemas/${id}`, data); }
  eliminarSistema(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/sistemas/${id}`); }
  eliminarSistemaHard(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/sistemas/${id}/hard`); }

  crearCategoria(data: any): Observable<any> { return this.http.post(`${this.apiUrl}/categorias`, data); }
  actualizarCategoria(id: number, data: any): Observable<any> { return this.http.put(`${this.apiUrl}/categorias/${id}`, data); }
  eliminarCategoria(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/categorias/${id}`); }
  eliminarCategoriaHard(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/categorias/${id}/hard`); }

  crearSubcategoria(data: any): Observable<any> { return this.http.post(`${this.apiUrl}/subcategorias`, data); }
  actualizarSubcategoria(id: number, data: any): Observable<any> { return this.http.put(`${this.apiUrl}/subcategorias/${id}`, data); }
  eliminarSubcategoria(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/subcategorias/${id}`); }
  eliminarSubcategoriaHard(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/subcategorias/${id}/hard`); }

  seed(): Observable<any> { return this.http.post(`${this.apiUrl}/seed`, {}); }
}
