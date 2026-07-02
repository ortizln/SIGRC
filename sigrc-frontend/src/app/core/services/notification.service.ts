import { Injectable, NgZone } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { environment } from '@env/environment';
import { BehaviorSubject, Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface Notificacion {
  id: string;
  tipo: 'CORRESPONDENCIA' | 'TICKET' | 'CAMBIO';
  titulo: string;
  mensaje: string;
  idEntidad: number;
  fecha: string;
  leida: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private client: Client;
  private notificacionesSubject = new BehaviorSubject<Notificacion[]>([]);
  private noLeidasSubject = new BehaviorSubject<number>(0);
  private asignacionSubject = new BehaviorSubject<any>(null);

  notificaciones$: Observable<Notificacion[]> = this.notificacionesSubject.asObservable();
  noLeidas$: Observable<number> = this.noLeidasSubject.asObservable();
  asignacion$: Observable<any> = this.asignacionSubject.asObservable();

  constructor(private zone: NgZone, private auth: AuthService) {
    const wsUrl = environment.apiUrl.startsWith('http')
      ? environment.apiUrl.replace(/^http/, 'ws')
      : `${location.origin.replace(/^http/, 'ws')}${environment.apiUrl}`;
    this.client = new Client({
      brokerURL: `${wsUrl}/ws`,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.client.onConnect = () => {
      const user = this.auth.getUsuario();
      if (user?.idUsuario) {
        this.client.subscribe(`/topic/asignaciones/${user.idUsuario}`, (msg: IMessage) => {
          const data = JSON.parse(msg.body);
          this.zone.run(() => {
            this.agregar({ ...data, creadoPor: 'SISTEMA' });
            this.asignacionSubject.next(data);
          });
        });
      }
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error', frame.headers['message']);
    };

    this.client.activate();
  }

  private agregar(data: any) {
    const notif: Notificacion = {
      id: Date.now().toString(36) + Math.random().toString(36).slice(2, 9),
      tipo: data.tipo,
      titulo: data.titulo,
      mensaje: data.mensaje,
      idEntidad: data.idEntidad,
      fecha: data.fecha,
      leida: false,
    };
    const actuales = this.notificacionesSubject.getValue();
    actuales.unshift(notif);
    if (actuales.length > 50) actuales.pop();
    this.notificacionesSubject.next(actuales);
    this.actualizarNoLeidas();
  }

  marcarLeida(id: string) {
    const actuales = this.notificacionesSubject.getValue();
    const n = actuales.find(x => x.id === id);
    if (n) { n.leida = true; this.notificacionesSubject.next(actuales); }
    this.actualizarNoLeidas();
  }

  marcarTodasLeidas() {
    const actuales = this.notificacionesSubject.getValue();
    actuales.forEach(n => n.leida = true);
    this.notificacionesSubject.next(actuales);
    this.noLeidasSubject.next(0);
  }

  private actualizarNoLeidas() {
    const count = this.notificacionesSubject.getValue().filter(n => !n.leida).length;
    this.noLeidasSubject.next(count);
  }

  getNotificaciones(): Notificacion[] {
    return this.notificacionesSubject.getValue();
  }
}
