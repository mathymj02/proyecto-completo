// src/auth/AuthConfig.js
//
// Este archivo NO ejecuta nada — solo define objetos de configuracion que
// otros archivos importan (main.jsx, LoginButton.jsx, TokenSender.jsx).
// Es el unico lugar donde deberian vivir los datos de tu registro en
// Azure AD, para no repetirlos por todo el codigo.

export const msalConfig = {
    auth: {
        // El "Application (client) ID": identifica TU app dentro de Azure AD.
        // Es publico (va en el frontend, cualquiera puede verlo en el
        // navegador) — no es un secreto, es como un nombre de usuario de
        // la app, no una contraseña.
        clientId: "57750927-6116-478c-a047-d06caa8fcd00",
        // "authority" = de que tenant (organizacion) de Azure AD acepta
        // logins esta app. Con esto, MSAL arma la URL de login como
        // https://login.microsoftonline.com/{tenant-id}/... Si quisieras
        // aceptar cuentas de CUALQUIER organizacion (multi-tenant) o
        // cuentas personales de Microsoft, aca iria "common" en vez del
        // tenant-id especifico.
        authority: "https://login.microsoftonline.com/common",
        // A donde debe volver el navegador despues de que el usuario haga
        // login en Azure. TIENE que coincidir EXACTO (incluido el "/" final)
        // con una de las "Redirect URIs" configuradas en el registro de la
        // app en el portal de Azure, o el login falla con
        // "redirect_uri_mismatch". Si cambias de local a produccion (AWS),
        // este valor tiene que cambiar tambien.
        redirectUri: "http://localhost:5173/",
        // A donde volver despues de cerrar sesion (logout). Misma regla que redirectUri.
        postLogoutRedirectUri: "http://localhost:5173/",
    },
    cache: {
        // Donde guarda MSAL el token y los datos de sesion en el navegador.
        // "sessionStorage" (lo que usamos aca) se borra al cerrar la pestaña
        // — mas seguro para un examen/demo. "localStorage" persiste entre
        // sesiones del navegador (util para "mantener sesion iniciada" en
        // apps reales, pero mas riesgoso si el dispositivo es compartido).
        cacheLocation: "sessionStorage",
        // Guardar el estado de auth tambien en una cookie ademas del storage
        // de arriba. Solo hace falta activarlo si tienes usuarios con
        // navegadores muy viejos que tienen problemas con sessionStorage/localStorage.
        storeAuthStateInCookie: false,
    },
};

// "Scopes" = que permisos le estas pidiendo a Microsoft cuando el usuario
// hace login. Este primer grupo (loginRequest) es el MINIMO para poder
// iniciar sesion e identificar quien es el usuario.
export const loginRequest = {
    // "User.Read" es un permiso de Microsoft Graph (la API general de
    // Microsoft 365): permite leer el perfil basico del usuario logueado
    // (nombre, email, foto). Es el scope estandar que casi toda app pide
    // al hacer login con Microsoft, aunque nunca llames a Graph realmente.
    scopes: ["User.Read"]
};

// Scopes para consumir TU PROPIO backend (resource server).
// Requiere haber configurado "Expose an API" en el registro de la app en Azure:
// Application ID URI: api://e5ece131-cd6a-469c-b21b-c69aa689316f y scope .read-write .
// Un token con este scope SI es un JWT firmado por el tenant que Spring puede validar;
// los de Graph vienen encriptados y siempre fallarian.
//
// Diferencia clave con loginRequest: el token que devuelve Microsoft para
// el scope "User.Read" esta pensado para llamar a Microsoft Graph, y NO
// es un JWT que tu propio backend pueda validar (viene cifrado/opaco para
// terceros). En cambio, un scope de la forma "api://<tu-client-id>/algo"
// le dice a Azure "quiero un token PARA MI PROPIA API" — ese si es un JWT
// firmado (RS256) que Spring Security puede decodificar y validar con las
// llaves publicas del tenant (ver SecurityConfig.java del backend).
export const apiRequest = {
    scopes: ["api://57750927-6116-478c-a047-d06caa8fcd00/desarrollo/write-read"]
};