// vite.config.ts
import { defineConfig } from 'vite';
import * as path from 'path';

export default defineConfig({
  // You can also try marking hammerjs as external in SSR if needed:
  // ssr: { external: ['hammerjs'] },
  resolve: {
    alias: [
      {
        // This regex matches imports for "hammerjs" or "hammerjs/hammer"
        find: /^hammerjs(\/hammer)?$/,
        replacement: path.resolve(__dirname, 'empty-hammer.js'),
      },
    ],
  },
});
