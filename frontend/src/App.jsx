// src/App.jsx
//
// Componente raiz de la UI. Muy corto a proposito: toda la logica de auth
// vive en MSAL/AuthConfig/LoginButton/TokenSender — este archivo solo
// decide QUE mostrar segun si hay sesion o no.
import React from 'react';
import { LoginButton } from './component/LoginButton';
import { TokenSender } from './TokenSender';
import { useIsAuthenticated } from "@azure/msal-react";

function App() {
  // Hook de conveniencia de msal-react: devuelve true/false segun si hay
  // alguna cuenta logueada. Por dentro hace lo mismo que preguntar
  // "accounts.length > 0" con useMsal() (como en LoginButton.jsx), pero
  // esta version mas corta alcanza cuando solo necesitas el booleano.
  const isAuthenticated = useIsAuthenticated();

  return (
    <div style={{ padding: '40px', fontFamily: 'Arial, sans-serif', maxWidth: '800px', margin: '0 auto' }}>
      <h2>Dashboard Frontend - Arquitectura Cloud OIDC</h2>
      <hr style={{ margin: '20px 0' }} />

      <div style={{ marginBottom: '20px' }}>
        <LoginButton />
      </div>

      {/* Renderizado condicional: mientras no haya sesion, ni siquiera se
          monta <TokenSender /> — asi evitamos que intente pedir un token
          para un usuario que no existe. */}
      {isAuthenticated ? (
        <TokenSender />
      ) : (
        <p style={{ color: '#605e5c', fontStyle: 'italic' }}>
          Inicia sesión con tu cuenta de Microsoft para habilitar el consumo del token hacia el API Gateway.
        </p>
      )}
    </div>
  );
}

export default App;