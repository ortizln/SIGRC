import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
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
      { path: 'correspondencia/:id', loadComponent: () => import('./features/correspondencia/correspondencia-detail.component').then(c => c.CorrespondenciaDetailComponent) },
      { path: 'tickets', loadComponent: () => import('./features/tickets/tickets.component').then(c => c.TicketsComponent) },
      { path: 'tickets/nuevo', loadComponent: () => import('./features/tickets/ticket-form.component').then(c => c.TicketFormComponent) },
      { path: 'tickets/:id', loadComponent: () => import('./features/tickets/ticket-detail.component').then(c => c.TicketDetailComponent) },
      { path: 'cambios', loadComponent: () => import('./features/cambios/cambios.component').then(c => c.CambiosComponent) },
      { path: 'cambios/nuevo', loadComponent: () => import('./features/cambios/cambio-form.component').then(c => c.CambioFormComponent) },
      { path: 'auditoria', loadComponent: () => import('./features/auditoria/auditoria.component').then(c => c.AuditoriaComponent) },
      { path: 'usuarios', loadComponent: () => import('./features/usuarios/usuarios.component').then(c => c.UsuariosComponent) },
      { path: 'conocimiento', loadComponent: () => import('./features/conocimiento/conocimiento.component').then(c => c.ConocimientoComponent) },
      { path: 'versiones', loadComponent: () => import('./features/versiones/versiones.component').then(c => c.VersionesComponent) },
      { path: 'roles', loadComponent: () => import('./features/roles/roles.component').then(c => c.RolesComponent) },
      { path: 'catalogos', loadComponent: () => import('./features/catalogos/catalogos.component').then(c => c.CatalogosComponent) },
      { path: 'reportes', loadComponent: () => import('./features/reportes/reportes.component').then(c => c.ReportesComponent) },
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
