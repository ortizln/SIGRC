import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { CatalogoService } from '@core/services/catalogo.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-catalogos',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './catalogos.component.html',
  styleUrl: './catalogos.component.css'
})
export class CatalogosComponent implements OnInit {
  tabActivo = 'areas';
  areas: any[] = [];
  sistemas: any[] = [];
  categorias: any[] = [];
  subcategorias: any[] = [];
  menuAbierto: number | null = null;

  constructor(private svc: CatalogoService, private auth: AuthService) {}

  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }

  get tipoRuta(): string {
    switch (this.tabActivo) {
      case 'areas': return 'area';
      case 'sistemas': return 'sistema';
      case 'categorias': return 'categoria';
      default: return 'subcategoria';
    }
  }

  ngOnInit() {
    this.cargarAreas();
    this.cargarSistemas();
    this.cargarCategorias();
  }

  cargarAreas() { this.svc.getAreas().subscribe(r => this.areas = r); }
  cargarSistemas() { this.svc.getSistemas().subscribe(r => this.sistemas = r); }
  cargarCategorias() { this.svc.getCategorias().subscribe(r => this.categorias = r); }

  onTab(tab: string) {
    this.tabActivo = tab;
    this.menuAbierto = null;
  }

  toggleMenu(id: number) {
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  desactivar(item: any, tipo: string) {
    Swal.fire({
      title: '¿Desactivar?',
      text: `${item.nombre} quedará inactivo pero los datos se conservan.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      let obs;
      if (tipo === 'area') obs = this.svc.eliminarArea(item.idArea);
      else if (tipo === 'sistema') obs = this.svc.eliminarSistema(item.idSistema);
      else if (tipo === 'categoria') obs = this.svc.eliminarCategoria(item.idCategoria);
      else obs = this.svc.eliminarSubcategoria(item.idSubcategoria);
      obs.subscribe(() => {
        if (tipo === 'area') this.cargarAreas();
        else if (tipo === 'sistema') this.cargarSistemas();
        else if (tipo === 'categoria') this.cargarCategorias();
      });
    });
  }

  eliminarFisico(item: any, tipo: string) {
    Swal.fire({
      title: '¿Eliminar permanentemente?',
      text: `${item.nombre} se eliminará de la base de datos. Esta acción no se puede deshacer.`,
      icon: 'error',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#dc3545'
    }).then(r => {
      if (!r.isConfirmed) return;
      this.menuAbierto = null;
      let obs;
      if (tipo === 'area') obs = this.svc.eliminarAreaHard(item.idArea);
      else if (tipo === 'sistema') obs = this.svc.eliminarSistemaHard(item.idSistema);
      else if (tipo === 'categoria') obs = this.svc.eliminarCategoriaHard(item.idCategoria);
      else obs = this.svc.eliminarSubcategoriaHard(item.idSubcategoria);
      obs.subscribe((res: any) => {
        if (res.success) {
          Swal.fire('Eliminado', 'Registro eliminado permanentemente.', 'success');
          if (tipo === 'area') this.cargarAreas();
          else if (tipo === 'sistema') this.cargarSistemas();
          else if (tipo === 'categoria') this.cargarCategorias();
        } else {
          Swal.fire('No se puede eliminar', res.message || 'Este registro está siendo usado por otros datos.', 'warning');
        }
      });
    });
  }
}