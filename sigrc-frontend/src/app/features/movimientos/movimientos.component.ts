import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { GestionPersonalService } from '@core/services/gestion-personal.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-movimientos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './movimientos.component.html',
  styleUrl: './movimientos.component.css'
})
export class MovimientosComponent implements OnInit {
  empleados: any[] = [];
  asignaciones: any[] = [];

  idEmpleado: number | null = null;
  estadoFiltro = '';
  movimientos: any[] = [];

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
    this.svc.listarMovimientos(this.idEmpleado, this.estadoFiltro || undefined).subscribe(r => this.movimientos = r);
  }

  onEmpleadoChange() {
    if (this.idEmpleado) {
      this.thSvc.getAsignaciones(this.idEmpleado).subscribe(r => this.asignaciones = r);
    } else {
      this.asignaciones = [];
    }
    this.cargar();
  }

  estadoBadge(estado: string): string {
    const map: Record<string, string> = {
      'BORRADOR': 'borde', 'PENDIENTE': 'info', 'APROBADA': 'ok', 'RECHAZADA': 'no', 'ANULADA': 'no'
    };
    return map[estado] || 'info';
  }

  enviar(m: any) {
    this.svc.enviarMovimiento(m.idMovimiento).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Enviado', 'Movimiento enviado a aprobación.', 'success');
    });
  }

  aprobar(m: any) {
    this.svc.aprobarMovimiento(m.idMovimiento).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Aprobado', 'Movimiento aprobado. Ahora puede ejecutarse.', 'success');
    });
  }

  ejecutar(m: any) {
    Swal.fire({
      title: '¿Ejecutar movimiento?',
      text: 'Se registrará la nueva asignación de puesto y se cerrará la anterior (se conserva historial).',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, ejecutar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.svc.ejecutarMovimiento(m.idMovimiento).subscribe(r => {
          this.remplazar(r);
          Swal.fire('Ejecutado', 'Asignación de puesto registrada.', 'success');
        });
      }
    });
  }

  rechazar(m: any) {
    this.svc.rechazarMovimiento(m.idMovimiento).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Rechazado', 'Movimiento rechazado.', 'info');
    });
  }

  anular(m: any) {
    this.svc.anularMovimiento(m.idMovimiento).subscribe(r => {
      this.remplazar(r);
      Swal.fire('Anulado', 'Movimiento anulado.', 'info');
    });
  }

  private remplazar(r: any) {
    const i = this.movimientos.findIndex(x => x.idMovimiento === r.idMovimiento);
    if (i >= 0) this.movimientos[i] = r;
  }
}
