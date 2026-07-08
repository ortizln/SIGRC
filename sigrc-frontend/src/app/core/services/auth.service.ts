import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '@env/environment';

interface LoginRequest { username: string; password: string; }
interface LoginResponse {
  token: string; refreshToken: string; tipo: string;
  expiracion: string; usuario: any;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'sigrc_token';
  private readonly USER_KEY = 'sigrc_user';
  private apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data).pipe(
      tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.USER_KEY, JSON.stringify(res.usuario));
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  getToken(): string | null { return localStorage.getItem(this.TOKEN_KEY); }
  isLoggedIn(): boolean { return !!this.getToken(); }

  getUsuario(): any {
    const u = localStorage.getItem(this.USER_KEY);
    return u ? JSON.parse(u) : null;
  }

  hasRole(rol: string): boolean {
    const u = this.getUsuario();
    return u && u.rolCodigo === rol;
  }

  canModulo(modulo: string, tipoAcceso: string): boolean {
    const u = this.getUsuario();
    if (!u) return false;
    if (u.rolCodigo === 'ADMIN') return true;
    if (!u.permisos || u.permisos.length === 0) return false;
    const permiso = u.permisos.find((p: any) => p.modulo === modulo);
    if (!permiso) return false;
    if (tipoAcceso === 'LECTURA') return true;
    return permiso.tipoAcceso === 'ESCRITURA';
  }
}
