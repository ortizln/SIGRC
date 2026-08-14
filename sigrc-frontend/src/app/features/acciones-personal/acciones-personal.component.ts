import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-acciones-personal',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './acciones-personal.component.html',
  styleUrl: './acciones-personal.component.css'
})
export class AccionesPersonalComponent implements OnInit {
  empleados: any[] = [];
  idEmpleado: number | null = null;
  estadoFiltro = '';
  acciones: any[] = [];

  constructor(
    private svc: GestionPersonalService,
    private thSvc: TalentoHumanoService,
    public auth: AuthService
  ) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.thSvc.getEmpleados().subscribe(r => this.empleados = r);
    this.cargar();
  }

  cargar() {
    this.svc.listarAcciones(this.idEmpleado, this.estadoFiltro || undefined).subscribe(r => this.acciones = r);
  }

  estadoBadge(estado: string): string {
    const map: Record<string, string> = {
      'BORRADOR': 'borde', 'EN_REVISION': 'info', 'APROBADA': 'ok', 'RECHAZADA': 'no', 'ANULADA': 'no'
    };
    return map[estado] || 'info';
  }

  enviarRevision(a: any) {
    this.svc.enviarRevisionAccion(a.idAccion).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Enviada', 'Acción enviada a revisión.', 'success');
    });
  }

  aprobar(a: any) {
    this.svc.aprobarAccion(a.idAccion).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Aprobada', 'Acción de personal aprobada.', 'success');
    });
  }

  rechazar(a: any) {
    this.svc.rechazarAccion(a.idAccion).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Rechazada', 'Acción rechazada.', 'info');
    });
  }

  anular(a: any) {
    this.svc.anularAccion(a.idAccion).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Anulada', 'Acción anulada.', 'info');
    });
  }

  private remplazar(r: any) {
    const i = this.acciones.findIndex(x => x.idAccion === r.idAccion);
    if (i >= 0) this.acciones[i] = r;
  }
}