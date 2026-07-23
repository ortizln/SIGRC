import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  @Input() collapsed = false;
  @Input() mobileOpen = false;
  @Output() closeMobile = new EventEmitter<void>();

  usuario: any;
  notifAbierto = false;
  notificaciones: any[] = [];
  noLeidas = 0;

  constructor(private auth: AuthService, private router: Router,
              private notifSvc: NotificationService) {
    this.usuario = this.auth.getUsuario();
    this.notifSvc.notificaciones$.subscribe(n => this.notificaciones = n);
    this.notifSvc.noLeidas$.subscribe(c => this.noLeidas = c);
  }

  getInitial(): string {
    return this.usuario?.nombres?.charAt(0)?.toUpperCase() || 'U';
  }

  isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }
  isAdminOrAuditor(): boolean { return this.auth.hasRole('ADMIN') || this.auth.hasRole('AUDITOR'); }
  canModulo(modulo: string, tipo: string): boolean { return this.auth.canModulo(modulo, tipo); }

  toggleNotif() { this.notifAbierto = !this.notifAbierto; }

  irANotificacion(n: any) {
    this.notifSvc.marcarLeida(n.id);
    this.notifAbierto = false;
    const ruta = n.tipo === 'CORRESPONDENCIA' ? '/correspondencia/' + n.idEntidad
                : n.tipo === 'TICKET' ? '/tickets/' + n.idEntidad
                : '/cambios/' + n.idEntidad;
    this.router.navigateByUrl(ruta);
  }

  marcarTodas() {
    this.notifSvc.marcarTodasLeidas();
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
