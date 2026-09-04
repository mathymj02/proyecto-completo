// src/app/components/login/login.component.ts
//
// Version Angular de LoginButton.jsx. La diferencia mas importante: en vez
// de un hook (useMsal), Angular usa INYECCION DE DEPENDENCIAS via el
// constructor - MsalService y MsalBroadcastService llegan solos, ya
// configurados desde app.config.ts, sin que este componente sepa como se
// construyeron.
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { InteractionStatus, EventMessage, EventType, AuthenticationResult } from '@azure/msal-browser';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit, OnDestroy {
  private msalService = inject(MsalService);
  private msalBroadcastService = inject(MsalBroadcastService);
  private router = inject(Router);

  // Subject usado como "interruptor" para cancelar las suscripciones RxJS
  // cuando el componente se destruye (ngOnDestroy) - evita fugas de
  // memoria si el usuario navega afuera de esta pantalla antes de que un
  // evento de MSAL termine de llegar.
  private readonly destroying$ = new Subject<void>();

  nombreUsuario: string | null = null;

  ngOnInit(): void {
    // msalBroadcastService.inProgress$ emite el estado de interaccion
    // actual de MSAL (Login, AcquireToken, None, etc). Esperamos a que
    // llegue a "None" para asegurarnos de que MSAL ya termino de procesar
    // cualquier redireccion pendiente antes de leer las cuentas - si lo
    // hacemos antes, podriamos leer un estado "vacio" por una fraccion de
    // segundo justo despues de volver del login.
    this.msalBroadcastService.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None),
        takeUntil(this.destroying$)
      )
      .subscribe(() => {
        this.actualizarCuentaActiva();
      });

    // Ademas escuchamos el evento puntual de login exitoso, para fijar la
    // cuenta activa apenas MSAL confirma el resultado (setActiveAccount es
    // necesario porque MSAL puede manejar VARIAS cuentas en cache; sin
    // esto, algunas llamadas no sabrian para cual usuario pedir el token).
    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) => msg.eventType === EventType.LOGIN_SUCCESS),
        takeUntil(this.destroying$)
      )
      .subscribe((result: EventMessage) => {
        const payload = result.payload as AuthenticationResult;
        this.msalService.instance.setActiveAccount(payload.account);
        this.actualizarCuentaActiva();
      });
  }

  private actualizarCuentaActiva(): void {
    const cuentas = this.msalService.instance.getAllAccounts();
    if (cuentas.length > 0) {
      this.msalService.instance.setActiveAccount(cuentas[0]);
      this.nombreUsuario = cuentas[0].name ?? cuentas[0].username;
    } else {
      this.nombreUsuario = null;
    }
  }

  iniciarSesion(): void {
    // A diferencia de la version React, aca no hace falta pasarle
    // "scopes" a mano: MSALGuardConfigFactory (auth-config.ts) ya definio
    // que scopes pedir por defecto para cualquier interaccion de login.
    this.msalService.loginRedirect();
  }

  cerrarSesion(): void {
    this.msalService.logoutRedirect();
  }

  irAlDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  ngOnDestroy(): void {
    this.destroying$.next();
    this.destroying$.complete();
  }
}
