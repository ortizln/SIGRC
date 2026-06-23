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
}
