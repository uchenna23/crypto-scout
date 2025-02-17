// vite.config.ts
import { defineConfig } from 'vite';
import * as path from 'path';

export default defineConfig({
  ssr: {
    // You can experiment with marking hammerjs as external as well, if needed:
    // external: ['hammerjs'],
  },
  resolve: {
    alias: [
      {
        find: 'hammerjs',
        replacement: path.resolve(__dirname, 'empty-hammer.js'),
      },
      {
        find: 'hammerjs/hammer',
        replacement: path.resolve(__dirname, 'empty-hammer.js'),
      },
      // If other subpaths are being imported, you can add similar aliases:
      // {
      //   find: /^hammerjs\/.*/,
      //   replacement: path.resolve(__dirname, 'empty-hammer.js'),
      // },
    ],
  },
});
