import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Cambio } from '@shared/models/cambio.model';

@Injectable({ providedIn: 'root' })
export class CambioService {
  private apiUrl = `${environment.apiUrl}/cambios`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Cambio[]> {
    return this.http.get<Cambio[]>(this.apiUrl);
  }

  obtener(id: number): Observable<Cambio> {
    return this.http.get<Cambio>(`${this.apiUrl}/${id}`);
  }

  crear(data: any): Observable<Cambio> {
    return this.http.post<Cambio>(this.apiUrl, data);
  }

  aprobar(id: number, idAprobador: number): Observable<Cambio> {
    return this.http.patch<Cambio>(`${this.apiUrl}/${id}/aprobar`, null, {
      params: { idAprobador }
    });
  }
}
