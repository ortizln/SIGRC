import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-distributivo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './distributivo.component.html',
  styleUrl: './distributivo.component.css'
})
export class DistributivoComponent implements OnInit {
  unidades: any[] = [];
  puestos: any[] = [];
  filas: any[] = [];
  filtros: any = { idUnidad: '', idPuesto: '', estado: '', tipoPersonal: '' };
  cargando = false;

  constructor(
    private svc: TalentoHumanoService,
    public auth: AuthService
  ) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  ngOnInit() {
    this.svc.getUnidades().subscribe(r => this.unidades = r);
    this.svc.getPuestos().subscribe(r => this.puestos = r);
    this.cargar();
  }

  cargar() {
    this.cargando = true;
    this.svc.distributivo({
      idUnidad: this.filtros.idUnidad || null,
      idPuesto: this.filtros.idPuesto || null,
      estado: this.filtros.estado || null,
      tipoPersonal: this.filtros.tipoPersonal || null
    }).subscribe({
      next: r => {
        this.filas = r;
        this.cargando = false;
      },
      error: () => this.cargando = false
    });
  }

  limpiar() {
    this.filtros = { idUnidad: '', idPuesto: '', estado: '', tipoPersonal: '' };
    this.cargar();
  }

  estadoBadge(estado: string): string {
    return estado === 'ACTIVO' ? 'badge-ok' : 'badge-no';
  }

  exportarCSV() {
    if (this.filas.length === 0) {
      Swal.fire('Sin datos', 'No hay filas para exportar.', 'info');
      return;
    }
    const cab = ['Identificación', 'Funcionario', 'Unidad', 'Puesto', 'Grupo ocupacional', 'Relación', 'Ingreso', 'Estado', 'Tipo personal'];
    const filas = this.filas.map(f => [
      f.identificacion, f.funcionario, f.unidad, f.puesto,
      f.grupoOcupacional || '', f.tipoRelacion || '', f.fechaIngreso || '',
      f.estadoLaboral || '', f.tipoPersonal || ''
    ]);
    const csv = [cab, ...filas].map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(';')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'distributivo-personal.csv';
    a.click();
    URL.revokeObjectURL(a.href);
  }

  exportar(formato: 'excel' | 'pdf') {
    if (this.filas.length === 0) {
      Swal.fire('Sin datos', 'No hay filas para exportar.', 'info');
      return;
    }
    this.svc.exportarDistributivo({
      idUnidad: this.filtros.idUnidad || null,
      idPuesto: this.filtros.idPuesto || null,
      estado: this.filtros.estado || null,
      tipoPersonal: this.filtros.tipoPersonal || null
    }, formato).subscribe({
      next: blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `distributivo_personal.${formato === 'pdf' ? 'pdf' : 'xlsx'}`;
        a.click();
        URL.revokeObjectURL(a.href);
      },
      error: () => Swal.fire('Error', 'No se pudo generar el archivo.', 'error')
    });
  }
}
