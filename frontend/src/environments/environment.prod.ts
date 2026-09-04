// src/environments/environment.prod.ts
//
// Mismos campos que environment.ts, pero con los valores que usaras una
// vez desplegado en AWS. Angular intercambia automaticamente este archivo
// por el de arriba cuando compilas con "ng build" (produccion es el
// default de "ng build" desde Angular 17+, ver "fileReplacements" en angular.json).
export const environment = {
  production: true,

  azureAd: {
    clientId: 'TU-CLIENT-ID-AQUI',
    authority: 'https://login.microsoftonline.com/common',
    // Cambiar por la URL de tu API Gateway + stage donde sirvas el frontend,
    // o por el dominio donde despliegues el build de Angular.
    redirectUri: 'https://TU-DOMINIO-DE-PRODUCCION/',
    postLogoutRedirectUri: 'https://TU-DOMINIO-DE-PRODUCCION/',
  },

  apiScopes: ['api://TU-CLIENT-ID-AQUI/write-read'],

  // En produccion, las 3 URLs deberian apuntar al MISMO API Gateway
  // (distintas rutas de un solo dominio), no a puertos sueltos como en local.
  apiUrls: {
    auth: 'https://TU-API-GATEWAY.execute-api.us-east-1.amazonaws.com/desarrollo/api/v1/auth',
    catalogo: 'https://TU-API-GATEWAY.execute-api.us-east-1.amazonaws.com/desarrollo/api/v1/catalogo',
    carrito: 'https://TU-API-GATEWAY.execute-api.us-east-1.amazonaws.com/desarrollo/api/v1/carritos',
  },
};
