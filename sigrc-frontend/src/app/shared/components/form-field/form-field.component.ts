import { Component, Input } from '@angular/core';
import { NgIf } from '@angular/common';
import { AbstractControl } from '@angular/forms';

@Component({
  selector: 'app-form-field',
  standalone: true,
  imports: [NgIf],
  template: `
    <div class="mb-3">
      <label class="form-label">
        {{ label }}
        <span class="text-danger" *ngIf="required">*</span>
        <span class="help-tip ms-1" *ngIf="help" [title]="help">?</span>
      </label>
      <ng-content></ng-content>
      <div class="validation-feedback" *ngIf="control && control.invalid && (control.dirty || control.touched)">
        <span *ngIf="control.errors?.['required']">{{ requiredMessage || 'Este campo es obligatorio' }}</span>
        <span *ngIf="control.errors?.['email']">Ingrese un correo electrónico válido</span>
        <span *ngIf="control.errors?.['minlength']">Mínimo {{ control.errors?.['minlength'].requiredLength }} caracteres</span>
        <span *ngIf="control.errors?.['maxlength']">Máximo {{ control.errors?.['maxlength'].requiredLength }} caracteres</span>
        <span *ngIf="control.errors?.['pattern']">Formato inválido</span>
        <span *ngIf="control.errors?.['min']">Valor mínimo: {{ control.errors?.['min'].min }}</span>
        <span *ngIf="control.errors?.['max']">Valor máximo: {{ control.errors?.['max'].max }}</span>
      </div>
      <div class="form-text" *ngIf="hint && (!control || !control.invalid || (!control.dirty && !control.touched))">
        {{ hint }}
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
  `]
})
export class FormFieldComponent {
  @Input() label = '';
  @Input() required = false;
  @Input() help = '';
  @Input() hint = '';
  @Input() control?: AbstractControl | null;
  @Input() requiredMessage = 'Este campo es obligatorio';
}
