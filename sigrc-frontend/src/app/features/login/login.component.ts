import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  username = '';
  password = '';
  showPassword = false;
  error = '';
  cargando = false;

  constructor(private auth: AuthService, private router: Router) {}

  login() {
    if (!this.username || !this.password) { this.error = 'Ingrese usuario y contraseña'; return; }
    this.cargando = true;
    this.error = '';
    this.auth.login({ username: this.username, password: this.password }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (e) => {
        if (e.status === 401) {
          this.error = 'Usuario o contraseña incorrectos';
        } else if (e.status === 0) {
          this.error = 'No se pudo conectar con el servidor';
        } else {
          this.error = e.error?.mensaje || 'Error al iniciar sesión';
        }
        this.cargando = false;
      }
    });
  }
}
