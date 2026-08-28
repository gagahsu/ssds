import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProductControllerService } from '../../../core/api/api/productController.service';
import { ProductDetail } from '../../../core/api/model/productDetail';

/**
 * FR-05 品項詳情的品項主檔部分。AI 洞察／評分因子明細區塊待 Track 3（AI）、
 * 評分批次排程接上後再補（見 docs/module-tasks.md）。
 */
@Component({
  selector: 'app-product-detail',
  imports: [CommonModule, MatButtonModule, MatProgressSpinnerModule, MatSnackBarModule],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
})
export class ProductDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly productService = inject(ProductControllerService);

  readonly product = signal<ProductDetail | null>(null);
  readonly loading = signal(true);

  private id!: number;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.productService.get({ id: this.id }).subscribe({
      next: (res) => {
        this.product.set(res.data ?? null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  edit(): void {
    this.router.navigateByUrl(`/products/${this.id}/edit`);
  }

  markEvaluating(): void {
    this.changeStatus('EVALUATING');
  }

  markListed(): void {
    this.changeStatus('LISTED');
  }

  private changeStatus(status: 'EVALUATING' | 'LISTED'): void {
    this.productService.changeStatus({ id: this.id, productStatusChangeRequest: { status } }).subscribe({
      next: (res) => this.product.set(res.data ?? null),
      error: (err) =>
        this.snackBar.open(err?.error?.error?.message ?? '狀態變更失敗', '關閉', { duration: 4000 }),
    });
  }

  remove(): void {
    if (!confirm('確定要刪除此品項嗎？（軟刪除，可由系統管理員還原）')) {
      return;
    }
    this.productService._delete({ id: this.id }).subscribe({
      next: () => this.router.navigateByUrl('/products'),
      error: (err) =>
        this.snackBar.open(err?.error?.error?.message ?? '刪除失敗', '關閉', { duration: 4000 }),
    });
  }
}
