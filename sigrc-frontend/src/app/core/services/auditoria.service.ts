import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Auditoria } from '@shared/models/auditoria.model';
import { Paginacion } from '@shared/models/paginacion.model';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private apiUrl = `${environment.apiUrl}/auditoria`;

  constructor(private http: HttpClient) {}

  listar(filtros: any): Observable<Paginacion<Auditoria>> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([k, v]) => {
      if (v) params = params.set(k, v.toString());
    });
    return this.http.get<Paginacion<Auditoria>>(this.apiUrl, { params });
  }
}
