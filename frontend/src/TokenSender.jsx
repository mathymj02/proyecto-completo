// src/TokenSender.jsx
//
// Este componente hace 2 cosas encadenadas: (1) le pide a MSAL un token
// valido PARA NUESTRA API (no el de login general), y (2) llama al
// backend mandando ese token en el header Authorization. Es el
// componente que conecta visualmente "el login de Microsoft" con "una
// peticion real a tu Spring Boot".
import React, { useState, useEffect } from 'react';
import { useMsal } from "@azure/msal-react";
import { apiRequest } from "./auth/AuthConfig";

// URL del backend: se puede sobreescribir con VITE_API_URL en un archivo .env
// Ruta relativa "/api/v1": el proxy de Vite (vite.config.js) la reenvia a
// http://localhost:8080 evitando CORS. NO poner "localhost:8080/..." aqui,
// fetch lo interpretaria como URL invalida dentro del origen del frontend.
//
// import.meta.env.VITE_API_URL: asi es como Vite expone las variables de
// un archivo .env al codigo del navegador. Solo las que empiezan con
// "VITE_" quedan visibles aca (por seguridad, para no filtrar variables
// sensibles del servidor sin querer). El "||" define un valor por defecto
// si no existe el .env — en este caso, la URL de produccion en AWS.
const backendUrl = import.meta.env.VITE_API_URL || "https://0t16t89h07.execute-api.us-east-1.amazonaws.com/desarrallo/api/v1";

