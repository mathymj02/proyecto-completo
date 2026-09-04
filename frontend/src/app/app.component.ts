// src/app/app.component.ts
//
// Aca vive la inicializacion de MSAL que en la version React estaba en
// main.jsx (msalInstance.initialize() + handleRedirectPromise()). En
// Angular esto se hace en ngOnInit() del componente raiz, siguiendo el
// patron oficial de Microsoft para @azure/msal-angular v3.
import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private msalService = inject(MsalService);
  title = 'pedidos360-frontend';

  ngOnInit(): void {
    // Igual que en la version React: MSAL exige inicializar la instancia
    // ANTES de cualquier otra operacion.
    this.msalService.instance.initialize().then(() => {
      // handleRedirectObservable() es el equivalente Angular (basado en
      // RxJS) de handleRedirectPromise() en React: procesa el resultado
      // si esta carga de pagina es un "regreso" desde el login de Azure.
      this.msalService.handleRedirectObservable().subscribe({
        next: () => this.fijarCuentaActivaSiHaceFalta(),
        error: (error) => console.error('Error procesando el redirect de Azure:', error),
      });

      // CLAVE: esto corre en TODAS las rutas, no solo en LoginComponent.
      // Si el usuario recarga la pagina estando en /dashboard (o entra
      // directo a esa URL), este componente raiz SIEMPRE se ejecuta
      // primero — a diferencia de LoginComponent, que solo corre si el
      // usuario pasa por la ruta "". Sin esta linea, MSAL "olvida" cual
      // era la cuenta activa en cada recarga fuera del login, y entonces
      // ni acquireTokenSilent() ni MsalInterceptor saben para qué cuenta
      // pedir el token — el resultado es que las peticiones salen SIN
      // Authorization en absoluto (el backend las ve como anonimas).
      this.fijarCuentaActivaSiHaceFalta();
    });
  }

  /**
   * Si MSAL ya tiene una cuenta guardada en cache (sessionStorage) pero
   * ninguna esta marcada como "activa" en este momento del ciclo de vida
   * de la app, fija la primera como activa. Es idempotente: si ya hay una
   * cuenta activa, no hace nada.
   */
  private fijarCuentaActivaSiHaceFalta(): void {
    const cuentaActiva = this.msalService.instance.getActiveAccount();
    if (!cuentaActiva) {
      const cuentas = this.msalService.instance.getAllAccounts();
      if (cuentas.length > 0) {
        this.msalService.instance.setActiveAccount(cuentas[0]);
      }
    }
  }
}
