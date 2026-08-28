import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

/** FR-10 忽略示警的理由輸入（理由必填，§FR-10-3）。 */
@Component({
  selector: 'app-ignore-reason-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>忽略此風險示警</h2>
    <div mat-dialog-content>
      <mat-form-field appearance="outline" style="width: 100%">
        <mat-label>忽略理由</mat-label>
        <textarea matInput [formControl]="reason" rows="3" required></textarea>
      </mat-form-field>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">取消</button>
      <button mat-raised-button color="warn" [disabled]="reason.invalid" (click)="dialogRef.close(reason.value)">
        確認忽略
      </button>
    </div>
  `,
})
export class IgnoreReasonDialogComponent {
  readonly dialogRef = inject(MatDialogRef<IgnoreReasonDialogComponent>);
  readonly reason = inject(FormBuilder).nonNullable.control('', [Validators.required, Validators.maxLength(300)]);
}
