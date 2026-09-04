// src/app/guards/role.guard.ts
//
// MsalGuard (de la libreria) solo pregunta "¿hay sesion?". Este guard
// PROPIO va un paso mas alla: pregunta "¿el token de esta sesion trae el
// permiso 'write-read'?". Es la version Angular de lo que en el backend
// hacemos con @PreAuthorize o requestMatchers().hasAuthority(...) - aca lo
// hacemos ANTES de dejar entrar a la ruta, para no ni siquiera mostrar la
// pantalla si el usuario no deberia poder usarla.
//
// Desde Angular 15+, los guards pueden ser funciones simples (no clases)
// usando inject() - es el estilo moderno recomendado, mas corto que la
// forma antigua con "implements CanActivate".
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { decodeJwtPayload, extraerPermisos } from '../services/jwt.util';

export const roleGuard: CanActivateFn = (route) => {
  const msalService = inject(MsalService);
  const router = inject(Router);

  // El permiso requerido se define en la propia ruta (ver app.routes.ts,
  // "data: { permisoRequerido: 'write-read' }") - asi este mismo guard
  // sirve para cualquier ruta futura, sin repetir codigo por cada permiso.
  const permisoRequerido = route.data['permisoRequerido'] as string;

  const cuentaActiva = msalService.instance.getActiveAccount();
  if (!cuentaActiva) {
    // Sin cuenta activa, ni siquiera tiene sentido chequear permisos —
    // dejamos que MsalGuard (que corre junto a este en app.routes.ts) se
    // encargue de mandar al login.
    return true;
  }

  // idTokenClaims trae los claims del ID Token (identidad), no del Access
  // Token (permisos de API) - para leer "scp"/"roles" reales del backend
  // hace falta el Access Token, que MSAL guarda internamente. Para este
  // guard simplificamos leyendo los claims que MSAL ya cacheo del login.
  const permisos = extraerPermisos(cuentaActiva.idTokenClaims as Record<string, any>);

  if (!permisoRequerido || permisos.length === 0) {
    // Si no configuramos App Roles en Azure (caso mas comun en este
    // examen, donde usamos "scp" via consentimiento), dejamos pasar y
    // confiamos en que el BACKEND es quien de verdad hace cumplir el
    // permiso via @PreAuthorize - este guard es una mejora de UX
    // (evita mostrar una pantalla que de todas formas el backend
    // rechazaria), no la unica linea de defensa.
    return true;
  }

  if (permisos.includes(permisoRequerido)) {
    return true;
  }

  console.warn(`Acceso denegado: se requiere el permiso '${permisoRequerido}'`);
  router.navigate(['/']);
  return false;
};
