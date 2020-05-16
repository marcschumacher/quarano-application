import { SnackbarService } from './../../services/snackbar.service';
import { SubSink } from 'subsink';
import { UserService } from './../../services/user.service';
import { MatInput } from '@angular/material/input';
import { FormGroup, Validators, FormControl } from '@angular/forms';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ConfirmValidPasswordMatcher } from '@validators/ConfirmValidPasswordMatcher';
import { PasswordValidator } from '@validators/password-validator';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss']
})
export class ChangePasswordComponent implements OnInit, OnDestroy {
  formGroup: FormGroup;
  confirmValidParentMatcher = new ConfirmValidPasswordMatcher();
  private subs = new SubSink();

  constructor(
    private userService: UserService,
    private snackbarService: SnackbarService,
    private router: Router) { }

  ngOnInit() {
    this.createForm();
    this.subs.add(this.userService.user$
      .subscribe(user => this.formGroup.controls.username.setValue(user.username)));
  }

  ngOnDestroy() {
    this.subs.unsubscribe();
  }

  createForm() {
    this.formGroup = new FormGroup({
      username: new FormControl({ value: '', disabled: true }),
      oldPassword: new FormControl(null, [
        Validators.required]),
      password: new FormControl(null, [
        Validators.required, PasswordValidator.secure
      ]),
      passwordConfirm: new FormControl(null, [
        Validators.required
      ]),
    }, {
      validators: [PasswordValidator.mustMatch, PasswordValidator.mustNotIncludeUsername]
    });
  }

  submitForm() {
    if (this.formGroup.valid) {
      // ToDo: Backend aufrufen, wenn fertig
      this.snackbarService.success('Ihr Passwort wurde geändert');
      this.router.navigate(['/welcome']);
    }
  }

  trimValue(input: MatInput) {
    input.value = input.value.trim();
  }
}
