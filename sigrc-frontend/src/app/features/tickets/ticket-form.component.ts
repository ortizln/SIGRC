import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TicketService } from '@core/services/ticket.service';
import { AdjuntoService } from '@core/services/adjunto.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-ticket-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './ticket-form.component.html',
  styleUrl: './ticket-form.component.css'
})
export class TicketFormComponent implements OnInit {
  form: any = { tipo: '', prioridad: '', idSolicitante: 0, idArea: 0, asunto: '', descripcion: '' };
  areas: any[] = [];
  sistemas: any[] = [];
  categorias: any[] = [];
  subcategorias: any[] = [];
  archivos: File[] = [];
  cargando = false;

  constructor(
    private ticketSvc: TicketService,
    private adjuntoSvc: AdjuntoService,
    private catSvc: CatalogoService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    const user = this.auth.getUsuario();
    if (user) {
      this.form.idSolicitante = user.idUsuario;
      this.form.idArea = user.idArea || 0;
    }
    this.catSvc.getAreas().subscribe(r => this.areas = r);
    this.catSvc.getSistemas().subscribe(r => this.sistemas = r);
    this.catSvc.getCategorias().subscribe(r => this.categorias = r);
  }

  cargarSubcategorias() {
    if (this.form.idCategoria) {
      this.catSvc.getSubcategorias(this.form.idCategoria).subscribe(r => this.subcategorias = r);
    } else {
      this.subcategorias = [];
    }
  }

  onArchivosSeleccionados(event: any) {
    this.archivos = Array.from(event.target.files || []);
  }

  quitarArchivo(i: number) {
    this.archivos.splice(i, 1);
  }

  guardar() {
    if (this.cargando) return;
    this.cargando = true;
    this.ticketSvc.crear(this.form).subscribe({
      next: async r => {
        const idTicket = r.idTicket;
        for (const f of this.archivos) {
          try { await this.adjuntoSvc.subir(idTicket, this.form.idSolicitante, f).toPromise(); } catch (_) {}
        }
        this.router.navigate(['/tickets', idTicket]);
      },
      error: () => { this.cargando = false; }
    });
  }
}
