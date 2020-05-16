import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AngularMaterialModule } from './../angular-material/angular-material.module';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChangePasswordComponent } from './change-password.component';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    CommonModule,
    AngularMaterialModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild([{
      path: '',
      pathMatch: 'full',
      component: ChangePasswordComponent,
    }])
  ],
  declarations: [ChangePasswordComponent]
})
export class ChangePasswordModule { }
