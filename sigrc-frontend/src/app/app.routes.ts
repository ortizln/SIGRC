import { Routes } from '@angular/router';
import { authGuard, roleGuard } from '@core/guards/auth.guard';
import { MainLayoutComponent } from '@layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then(c => c.LoginComponent)
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(c => c.DashboardComponent) },
      { path: 'correspondencia', loadComponent: () => import('./features/correspondencia/correspondencia.component').then(c => c.CorrespondenciaListComponent) },
      { path: 'correspondencia/nuevo', loadComponent: () => import('./features/correspondencia/correspondencia-form.component').then(c => c.CorrespondenciaFormComponent) },
      { path: 'correspondencia/editar/:id', loadComponent: () => import('./features/correspondencia/correspondencia-form.component').then(c => c.CorrespondenciaFormComponent) },
      { path: 'correspondencia/:id', loadComponent: () => import('./features/correspondencia/correspondencia-detail.component').then(c => c.CorrespondenciaDetailComponent) },
      { path: 'tickets', loadComponent: () => import('./features/tickets/tickets.component').then(c => c.TicketsComponent) },
      { path: 'tickets/nuevo', loadComponent: () => import('./features/tickets/ticket-form.component').then(c => c.TicketFormComponent) },
      { path: 'tickets/:id', loadComponent: () => import('./features/tickets/ticket-detail.component').then(c => c.TicketDetailComponent) },
      { path: 'cambios', loadComponent: () => import('./features/cambios/cambios.component').then(c => c.CambiosComponent) },
      { path: 'cambios/nuevo', loadComponent: () => import('./features/cambios/cambio-form.component').then(c => c.CambioFormComponent) },
      { path: 'auditoria', loadComponent: () => import('./features/auditoria/auditoria.component').then(c => c.AuditoriaComponent), canActivate: [() => roleGuard(['ADMIN', 'AUDITOR'])] },
      { path: 'usuarios', loadComponent: () => import('./features/usuarios/usuarios.component').then(c => c.UsuariosComponent) },
      { path: 'conocimiento', loadComponent: () => import('./features/conocimiento/conocimiento.component').then(c => c.ConocimientoComponent) },
      { path: 'versiones', loadComponent: () => import('./features/versiones/versiones.component').then(c => c.VersionesComponent) },
      { path: 'roles', loadComponent: () => import('./features/roles/roles.component').then(c => c.RolesComponent), canActivate: [() => roleGuard(['ADMIN'])] },
      { path: 'catalogos', loadComponent: () => import('./features/catalogos/catalogos.component').then(c => c.CatalogosComponent) },
      { path: 'reportes', loadComponent: () => import('./features/reportes/reportes.component').then(c => c.ReportesComponent) },
      { path: 'app-movil', loadComponent: () => import('./features/app-movil/app-movil.component').then(c => c.AppMovilComponent) },
      { path: 'carrusel', loadComponent: () => import('./features/carrusel/carrusel.component').then(c => c.CarruselComponent) },
      { path: 'talento-humano/estructura', loadComponent: () => import('./features/estructura/estructura.component').then(c => c.EstructuraComponent), canActivate: [() => roleGuard(['ADMIN'])] },
      { path: 'talento-humano/puestos', loadComponent: () => import('./features/puestos/puestos.component').then(c => c.PuestosComponent), canActivate: [() => roleGuard(['ADMIN'])] },
      { path: 'talento-humano/empleados', loadComponent: () => import('./features/empleados/empleados.component').then(c => c.EmpleadosComponent), canActivate: [() => roleGuard(['ADMIN'])] },
      { path: 'talento-humano/asignaciones', loadComponent: () => import('./features/asignaciones/asignaciones.component').then(c => c.AsignacionesComponent), canActivate: [() => roleGuard(['ADMIN'])] },
      { path: 'talento-humano/movimientos', loadComponent: () => import('./features/movimientos/movimientos.component').then(c => c.MovimientosComponent), canActivate: [() => roleGuard(['ADMIN'])] },
      { path: 'talento-humano/acciones-personal', loadComponent: () => import('./features/acciones-personal/acciones-personal.component').then(c => c.AccionesPersonalComponent), canActivate: [() => roleGuard(['ADMIN'])] },
      { path: 'talento-humano/vacaciones-permisos', loadComponent: () => import('./features/vacaciones-permisos/vacaciones-permisos.component').then(c => c.VacacionesPermisosComponent) },
      { path: 'talento-humano/distributivo', loadComponent: () => import('./features/distributivo/distributivo.component').then(c => c.DistributivoComponent) },
      { path: 'talento-humano/dashboard', loadComponent: () => import('./features/dashboard-th/dashboard-th.component').then(c => c.DashboardThComponent) },
      { path: 'talento-humano/manual-funciones', loadComponent: () => import('./features/manual-funciones/manual-funciones.component').then(c => c.ManualFuncionesComponent) },
      { path: 'talento-humano/delegaciones', loadComponent: () => import('./features/delegaciones/delegaciones.component').then(c => c.DelegacionesComponent) },
      { path: 'talento-humano/mi-expediente', loadComponent: () => import('./features/mi-expediente/mi-expediente.component').then(c => c.MiExpedienteComponent) },
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
