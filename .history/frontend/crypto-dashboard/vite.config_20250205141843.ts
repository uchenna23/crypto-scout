// vite.config.ts
import { defineConfig } from 'vite';
import * as path from 'path';

export default defineConfig({
  // This ensures that during SSR, hammerjs is replaced with our empty stub.
  ssr: {
    noExternal: ['hammerjs'],
  },
  resolve: {
    alias: [
      {
        find: 'hammerjs',
        replacement: path.resolve(__dirname, 'empty-hammer.js'),
      },
    ],
  },
});
