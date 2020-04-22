import { AppFormsModule } from './../app-forms/app-forms.module';
import { AlertModule } from '@ui/alert/alert.module';
import { RouterModule } from '@angular/router';
import { ContactPersonResolver } from '@resolvers/contact-person.resolver';
import { ContactPersonComponent } from './contact-person/contact-person.component';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContactComponent } from './contact.component';
import { ContactRoutingModule } from './contact-routing.module';
import { AngularMaterialModule } from '../angular-material/angular-material.module';
import { ContactPersonsResolver } from '@resolvers/contact-persons.resolver';
import { ReactiveFormsModule } from '@angular/forms';
import { ConfirmationDialogModule } from '@ui/confirmation-dialog/confirmation-dialog.module';

const COMPONENTS = [
  ContactComponent,
  ContactPersonComponent,
];

@NgModule({
  imports: [
    CommonModule,
    ContactRoutingModule,
    AngularMaterialModule,
    ReactiveFormsModule,
    RouterModule,
    AlertModule,
    ConfirmationDialogModule,
    AppFormsModule
  ],
  declarations: [...COMPONENTS],
  providers: [ContactPersonsResolver, ContactPersonResolver],
})
export class ContactModule { }
