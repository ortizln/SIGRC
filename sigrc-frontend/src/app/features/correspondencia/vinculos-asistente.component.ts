import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CorrespondenciaService } from '@core/services/correspondencia.service';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { AuthService } from '@core/services/auth.service';
import { VinculoAsistente } from '@shared/models/correspondencia.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-vinculos-asistente',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container-fluid px-0 animate-fade-in" style="max-width:1200px;">
      <div class="page-header d-flex justify-content-between align-items-start">
        <div>
          <h2>Vínculos Asistente → Titular</h2>
          <p>Configure qué secretarias/auxiliares pueden recibir documentos en la bandeja de su jefe inmediato</p>
        </div>
        <a routerLink="/correspondencia" class="btn btn-sm btn-outline-secondary">
          <i class="pi pi-arrow-left" style="font-size:0.75rem;"></i> Volver
        </a>
      </div>

      <!-- Lista de vínculos -->
      <div class="card-modern p-3 mb-3">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <h6 class="mb-0"><i class="pi pi-link me-1"></i> Vínculos activos</h6>
          <button class="btn btn-primary btn-sm" (click)="abrirForm()">
            <i class="pi pi-plus" style="font-size:0.75rem;"></i> Nuevo vínculo
          </button>
        </div>
        <div class="table-responsive">
          <table class="table table-sm table-hover align-middle" style="font-size:0.82rem;">
            <thead>
              <tr>
                <th>Asistente</th>
                <th>Puesto Asistente</th>
                <th>Titular</th>
                <th>Puesto Titular</th>
                <th>Permisos</th>
                <th>Desde</th>
                <th>Hasta</th>
                <th>Estado</th>
                <th class="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let v of vinculos">
                <td>{{ v.nombreAsistente }}</td>
                <td>{{ v.puestoAsistenteNombre }}</td>
                <td>{{ v.nombreTitular }}</td>
                <td>{{ v.puestoTitularNombre }}</td>
                <td>
                  <span class="badge me-1" [class.bg-success]="v.puedeRecepcionar" [class.bg-light]="!v.puedeRecepcionar">R</span>
                  <span class="badge me-1" [class.bg-info]="v.puedeOrganizar" [class.bg-light]="!v.puedeOrganizar">O</span>
                  <span class="badge me-1" [class.bg-secondary]="v.puedeSeguimiento" [class.bg-light]="!v.puedeSeguimiento">S</span>
                </td>
                <td>{{ v.vigenteDesde }}</td>
                <td>{{ v.vigenteHasta || '—' }}</td>
                <td>
                  <span class="badge" [class.bg-success]="v.vigente" [class.bg-danger]="!v.vigente">
                    {{ v.vigente ? 'Vigente' : 'Vencido' }}
                  </span>
                </td>
                <td class="text-end">
                  <button class="btn btn-sm btn-outline-primary me-1" (click)="editar(v)" title="Editar">
                    <i class="pi pi-pencil" style="font-size:0.7rem;"></i>
                  </button>
                  <button class="btn btn-sm btn-outline-danger" (click)="desactivar(v)" title="Desactivar">
                    <i class="pi pi-ban" style="font-size:0.7rem;"></i>
                  </button>
                </td>
              </tr>
              <tr *ngIf="vinculos.length === 0">
                <td colspan="9" class="text-center text-muted py-3">No hay vínculos configurados</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Formulario -->
      <div *ngIf="mostrarForm" class="card-modern p-3 mb-3 animate-fade-in">
        <h6 class="mb-3">{{ editando ? 'Editar' : 'Nuevo' }} vínculo</h6>
        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label small">Puesto Asistente <span class="text-danger">*</span></label>
            <select class="form-select form-select-sm" [(ngModel)]="form.puestoAsistenteId" [disabled]="editando">
              <option value="">— Seleccionar —</option>
              <option *ngFor="let p of puestos" [value]="p.idPuesto">{{ p.nombre }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label small">Puesto Titular <span class="text-danger">*</span></label>
            <select class="form-select form-select-sm" [(ngModel)]="form.puestoTitularId">
              <option value="">— Seleccionar —</option>
              <option *ngFor="let p of puestos" [value]="p.idPuesto">{{ p.nombre }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label small">Vigencia</label>
            <div class="d-flex gap-1">
              <input type="date" class="form-control form-control-sm" [(ngModel)]="form.vigenteDesde">
              <input type="date" class="form-control form-control-sm" [(ngModel)]="form.vigenteHasta" placeholder="Hasta (opcional)">
            </div>
          </div>
          <div class="col-12">
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" [(ngModel)]="form.puedeRecepcionar" id="permR">
              <label class="form-check-label small" for="permR">Recepcionar documentos</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" [(ngModel)]="form.puedeOrganizar" id="permO">
              <label class="form-check-label small" for="permO">Organizar / clasificar</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" [(ngModel)]="form.puedeSeguimiento" id="permS">
              <label class="form-check-label small" for="permS">Ver seguimiento</label>
            </div>
          </div>
        </div>
        <div class="d-flex gap-2 mt-3">
          <button class="btn btn-primary btn-sm" (click)="guardar()" [disabled]="guardando">
            <i class="pi pi-save" style="font-size:0.75rem;"></i> {{ guardando ? 'Guardando...' : 'Guardar' }}
          </button>
          <button class="btn btn-outline-secondary btn-sm" (click)="cancelar()">Cancelar</button>
        </div>
      </div>
    </div>
  `
})
export class VinculosAsistenteComponent implements OnInit {
  vinculos: VinculoAsistente[] = [];
  puestos: any[] = [];
  mostrarForm = false;
  editando = false;
  editandoId: number | null = null;
  guardando = false;
  form: Partial<VinculoAsistente> = this.formVacio();

  constructor(
    private svc: CorrespondenciaService,
    private thSvc: TalentoHumanoService,
    public auth: AuthService
  ) {}

  ngOnInit() {
    this.cargar();
    this.thSvc.getPuestos().subscribe((p: any[]) => this.puestos = p);
  }

  cargar() {
    this.svc.listarVinculosAsistente().subscribe(v => this.vinculos = v);
  }

  abrirForm() {
    this.editando = false;
    this.editandoId = null;
    this.form = this.formVacio();
    this.mostrarForm = true;
  }

  editar(v: VinculoAsistente) {
    this.editando = true;
    this.editandoId = v.idVinculo;
    this.form = { ...v };
    this.mostrarForm = true;
  }

  cancelar() {
    this.mostrarForm = false;
  }

  guardar() {
    if (!this.form.puestoAsistenteId || !this.form.puestoTitularId) {
      Swal.fire('Campos requeridos', 'Seleccione puesto asistente y titular', 'warning');
      return;
    }
    this.guardando = true;
    const obs = this.editando
      ? this.svc.actualizarVinculoAsistente(this.editandoId!, this.form)
      : this.svc.crearVinculoAsistente(this.form);
    obs.subscribe({
      next: () => {
        this.guardando = false;
        this.mostrarForm = false;
        this.cargar();
        Swal.fire('Éxito', 'Vinculo guardado correctamente', 'success');
      },
      error: (e) => {
        this.guardando = false;
        Swal.fire('Error', e.error?.message || 'No se pudo guardar', 'error');
      }
    });
  }

  desactivar(v: VinculoAsistente) {
    Swal.fire({
      title: '¿Desactivar vínculo?',
      text: `${v.nombreAsistente} dejará de ver la bandeja de ${v.nombreTitular}`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (r.isConfirmed) {
        this.svc.desactivarVinculoAsistente(v.idVinculo).subscribe(() => this.cargar());
      }
    });
  }

  private formVacio(): Partial<VinculoAsistente> {
    return {
      puestoAsistenteId: 0,
      puestoTitularId: 0,
      puedeRecepcionar: true,
      puedeOrganizar: false,
      puedeSeguimiento: true,
      vigenteDesde: new Date().toISOString().split('T')[0],
      vigenteHasta: null as any
    };
  }
}
