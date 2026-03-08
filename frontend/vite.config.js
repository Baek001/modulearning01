import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/rest': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/common': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/mail': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/chat': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/starworks-groupware-websocket': {
        target: 'ws://localhost:18080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
