// src/app/services/auth-api.service.ts
//
// @Injectable({ providedIn: 'root' }): registra este servicio en el
// injector RAIZ de Angular - una unica instancia compartida por toda la
// app (singleton), sin tener que declararlo a mano en ningun modulo.
//
// Nota clave: NINGUNO de estos metodos agrega el header Authorization a
// mano. Eso lo hace MsalInterceptor automaticamente (ver auth-config.ts,
// protectedResourceMap) apenas detecta que la URL de la peticion esta en
// el mapa de recursos protegidos. Este servicio es "tonto" a proposito:
// solo arma la peticion HTTP, la seguridad vive en otra capa.
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  constructor(private http: HttpClient) {}

  /** Llama a GET /api/v1/auth/me - devuelve los claims que el backend leyo del JWT ya validado. */
  obtenerMisClaims(): Observable<any> {
    return this.http.get(`${environment.apiUrls.auth}/me`);
  }
}
