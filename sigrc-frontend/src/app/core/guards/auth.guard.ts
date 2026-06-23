import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

export const authGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  router.navigate(['/login']);
  return false;
};

export const roleGuard = (roles: string[]) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.getUsuario();
  if (user && roles.includes(user.rolCodigo)) return true;
  router.navigate(['/']);
  return false;
};
