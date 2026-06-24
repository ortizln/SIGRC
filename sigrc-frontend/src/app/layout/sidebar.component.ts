import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgIf],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  usuario: any;

  constructor(private auth: AuthService, private router: Router) {
    this.usuario = this.auth.getUsuario();
  }

  getInitial(): string {
    return this.usuario?.nombres?.charAt(0)?.toUpperCase() || 'U';
  }

  isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }
  isAdminOrAuditor(): boolean { return this.auth.hasRole('ADMIN') || this.auth.hasRole('AUDITOR'); }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
