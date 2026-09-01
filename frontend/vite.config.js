import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
// Nota: el frontend llama directo a la URL definida en VITE_API_URL (ver .env),
// no hay proxy de Vite configurado. El backend permite el origen
// http://localhost:5173 en su configuracion de CORS (SecurityConfig#corsConfigurationSource).
export default defineConfig({
  plugins: [react()],
  base: './'
})
