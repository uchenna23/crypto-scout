// vite.config.ts
import { defineConfig } from 'vite';
import * as path from 'path';

const isSSR = process.env['SSR'] === 'true';

export default defineConfig({
  define: isSSR
    ? {
        // ✅ Define a dummy window only for SSR builds
        window: {},
      }
    : {},
  ssr: {
    // You can also try marking hammerjs as external (optional)
    // external: ['hammerjs'],
  },
  resolve: {
    alias: [
      // ✅ Explicitly alias all possible hammerjs import paths
      {
        find: 'hammerjs',
        replacement: path.resolve(__dirname, 'empty-hammer.js'),
      },
      {
        find: 'hammerjs/hammer',
        replacement: path.resolve(__dirname, 'empty-hammer.js'),
      },
      {
        find: 'hammerjs/hammer.js',
        replacement: path.resolve(__dirname, 'empty-hammer.js'),
      },
    ],
  },
});
