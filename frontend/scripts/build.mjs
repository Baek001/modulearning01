import { build } from 'vite';
import react from '@vitejs/plugin-react';

const projectRoot = process.cwd().replace(/\\/g, '/');

await build({
  root: projectRoot,
  resolve: {
    preserveSymlinks: true,
  },
  plugins: [react()],
});
