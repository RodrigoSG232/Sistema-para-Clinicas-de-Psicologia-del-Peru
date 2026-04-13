import { Routes } from '@angular/router';
import { MainLayout } from './layout/main-layout/main-layout';
import { Recepcion } from './modules/recepcion/recepcion';
import { Caja } from './modules/caja/caja';
import { Psicologia } from './modules/psicologia/psicologia';
import { LoginComponent } from './modules/login/login';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },

  {
    path: '',
    component: MainLayout,
    children: [
      { path: 'recepcion', component: Recepcion },
      { path: 'caja', component: Caja },
      { path: 'psicologia', component: Psicologia },
    ]
  },
  { path: '**', redirectTo: 'login' }
];