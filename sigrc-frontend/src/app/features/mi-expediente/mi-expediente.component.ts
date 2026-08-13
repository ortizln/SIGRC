import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import Swal from 'sweetalert2';
import { TalentoHumanoService } from '@core/services/talento-humano.service';
import { Empleado } from '@shared/models/empleado.model';

@Component({
  selector: 'app-mi-expediente',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mi-expediente.component.html',
  styleUrl: './mi-expediente.component.css'
})
export class MiExpedienteComponent implements OnInit {
  cargando = true;
  error = '';
  empleado: Empleado | null = null;

  constructor(private svc: TalentoHumanoService) {}

  ngOnInit(): void {
    this.svc.miExpediente().subscribe({
      next: e => { this.empleado = e; this.cargando = false; },
      error: err => {
        this.cargando = false;
        this.error = err.error?.error || 'No se pudo cargar su expediente.';
      }
    });
  }

  tieneDatos(coleccion: any[] | undefined): boolean {
    return !!coleccion && coleccion.length > 0;
  }

  telefonos(): string {
    return [this.empleado?.telefono, this.empleado?.celular].filter(Boolean).join(' / ') || '—';
  }

  descargarDocumento(d: any) {
    if (!this.empleado || !d.idEmpleadoDocumento) return;
    this.svc.descargarArchivoExpediente(this.empleado.idEmpleado, d.idEmpleadoDocumento).subscribe({
      next: blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = d.nombreArchivo || `documento-${d.idEmpleadoDocumento}`;
        a.click();
        URL.revokeObjectURL(a.href);
      },
      error: (err) => {
        const msg = err.error?.error || 'No se pudo descargar el archivo.';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }
}