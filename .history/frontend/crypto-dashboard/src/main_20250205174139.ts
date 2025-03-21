import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { HttpClientModule } from '@angular/common/http';
import './polyfills';

bootstrapApplication(AppComponent, {
  ...appConfig,
  providers: [
    ...(appConfig.providers || []), // ✅ Ensure existing providers are preserved
    HttpClientModule               // ✅ Add HttpClientModule as a global provider
  ]
})
  .catch((err) => console.error(err));
