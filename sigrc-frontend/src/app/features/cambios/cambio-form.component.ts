import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CambioService } from '@core/services/cambio.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-cambio-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cambio-form.component.html',
  styleUrl: './cambios.component.css'
})
export class CambioFormComponent implements OnInit {
  form: any = {
    titulo: '', descripcion: '', justificacion: '', tipo: 'NORMAL',
    impacto: 'MEDIO', riesgo: 'MEDIO', idSistema: null,
    idTicket: null, planImplementacion: '', planRetorno: ''
  };
  sistemas: any[] = [];
  cargando = false;

  constructor(
    private svc: CambioService,
    private catSvc: CatalogoService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.catSvc.getSistemas().subscribe(r => this.sistemas = r);
  }

  guardar() {
    if (this.cargando) return;
    this.cargando = true;
    const user = this.auth.getUsuario();
    this.form.idSolicitante = user.idUsuario;
    this.svc.crear(this.form).subscribe({
      next: r => this.router.navigate(['/cambios', r.idCambio]),
      error: () => { this.cargando = false; }
    });
  }
}
