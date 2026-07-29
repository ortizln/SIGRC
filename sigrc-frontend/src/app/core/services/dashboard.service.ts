import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Dashboard, TicketItem, DocumentoItem } from '@shared/models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  obtener(): Observable<Dashboard> {
    return this.http.get<Dashboard>(this.apiUrl);
  }

  ticketsAbiertos(): Observable<TicketItem[]> {
    return this.http.get<TicketItem[]>(`${this.apiUrl}/tickets/abiertos`);
  }

  ticketsCerrados(): Observable<TicketItem[]> {
    return this.http.get<TicketItem[]>(`${this.apiUrl}/tickets/cerrados`);
  }

  ticketsVencidos(): Observable<TicketItem[]> {
    return this.http.get<TicketItem[]>(`${this.apiUrl}/tickets/vencidos`);
  }

  ticketsSinAsignar(): Observable<TicketItem[]> {
    return this.http.get<TicketItem[]>(`${this.apiUrl}/tickets/sin-asignar`);
  }

  documentosPendientes(): Observable<DocumentoItem[]> {
    return this.http.get<DocumentoItem[]>(`${this.apiUrl}/documentos/pendientes`);
  }

  documentosVencidos(): Observable<DocumentoItem[]> {
    return this.http.get<DocumentoItem[]>(`${this.apiUrl}/documentos/vencidos`);
  }

  documentosConTicket(): Observable<DocumentoItem[]> {
    return this.http.get<DocumentoItem[]>(`${this.apiUrl}/documentos/con-ticket`);
  }
}
