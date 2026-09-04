// src/app/components/dashboard/dashboard.component.ts
//
// Esta pantalla solo se puede ver si MsalGuard + roleGuard dejaron pasar
// (ver app.routes.ts). Al llegar aca, YA hay una sesion activa con token -
// por eso los 3 servicios (AuthApiService, CatalogoService, CarritoService)
// se pueden llamar directo, sin pedir el token a mano: MsalInterceptor se
// encarga de pegarlo en cada request saliente.
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MsalService } from '@azure/msal-angular';
import { AuthApiService } from '../../services/auth-api.service';
import { CatalogoService, Producto } from '../../services/catalogo.service';
import { CarritoService } from '../../services/carrito.service';
import { decodeJwtPayload, extraerPermisos } from '../../services/jwt.util';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  private msalService = inject(MsalService);
  private authApiService = inject(AuthApiService);
  private catalogoService = inject(CatalogoService);
  private carritoService = inject(CarritoService);

  // Estado de la pantalla. Angular no tiene un equivalente directo a
  // useState de React - simplemente son propiedades normales de la clase,
  // y el template HTML se re-renderiza solo cuando Angular detecta un
  // cambio (deteccion de cambios automatica, no hay que "setear" nada
  // especial como setState).
  claimsBackend: any = null;
  productos: Producto[] = [];
  carrito: any = null;
  permisosDelToken: string[] = [];
  cargando = true;
  error: string | null = null;

  ngOnInit(): void {
    this.cargarTodo();
  }

  private cargarTodo(): void {
    this.cargando = true;
    this.error = null;

    // Lee los permisos directo del Access Token guardado en cache de MSAL,
    // para mostrar en pantalla (satisface el punto de la rubrica "leen
    // roles y scopes desde los claims del token").
    this.msalService.instance
      .acquireTokenSilent({
        scopes: environment.apiScopes,
        account: this.msalService.instance.getActiveAccount() ?? undefined,
      })
      .then((resultado) => {
        const payload = decodeJwtPayload(resultado.accessToken);
        this.permisosDelToken = extraerPermisos(payload);
      })
      .catch((err) => console.warn('No se pudo leer el token para mostrar permisos:', err));

    // Llama a las 3 APIs EN PARALELO (no hace falta esperar una para
    // empezar la otra, son independientes). Cada .subscribe() maneja su
    // propio exito/error por separado.
    this.authApiService.obtenerMisClaims().subscribe({
      next: (data) => (this.claimsBackend = data),
      error: (err) => console.error('Error en auth-api:', err),
    });

    this.catalogoService.listar().subscribe({
      next: (data) => {
        this.productos = data;
        this.cargando = false;
      },
      error: (err) => {
        this.error = `Error consultando catalogo-api: ${err.status} ${err.statusText}`;
        this.cargando = false;
      },
    });

    this.carritoService.obtenerMiCarrito().subscribe({
      next: (data) => (this.carrito = data),
      error: (err) => console.error('Error en carrito-api:', err),
    });
  }

  /** Agrega el primer producto del catalogo al carrito, como demo rapida del flujo completo. */
  agregarPrimerProductoAlCarrito(): void {
    if (this.productos.length === 0) return;
    const producto = this.productos[0];

    this.carritoService
      .agregarItem({
        productoId: producto.id!,
        nombreProducto: producto.nombre,
        precioUnitario: producto.precio,
        cantidad: 1,
      })
      .subscribe({
        next: (carritoActualizado) => (this.carrito = carritoActualizado),
        error: (err) => console.error('Error agregando al carrito:', err),
      });
  }
}
