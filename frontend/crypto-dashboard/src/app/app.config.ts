import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { MenubarModule } from 'primeng/menubar';
import { ButtonModule } from 'primeng/button';
import { provideAnimations } from '@angular/platform-browser/animations';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    importProvidersFrom(MenubarModule, ButtonModule)
  ]
};