export function TokenSender() {
    // De nuevo el hook useMsal(): "instance" para pedir el token,
    // "accounts" para saber quien esta logueado ahora mismo.
    const { instance, accounts } = useMsal();
    // 3 piezas de estado de React: el JWT que conseguimos (para mostrarlo
    // en pantalla), la respuesta que dio el backend (texto o JSON), y un
    // flag para deshabilitar el boton mientras la peticion esta en curso.
    const [tokenJWT, setTokenJWT] = useState("");
    const [apiResponse, setApiResponse] = useState("");
    const [loading, setLoading] = useState(false);

    // Adquiere el token: primero silencioso y, si requiere interaccion
    // (ej. consentimiento de un scope nuevo), navega a Azure y vuelve.
    const acquireAccessToken = async () => {
        // apiRequest trae el scope "api://.../write-read" (de AuthConfig.js).
        // Le agregamos "account: accounts[0]" para decirle a MSAL EXACTAMENTE
        // para que usuario queremos el token (MSAL podria manejar varias
        // cuentas en teoria, aca solo usamos la primera logueada).
        const request = {
            ...apiRequest,
            account: accounts[0]
        };

        try {
            // acquireTokenSilent: intenta conseguir el token SIN mostrarle
            // nada al usuario — usa el "refresh token" guardado en cache
            // desde el login inicial. Esto es lo que pasa el 99% de las
            // veces despues del primer login (rapido, invisible).
            const response = await instance.acquireTokenSilent(request);
            return response.accessToken;
        } catch (silentError) {
            // Si el silencioso falla (ej. primera vez que se pide ESTE
            // scope especifico y Azure necesita que el usuario consienta,
            // o el refresh token expiro), hay que hacer una redireccion
            // interactiva. Guardamos una bandera en sessionStorage para
            // saber, cuando la pagina recargue de vuelta, que debemos
            // reintentar automaticamente esta misma accion (ver el
            // useEffect mas abajo).
            console.warn("Fallo el token silencioso, redirigiendo a Azure:", silentError);
            sessionStorage.setItem("msal_pending_api_call", "1");
            await instance.acquireTokenRedirect(request);
            return null; // nunca se llega a usar: la pagina ya esta navegando fuera
        }
    };

    const handleGetTokenAndCallApi = async () => {
        if (accounts.length === 0) return; // nadie logueado, no hay nada que hacer

        setLoading(true);
        setApiResponse("");

        try {
            const accessToken = await acquireAccessToken();
            // Si es null, se inicio una redireccion a Azure: al volver, el
            // efecto de abajo reejecuta esta funcion automaticamente.
            if (!accessToken) return;
            setTokenJWT(accessToken);
            console.log("Token JWT obtenido con éxito:", accessToken);

            // Enviar el token al Backend / API Gateway mediante Authorization: Bearer
            // Esta es la unica linea que realmente "habla" con tu Spring
            // Boot: un GET plano con el header Authorization. Todo lo
            // anterior fue solo conseguir el valor de ese header.
            const res = await fetch(backendUrl, {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${accessToken}`
                }
            });

            // El backend puede responder texto plano o JSON; se parsea de forma tolerante.
            // Se lee siempre como texto primero (res.text()) y DESPUES se
            // intenta convertir a JSON — asi, si el backend devuelve algo
            // que no es JSON valido (ej. un mensaje de error de Spring en
            // texto plano), igual lo podemos mostrar sin que la app explote.
            const text = await res.text();
            let data;
            try {
                data = JSON.parse(text);
            } catch {
                data = text;
            }

            // res.ok es true solo para status 200-299. Un 401/404/500 cae
            // en el "else" aunque la peticion HTTP en si haya funcionado
            // (fetch NO tira excepcion por códigos de error, hay que
            // chequear res.ok a mano — a diferencia de axios, por ejemplo).
            if (!res.ok) {
                setApiResponse(`HTTP ${res.status} ${res.statusText}\n\n${typeof data === "string" ? data : JSON.stringify(data, null, 2)}`);
            } else {
                setApiResponse(typeof data === "string" ? data : JSON.stringify(data, null, 2));
            }

        } catch (error) {
            // Esto atrapa errores de RED (backend caido, CORS bloqueado,
            // sin internet) — distinto de un 401/500, que SI llega como
            // respuesta HTTP y se maneja arriba.
            console.error("Error al adquirir el token o consultar la API:", error);
            setApiResponse("Error crítico de autenticación: " + error.message);
        } finally {
            setLoading(false);
        }
    };

    // Si la pagina recargo por una redireccion de consentimiento, reejecuta
    // automaticamente la llamada que quedo pendiente.
    //
    // useEffect con array de dependencias vacio ([]): se ejecuta UNA sola
    // vez, justo cuando este componente aparece en pantalla (equivalente a
    // "componentDidMount" en componentes de clase). Aca se usa para
    // retomar el flujo exactamente donde quedo antes de que
    // acquireTokenRedirect() sacara al usuario de la app.
    useEffect(() => {
        if (sessionStorage.getItem("msal_pending_api_call") === "1") {
            sessionStorage.removeItem("msal_pending_api_call");
            handleGetTokenAndCallApi();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Si nadie ha iniciado sesion, este componente no renderiza nada
    // (App.jsx ya se encarga de mostrar un mensaje en ese caso).
    if (accounts.length === 0) return null;

    return (
        <div style={{ marginTop: '20px', padding: '20px', backgroundColor: '#f9f9f9', border: '1px solid #ddd', borderRadius: '6px' }}>
            <h3>Prueba de envío de Token al Backend</h3>
            <p style={{ fontSize: '14px', color: '#555' }}>
                Haz clic para obtener tu Token JWT de Azure y enviarlo en el header <code style={{ background: '#eee', padding: '2px 4px' }}>Authorization: Bearer &lt;token&gt;</code>.
            </p>

            <button
                onClick={handleGetTokenAndCallApi}
                disabled={loading}
                style={{
                    backgroundColor: '#107c10',
                    color: 'white',
                    border: 'none',
                    padding: '10px 18px',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontWeight: '600',
                    marginTop: '10px'
                }}
            >
                {loading ? "Obteniendo Token..." : "Obtener Token y Enviar al Backend"}
            </button>

            {tokenJWT && (
                <div style={{ marginTop: '15px' }}>
                    <h4>Token JWT Capturado:</h4>
                    <textarea
                        readOnly
                        value={tokenJWT}
                        rows={4}
                        style={{ width: '100%', fontFamily: 'monospace', fontSize: '11px', padding: '8px', background: '#fff' }}
                    />
                </div>
            )}

            {apiResponse && (
                <div style={{ marginTop: '15px' }}>
                    <h4>Respuesta del Backend / API Gateway:</h4>
                    <pre style={{ background: '#333', color: '#adff2f', padding: '10px', borderRadius: '4px', overflowX: 'auto', fontSize: '12px' }}>
                        {apiResponse}
                    </pre>
                </div>
            )}
        </div>
    );
}
