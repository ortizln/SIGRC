import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { DireccionManual, UnidadManual, PuestoManual, VersionManual } from '@shared/models/manual-funciones.model';
import { PuestoFuncion } from '@shared/models/puesto.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-manual-funciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './manual-funciones.component.html',
  styleUrl: './manual-funciones.component.css'
})
export class ManualFuncionesComponent implements OnInit {
  manual: any = null;
  direccionSel: DireccionManual | null = null;
  unidadSel: UnidadManual | null = null;
  puestoSel: PuestoManual | null = null;

  versiones: VersionManual[] = [];
  cargando = false;

  constructor(private svc: TalentoHumanoService, private auth: AuthService) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  get versionVigente(): VersionManual | null { return this.manual?.version || null; }

  ngOnInit() {
    this.cargar();
    if (this.isAdmin) this.cargarVersiones();
  }

  cargar() {
    this.cargando = true;
    this.svc.getManualFunciones().subscribe({
      next: r => {
        this.manual = r;
        this.cargando = false;
        if (r.direcciones?.length) {
          this.direccionSel = r.direcciones[0];
          if (this.direccionSel.unidades?.length) this.unidadSel = this.direccionSel.unidades[0];
          this.primeraFicha();
        }
      },
      error: () => this.cargando = false
    });
  }

  cargarVersiones() {
    this.svc.getVersionesManual().subscribe(r => this.versiones = r);
  }

  puestosActuales(): PuestoManual[] {
    if (this.unidadSel) return this.unidadSel.puestos || [];
    if (this.direccionSel) return this.direccionSel.puestos || [];
    return [];
  }

  primeraFicha() {
    const lista = this.puestosActuales();
    if (lista.length) this.puestoSel = lista[0];
  }

  seleccionarDireccion(d: DireccionManual) {
    this.direccionSel = d;
    this.unidadSel = d.unidades?.length ? d.unidades[0] : null;
    const lista = this.puestosActuales();
    this.puestoSel = lista.length ? lista[0] : null;
  }

  seleccionarUnidad(u: UnidadManual) {
    this.unidadSel = u;
    this.puestoSel = u.puestos?.length ? u.puestos[0] : null;
  }

  seleccionarPuesto(p: PuestoManual) {
    this.puestoSel = p;
  }

  funcionesOrdenadas(p: PuestoManual | null): PuestoFuncion[] {
    const orden = { ESENCIAL: 0, COMPLEMENTARIA: 1, CONTROL: 2 };
    return (p?.funciones || []).slice().sort((a, b) =>
      ((orden as any)[a.tipo ?? ''] ?? 9) - ((orden as any)[b.tipo ?? ''] ?? 9));
  }

  funcionesPorTipo(p: PuestoManual | null, tipo: string): PuestoFuncion[] {
    return this.funcionesOrdenadas(p).filter(f => (f.tipo || 'ESENCIAL') === tipo);
  }

  etiquetaTipo(tipo: string): string {
    if (tipo === 'ESENCIAL') return 'Esenciales';
    if (tipo === 'COMPLEMENTARIA') return 'Complementarias';
    if (tipo === 'CONTROL') return 'Control';
    return 'Funciones';
  }

  estadoBadge(estado: string): string {
    return estado === 'VIGENTE' ? 'badge-ok' : estado === 'DEROGADO' ? 'badge-no' : 'badge-otro';
  }

  // ─────────── Gestión de versiones ───────────

  aprobar(v: VersionManual) {
    Swal.fire({
      title: '¿Aprobar esta versión?',
      text: `Quedará VIGENTE y la versión vigente actual pasará a DEROGADO.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, aprobar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.aprobarVersionManual(v.idVersionManual).subscribe({
        next: () => { this.cargarVersiones(); this.cargar(); },
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo aprobar.', 'error')
      });
    });
  }

  derogar(v: VersionManual) {
    Swal.fire({
      title: '¿Derogar esta versión?',
      text: 'Dejará de estar vigente pero su información se conserva.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, derogar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.svc.derogarVersionManual(v.idVersionManual).subscribe({
        next: () => { this.cargarVersiones(); this.cargar(); },
        error: (e) => Swal.fire('Error', e.error?.error || 'No se pudo derogar.', 'error')
      });
    });
  }
}