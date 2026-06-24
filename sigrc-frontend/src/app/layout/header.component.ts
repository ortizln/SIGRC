import { Component, EventEmitter, Output } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  @Output() toggleSidebar = new EventEmitter<void>();
  usuario: any;

  constructor(private auth: AuthService, private router: Router) {
    this.usuario = this.auth.getUsuario();
  }

  getInitial(): string {
    return this.usuario?.nombres?.charAt(0)?.toUpperCase() || 'U';
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
