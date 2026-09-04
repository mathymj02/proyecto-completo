// src/app/app.config.ts
//
// Aca se "cablean" todas las piezas de MSAL dentro del sistema de
// Dependency Injection de Angular. Es el equivalente Angular a lo que en
// React hacia <MsalProvider instance={msalInstance}> envolviendo <App/> —
// pero en vez de un Context de React, Angular usa un array de "providers"
// que cualquier componente/servicio puede pedir via injection.
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import {
  MsalGuard,
  MsalBroadcastService,
  MsalService,
  MsalInterceptor,
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
} from '@azure/msal-angular';

import { routes } from './app.routes';
import {
  MSALInstanceFactory,
  MSALGuardConfigFactory,
  MSALInterceptorConfigFactory,
} from './config/auth-config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),

    // withInterceptorsFromDi(): habilita que HttpClient use interceptores
    // "clasicos" registrados via HTTP_INTERCEPTORS (como MsalInterceptor
    // de abajo), que es como esta hecha la libreria de MSAL. Sin esto, el
    // interceptor de MSAL nunca se ejecutaria con la API moderna de
    // provideHttpClient().
    provideHttpClient(withInterceptorsFromDi()),

    // Registra MsalInterceptor como interceptor HTTP global: a partir de
    // aca, CUALQUIER peticion HttpClient que coincida con una URL del
    // protectedResourceMap (ver auth-config.ts) recibe automaticamente el
    // header Authorization con el token correcto.
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true,
    },

    // Las 3 factory functions conectadas a sus respectivos "tokens" de
    // inyeccion que exige la libreria @azure/msal-angular.
    {
      provide: MSAL_INSTANCE,
      useFactory: MSALInstanceFactory,
    },
    {
      provide: MSAL_GUARD_CONFIG,
      useFactory: MSALGuardConfigFactory,
    },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: MSALInterceptorConfigFactory,
    },

    // Servicios de la libreria que vamos a inyectar en componentes:
    //   - MsalService: metodos como loginRedirect(), logout(), y acceso a
    //     la instancia/cuentas activas.
    //   - MsalGuard: protege rutas en app.routes.ts (canActivate).
    //   - MsalBroadcastService: emite eventos (login exitoso, token
    //     adquirido, etc.) que los componentes pueden escuchar.
    MsalService,
    MsalGuard,
    MsalBroadcastService,
  ],
};
