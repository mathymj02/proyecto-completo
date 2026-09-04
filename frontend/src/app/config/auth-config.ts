// src/app/config/auth-config.ts
//
// Este es el archivo equivalente a "AuthConfig.js" de la version React,
// pero con el patron OFICIAL de @azure/msal-angular: en vez de exportar un
// objeto de configuracion suelto, exportamos 3 FUNCIONES "factory" que
// Angular usa para construir 3 piezas independientes:
//
//   1. MSALInstanceFactory      -> LA instancia de MSAL (quien soy, que tenant)
//   2. MSALGuardConfigFactory   -> que hace MsalGuard cuando protege una ruta
//   3. MSALInterceptorConfigFactory -> a que URLs el interceptor le agrega el token
//
// Las 3 se conectan a Angular en app.config.ts via Dependency Injection
// (los "providers"). Este patron (factory functions) es requisito de
// @azure/msal-angular, no una eleccion de estilo nuestra.
import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType,
  BrowserCacheLocation,
  LogLevel,
} from '@azure/msal-browser';
import {
  MsalGuardConfiguration,
  MsalInterceptorConfiguration,
} from '@azure/msal-angular';
import { environment } from '../../environments/environment';

/**
 * Crea la instancia de MSAL (PublicClientApplication) con los datos de tu
 * App Registration. Es LO MISMO que "new PublicClientApplication(msalConfig)"
 * en la version React — solo que aca Angular la pide como una funcion
 * factory para poder inyectarla donde haga falta.
 */
export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.azureAd.clientId,
      authority: environment.azureAd.authority,
      redirectUri: environment.azureAd.redirectUri,
      postLogoutRedirectUri: environment.azureAd.postLogoutRedirectUri,
    },
    cache: {
      // sessionStorage: la sesion se pierde al cerrar la pestaña (mas
      // seguro para un examen/demo que localStorage, que persiste).
      // (Nota: la opcion "storeAuthStateInCookie", que existia en
      // versiones viejas de MSAL como parche para IE11, ya no existe en
      // esta version de la libreria - no hace falta reemplazarla por nada).
      cacheLocation: BrowserCacheLocation.SessionStorage,
    },
    system: {
      loggerOptions: {
        // Redirige los logs internos de MSAL a la consola del navegador,
        // filtrando los mas ruidosos (Verbose) para no ensuciar la consola.
        loggerCallback: (level: LogLevel, message: string) => {
          if (level === LogLevel.Error) console.error(message);
        },
      },
    },
  });
}

/**
 * Configura QUE HACE MsalGuard cuando alguien entra a una ruta protegida
 * sin sesion. InteractionType.Redirect = lo manda a login.microsoftonline.com
 * automaticamente (equivalente a llamar loginRedirect() a mano, pero
 * Angular lo hace por vos apenas detecta la ruta protegida en app.routes.ts).
 *
 * "authRequest.scopes" define que permisos pide ESTE login inicial - acá
 * pedimos de una vez el scope de nuestra API, asi no hace falta un segundo
 * pedido de consentimiento como pasaba en la version React (loginRequest
 * con solo User.Read, y luego apiRequest por separado).
 */
export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: [...environment.apiScopes],
    },
  };
}

/**
 * Configura el INTERCEPTOR HTTP: la pieza que agrega automaticamente el
 * header "Authorization: Bearer <token>" a las peticiones que salgan de tu
 * app, SIN que cada servicio (CatalogoService, CarritoService, etc.) tenga
 * que hacerlo a mano. Es el reemplazo directo de lo que en TokenSender.jsx
 * haciamos manualmente con fetch() + headers.
 *
 * "protectedResourceMap" es un Map<url, scopes[]>: le dice al interceptor
 * "cuando la peticion vaya a ESTA url, pegale el token pedido con ESTOS
 * scopes". Usamos el ORIGEN completo (protocolo + host + puerto, ej.
 * "http://localhost:8081") en vez del path completo — es más robusto:
 * cualquier endpoint de ese microservicio (actual o futuro) queda
 * cubierto automaticamente, sin tener que listar cada ruta una por una,
 * y evita problemas sutiles de coincidencia exacta de path.
 */
export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string> | null>();

  // IMPORTANTE: esta version de @azure/msal-angular usa "strict matching"
  // por defecto — compara cada componente de la URL (protocolo, host,
  // path, etc) con una expresion regular ANCLADA (^...$), no con un
  // simple "empieza con". Eso significa que la clave DEBE terminar en
  // "/*" para cubrir cualquier ruta dentro de ese origen — sin el "/*",
  // solo matchearia una URL exactamente igual a la clave (por ejemplo
  // "http://localhost:8081" a secas NUNCA calza con
  // "http://localhost:8081/api/v1/catalogo/productos").
  protectedResourceMap.set(`${new URL(environment.apiUrls.auth).origin}/*`, environment.apiScopes);
  protectedResourceMap.set(`${new URL(environment.apiUrls.catalogo).origin}/*`, environment.apiScopes);
  protectedResourceMap.set(`${new URL(environment.apiUrls.carrito).origin}/*`, environment.apiScopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}
