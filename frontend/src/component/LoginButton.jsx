// src/LoginButton.jsx
import React from 'react';
import { useMsal } from "@azure/msal-react";
import { loginRequest } from "../auth/AuthConfig";

/**
 * Componente que muestra el boton de Microsoft, o el saludo + boton de
 * salir si ya hay sesion activa. Toda la logica de auth vive en MSAL —
 * este componente solo la conecta con la UI.
 */
export function LoginButton() {
    // useMsal() es el hook que da acceso a lo que dejamos disponible en
    // <MsalProvider> (main.jsx) via React Context:
    //   - instance: el objeto PublicClientApplication, con metodos como
    //     loginRedirect(), logoutRedirect(), acquireTokenSilent()...
    //   - accounts: array de cuentas de Microsoft actualmente logueadas en
    //     esta pestaña (normalmente 0 o 1 elemento — MSAL soporta multiples
    //     cuentas simultaneas, pero acá solo usamos la primera).
    const { instance, accounts } = useMsal();

    const handleLogin = () => {
        // Flujo por redireccion: navega a la pagina de login de Microsoft y vuelve a la app.
        // Es mas fiable que el popup (evita bloqueos del navegador y popups que no se cierran).
        // loginRequest (de AuthConfig.js) le dice a Azure que solo pedimos
        // el scope basico "User.Read" en este paso — el scope de nuestra
        // propia API se pide DESPUES, en TokenSender.jsx, solo cuando
        // realmente se necesita (principio de "least privilege": pedir
        // permisos justo cuando se usan, no todos de una).
        instance.loginRedirect(loginRequest).catch(e => {
            console.error("Error en el inicio de sesión:", e);
        });
    };

    const handleLogout = () => {
        // logoutRedirect() limpia la sesion local de MSAL Y ademas navega
        // a Azure para cerrar la sesion alla tambien (single sign-out) —
        // no es solo "olvidar el token en el navegador".
        instance.logoutRedirect().catch(e => {
            console.error("Error al cerrar sesión:", e);
        });
    };

    // accounts.length > 0 es la forma estandar de MSAL para preguntar "¿hay
    // alguien logueado ahora mismo?" — no hace falta guardar un booleano
    // aparte en el estado del componente, MSAL ya lleva esa cuenta.
    if (accounts.length > 0) {
        return (
            <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                <span style={{ fontSize: '14px', color: '#333' }}>
                    {/* accounts[0].name viene directo del perfil de Microsoft
                        del usuario logueado — no lo guardamos nosotros en
                        ningun estado, MSAL lo expone automaticamente. */}
                    Hola, <strong>{accounts[0].name}</strong>
                </span>
                <button 
                    onClick={handleLogout}
                    style={{
                        backgroundColor: '#d83b01',
                        color: 'white',
                        border: 'none',
                        padding: '8px 16px',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontWeight: '600'
                    }}
                >
                    Cerrar Sesión
                </button>
            </div>
        );
    }

    // Si no ha iniciado sesión, muestra el botón oficial de Microsoft
    return (
        <button 
            onClick={handleLogin}
            style={{
                backgroundColor: '#2f2f2f',
                color: 'white',
                border: 'none',
                padding: '10px 20px',
                borderRadius: '4px',
                cursor: 'pointer',
                fontWeight: '600',
                display: 'flex',
                alignItems: 'center',
                gap: '10px'
            }}
        >
            {/* Pequeño icono simulado de Microsoft (4 cuadraditos de colores,
                sin necesidad de cargar una imagen externa) */}
            <span style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 6px)', gap: '2px' }}>
                <span style={{ backgroundColor: '#f25022', width: '6px', height: '6px' }}></span>
                <span style={{ backgroundColor: '#7fba00', width: '6px', height: '6px' }}></span>
                <span style={{ backgroundColor: '#00a4ef', width: '6px', height: '6px' }}></span>
                <span style={{ backgroundColor: '#ffb900', width: '6px', height: '6px' }}></span>
            </span>
            Iniciar sesión con Microsoft
        </button>
    );
}