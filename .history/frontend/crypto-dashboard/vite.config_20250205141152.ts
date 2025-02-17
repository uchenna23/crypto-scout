import { defineConfig } from 'vite';

export default defineConfig({
  ssr: {
    noExternal: ['hammerjs'], // Prevent Vite from processing hammerjs during SSR
  },
});
