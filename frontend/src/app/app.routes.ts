// src/app/app.routes.ts
//
// Definicion de rutas. Lo importante aca es "canActivate: [MsalGuard]" en
// la ruta /dashboard: eso le dice a Angular "antes de dejar entrar a esta
// ruta, pregunta a MsalGuard si hay sesion activa". Si NO la hay, el guard
// intercepta la navegacion y redirige a Microsoft para el login (gracias a
// MSALGuardConfigFactory con InteractionType.Redirect en auth-config.ts) -
// nunca se llega a mostrar el componente sin sesion.
import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { roleGuard } from './guards/role.guard';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';

export const routes: Routes = [
  { path: '', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    // Los guards corren EN ORDEN: primero MsalGuard confirma que haya
    // sesion (y si no, redirige a login); recien si eso pasa, roleGuard
    // chequea el permiso especifico. "data" es como le pasamos el permiso
    // requerido a roleGuard sin hardcodearlo dentro del guard mismo.
    canActivate: [MsalGuard, roleGuard],
    data: { permisoRequerido: 'write-read' },
  },
  { path: '**', redirectTo: '' },
];
