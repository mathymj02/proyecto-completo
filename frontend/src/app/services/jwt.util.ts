// src/app/services/jwt.util.ts
//
// Un JWT tiene 3 partes separadas por puntos: header.payload.signature.
// Cada parte esta codificada en Base64URL (una variante de Base64 que usa
// "-"/"_" en vez de "+"/"/" para que sea seguro meterlo en una URL).
// Esta funcion decodifica SOLO el payload (los claims) para poder
// mostrarlos en pantalla - NO valida la firma (eso es trabajo exclusivo
// del backend/API Gateway, nunca confies en un JWT solo por poder leerlo:
// cualquiera puede leer un JWT, pero solo quien tiene la clave privada del
// tenant pudo haberlo FIRMADO).
export function decodeJwtPayload(token: string): Record<string, any> | null {
  try {
    const payloadBase64Url = token.split('.')[1];
    // Revierte Base64URL a Base64 estandar antes de decodificar.
    const payloadBase64 = payloadBase64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(payloadBase64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('No se pudo decodificar el token:', error);
    return null;
  }
}

/**
 * Extrae los roles/scopes del payload ya decodificado. Azure AD puede
 * traer permisos en 2 claims distintos segun como esten configurados en
 * la App Registration:
 *   - "roles": App Roles asignados al usuario o la app (requiere
 *     configurarlos en "App roles" + asignar el rol al usuario).
 *   - "scp" (scope): permisos delegados que el usuario consintio (lo que
 *     nosotros usamos, via "Expose an API" con el scope "write-read").
 */
export function extraerPermisos(payload: Record<string, any> | null): string[] {
  if (!payload) return [];
  const roles: string[] = payload['roles'] ?? [];
  const scopes: string[] = payload['scp'] ? payload['scp'].split(' ') : [];
  return [...roles, ...scopes];
}
