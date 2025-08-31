import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    // Le proxy est la source des problèmes de connexion.
    // Pour simplifier et garantir que ça fonctionne, nous allons le retirer
    // et faire des appels directs au backend, ce qui est géré par CORS.
    // Si vous souhaitez remettre le proxy plus tard, il faudra une configuration plus avancée.
    // Pour l'instant, la priorité est que l'application fonctionne.
    proxy: {} // On vide la configuration du proxy.
  }
})