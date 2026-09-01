// src/main.jsx
//
// Este es el punto de entrada de toda la app React. Ademas de lo normal
// (montar <App /> en el DOM), aca vive la inicializacion de MSAL — la
// libreria de Microsoft que maneja todo el protocolo OIDC/OAuth2 (abrir
// la pantalla de login, guardar el token, refrescarlo, etc). Por eso este
// archivo es mas largo de lo tipico en un proyecto React: MSAL exige
// hacer algunos pasos ANTES de poder renderizar la app.
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import './index.css';

import { PublicClientApplication, EventType } from "@azure/msal-browser";
import { MsalProvider } from "@azure/msal-react";
import { msalConfig } from "./auth/AuthConfig";


// PublicClientApplication es la clase central de MSAL: representa "esta
// app registrada en Azure AD". Se construye UNA sola vez con la config de
// AuthConfig.js (clientId, tenant, redirectUri) y esa misma instancia se
// comparte con toda la app via <MsalProvider> mas abajo.
const msalInstance = new PublicClientApplication(msalConfig);

// Suscribirse a eventos de MSAL: cada vez que pasa algo (login exitoso,
// token adquirido, error, etc.) MSAL dispara un evento con ese "eventType".
// Aca solo nos interesan los de FALLO, para verlos claritos en consola en
// vez de que se pierdan silenciosamente.
msalInstance.addEventCallback((event) => {
  if (
    event.eventType === EventType.LOGIN_FAILURE ||
    event.eventType === EventType.ACQUIRE_TOKEN_FAILURE
  ) {
    console.error("Fallo de MSAL:", event.error);
  }
});

// Si algo sale mal durante la inicializacion (ej. Azure rechaza el login),
// en vez de dejar la pantalla en blanco mostramos el error directo en el
// HTML — util mientras se depura, ya que en este punto React todavia no
// esta montado y no podemos mostrar un componente normal.
function mostrarErrorPantalla(titulo, detalle) {
  document.getElementById('root').innerHTML =
    '<div style="font-family:Arial;padding:20px">' +
    `<h3 style="color:#d83b01">${titulo}</h3>` +
    `<pre style="background:#f3f2f1;padding:10px;border-radius:4px;white-space:pre-wrap">` +
    `${detalle}\n\nRecarga la pagina para reintentar. Si el error menciona AADSTS650053 o AADSTS65001,\nrevisa la configuracion de "Expose an API" en el registro de la aplicacion de Azure.` +
    '</pre></div>';
}

// Extrae los campos utiles de un error de MSAL (trae un formato propio,
// distinto al Error nativo de JS) para mostrarlos de forma legible.
function detalleError(error) {
  return [
    `codigo: ${error?.errorCode ?? "(sin codigo)"}`,
    `mensaje: ${error?.errorMessage ?? error?.message}`,
    `correlationId: ${error?.correlationId ?? "(sin correlationId)"}`,
    error?.errorDescription ? `descripcion: ${error.errorDescription}` : ""
  ].filter(Boolean).join("\n");
}

// Toda la inicializacion es asincrona, asi que se envuelve en una funcion
// aparte (no se puede poner "await" suelto al nivel superior del archivo
// en todos los entornos, y ademas asi controlamos el orden exacto de los
// pasos con un try/catch claro).
async function bootstrap() {
  // Paso 1: MSAL exige llamar a initialize() ANTES de cualquier otra cosa
  // (login, adquirir token, etc). Es una inicializacion interna de la
  // libreria (carga configuracion del navegador, cache, etc).
  await msalInstance.initialize();

  // Paso 2: revisa si esta carga de la pagina es en realidad un "regreso"
  // desde Azure. Recorda que LoginButton usa loginRedirect() — eso navega
  // FUERA de tu app hacia login.microsoftonline.com, el usuario inicia
  // sesion alla, y Azure lo manda de vuelta a tu redirectUri con el
  // resultado en la URL. handleRedirectPromise() es lo que lee ese
  // resultado (si existe) y termina de armar la sesion. Si el usuario
  // solo esta entrando normal a la pagina (no viene de un redirect),
  // esto simplemente devuelve null y sigue de largo.
  try {
    const response = await msalInstance.handleRedirectPromise();
    if (response) {
      console.info("Autenticacion por redirect completada:", {
        usuario: response.account?.username,
        scopes: response.scopes
      });
    }
  } catch (error) {
    // Un fallo del protocolo (scope inexistente, consentimiento denegado, etc.)
    // llega aqui con el detalle en la descripcion: se muestra y se detiene.
    console.error("Error al procesar la respuesta de Azure:", error);
    window.history.replaceState(null, "", window.location.pathname);
    mostrarErrorPantalla("Azure devolvio un error al procesar el token", detalleError(error));
    return; // no renderizamos <App/> si el login vino roto
  }

  // Paso 3: recien ahora es seguro renderizar la app. <MsalProvider>
  // envuelve TODO el arbol de componentes y comparte "msalInstance" via
  // React Context — por eso LoginButton.jsx y TokenSender.jsx pueden usar
  // el hook useMsal() sin que nosotros les pasemos la instancia a mano
  // por props.
  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <MsalProvider instance={msalInstance}>
        <App />
      </MsalProvider>
    </React.StrictMode>
  );
}

bootstrap().catch((error) => {
  console.error("Error al inicializar MSAL:", error);
  mostrarErrorPantalla("No se pudo inicializar la autenticación de Azure", detalleError(error));
});