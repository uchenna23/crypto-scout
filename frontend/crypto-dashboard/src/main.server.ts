import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { config } from './app/app.config.server';

const bootstrap = () => bootstrapApplication(AppComponent, config);
if (typeof window === 'undefined') {
    // Provide a dummy window to avoid "window is not defined" errors.
    // This is only for SSR and should not affect client-side behavior.
    globalThis.window = {} as any;
  }
export default bootstrap;
