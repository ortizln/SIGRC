import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CambioService } from '@core/services/cambio.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { TicketService } from '@core/services/ticket.service';
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
    idTicket: null, ticketNumero: '', planImplementacion: '', planRetorno: ''
  };
  sistemas: any[] = [];
  tickets: any[] = [];
  planArchivo: File | null = null;
  cargando = false;
  busquedaTicket = '';
  sugerenciasTicket: any[] = [];

  constructor(
    private svc: CambioService,
    private catSvc: CatalogoService,
    private ticketSvc: TicketService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.catSvc.getSistemas().subscribe(r => this.sistemas = r);
  }

  buscarTickets() {
    const texto = this.busquedaTicket?.trim();
    if (!texto || texto.length < 2) { this.sugerenciasTicket = []; return; }
    this.ticketSvc.listar({ texto, pagina: 0, tamanio: 15, sortBy: 'creado_en', sortDir: 'desc' })
      .subscribe(r => {
        this.sugerenciasTicket = r.contenido.filter((t: any) => t.idTicket !== this.form.idTicket);
      });
  }

  seleccionarTicket(t: any) {
    this.form.idTicket = t.idTicket;
    this.form.ticketNumero = t.numeroTicket;
    this.busquedaTicket = `${t.numeroTicket} — ${t.asunto}`;
    this.sugerenciasTicket = [];
  }

  quitarTicket() {
    this.form.idTicket = null;
    this.form.ticketNumero = '';
    this.busquedaTicket = '';
    this.sugerenciasTicket = [];
  }

  onPlanArchivo(event: any) {
    this.planArchivo = event.target.files?.[0] || null;
  }

  guardar() {
    if (this.cargando) return;
    this.cargando = true;
    const user = this.auth.getUsuario();
    this.form.idSolicitante = user.idUsuario;
    this.svc.crear(this.form).subscribe({
      next: async r => {
        if (this.planArchivo) {
          try { await this.svc.subirPlanArchivo(r.idCambio, this.planArchivo).toPromise(); } catch (_) {}
        }
        this.router.navigate(['/cambios', r.idCambio]);
      },
      error: () => { this.cargando = false; }
    });
  }
}
