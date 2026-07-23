import { Component, OnInit, OnDestroy } from '@angular/core';
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
export class LoginComponent implements OnInit, OnDestroy {
  username = '';
  password = '';
  showPassword = false;
  error = '';
  cargando = false;

  slides = [
    { icon: 'pi pi-ticket', title: 'Gestión de Tickets', desc: 'Incidentes, requerimientos y cambios centralizados' },
    { icon: 'pi pi-file', title: 'Correspondencia', desc: 'Registro y control de documentos internos y externos' },
    { icon: 'pi pi-shield', title: 'Auditoría', desc: 'Trazabilidad completa de todas las operaciones del sistema' },
    { icon: 'pi pi-chart-bar', title: 'Reportes', desc: 'Estadísticas y dashboards en tiempo real' },
    { icon: 'pi pi-users', title: 'Usuarios y Roles', desc: 'Control de acceso basado en perfiles y permisos' },
  ];
  slideActual = 0;
  private timer: any;

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit() {
    this.autoRotar();
  }

  ngOnDestroy() {
    clearInterval(this.timer);
  }

  private autoRotar() {
    this.timer = setInterval(() => {
      this.slideActual = (this.slideActual + 1) % this.slides.length;
    }, 4000);
  }

  irASlide(i: number) {
    this.slideActual = i;
    clearInterval(this.timer);
    this.autoRotar();
  }

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
