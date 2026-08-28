import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ApiEnvelope, AuthenticatedUser, LoginRequest, LoginResult, TokenPair, UserRole } from '../models/auth-model';
import { Observable, map, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly baseUrl = 'http://localhost:8080/api/v1';
  private readonly ACCESS_TOKEN_KEY = 'ssds_access_token';
  private readonly REFRESH_TOKEN_KEY = 'ssds_refresh_token';
  private readonly USER_KEY = 'ssds_user_info';

  readonly currentUser = signal<AuthenticatedUser | null>(this.getStoredUser());
  readonly isLoggedIn = computed(() => !!this.currentUser() && !!this.getAccessToken());

  login(credentials: LoginRequest): Observable<AuthenticatedUser> {
    return this.http.post<ApiEnvelope<LoginResult>>(`${this.baseUrl}/auth/login`, credentials).pipe(
      map((res) => res.data),
      tap((result) => this.saveAuthData(result)),
      map((result) => result.user),
    );
  }

  refresh(): Observable<TokenPair> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<ApiEnvelope<TokenPair>>(`${this.baseUrl}/auth/refresh`, { refreshToken }).pipe(
      map((res) => res.data),
      tap((tokens) => {
        localStorage.setItem(this.ACCESS_TOKEN_KEY, tokens.accessToken);
        localStorage.setItem(this.REFRESH_TOKEN_KEY, tokens.refreshToken);
      }),
    );
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    this.clearAuthData();
    this.router.navigate(['/login']);
    if (refreshToken) {
      this.http.post(`${this.baseUrl}/auth/logout`, { refreshToken }).subscribe({ error: () => {} });
    }
  }

  clearAuthData(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  hasRole(roles: UserRole | UserRole[]): boolean {
    const user = this.currentUser();
    if (!user) return false;
    const wanted = Array.isArray(roles) ? roles : [roles];
    return user.roles.some((r) => wanted.includes(r));
  }

  private saveAuthData(result: LoginResult): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, result.tokens.accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, result.tokens.refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(result.user));
    this.currentUser.set(result.user);
  }

  private getStoredUser(): AuthenticatedUser | null {
    const data = localStorage.getItem(this.USER_KEY);
    if (!data) return null;
    try {
      return JSON.parse(data) as AuthenticatedUser;
    } catch {
      return null;
    }
  }
}
