import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RiskAlertControllerService } from '../../core/api/api/riskAlertController.service';
import { RiskAlertListItem } from '../../core/api/model/riskAlertListItem';
import { IgnoreReasonDialogComponent } from './ignore-reason-dialog.component';

/** FR-10 風險示警中心。門檻調整（§FR-10-3）待評分批次排程接上後再做。 */
@Component({
  selector: 'app-risks',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  templateUrl: './risks.component.html',
  styleUrl: './risks.component.scss',
})
export class RisksComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly riskService = inject(RiskAlertControllerService);

  readonly displayedColumns = [
    'productName', 'riskType', 'severity', 'triggerValue', 'detectedAt', 'status', 'actions',
  ];

  readonly rows = signal<RiskAlertListItem[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(false);

  pageIndex = 0;
  pageSize = 20;

  readonly filterForm = this.fb.nonNullable.group({
    status: [null as RiskAlertListItem.StatusEnum | null],
    severity: [null as RiskAlertListItem.SeverityEnum | null],
  });

  readonly StatusEnum = RiskAlertListItem.StatusEnum;
  readonly SeverityEnum = RiskAlertListItem.SeverityEnum;

  ngOnInit(): void {
    this.filterForm.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.load();
    });
    this.load();
  }

  load(): void {
    const f = this.filterForm.getRawValue();
    this.loading.set(true);
    this.riskService
      .listRisks({
        pageable: { page: this.pageIndex, size: this.pageSize },
        status: f.status ?? undefined,
        severity: f.severity ?? undefined,
      })
      .subscribe({
        next: (res) => {
          this.rows.set(res.data?.content ?? []);
          this.totalElements.set(res.data?.totalElements ?? 0);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  acknowledge(row: RiskAlertListItem): void {
    this.riskService.acknowledge({ id: row.id! }).subscribe({
      next: () => this.load(),
      error: (err) =>
        this.snackBar.open(err?.error?.error?.message ?? '操作失敗', '關閉', { duration: 4000 }),
    });
  }

  ignore(row: RiskAlertListItem): void {
    const ref = this.dialog.open(IgnoreReasonDialogComponent);
    ref.afterClosed().subscribe((reason: string | undefined) => {
      if (!reason) {
        return;
      }
      this.riskService.ignore({ id: row.id!, riskIgnoreRequest: { reason } }).subscribe({
        next: () => this.load(),
        error: (err) =>
          this.snackBar.open(err?.error?.error?.message ?? '操作失敗', '關閉', { duration: 4000 }),
      });
    });
  }

  goToProduct(row: RiskAlertListItem): void {
    this.router.navigateByUrl(`/products/${row.productId}`);
  }
}
