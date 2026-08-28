import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProductControllerService } from '../../../core/api/api/productController.service';
import { CategoryControllerService } from '../../../core/api/api/categoryController.service';
import { SupplierControllerService } from '../../../core/api/api/supplierController.service';
import { CategoryOption } from '../../../core/api/model/categoryOption';
import { SupplierOption } from '../../../core/api/model/supplierOption';

/** FR-03-2 品項新增/編輯。A 軌必填成本+建議售價，B 軌必填至少一個關聯關鍵字。 */
@Component({
  selector: 'app-product-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.scss',
})
export class ProductFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly productService = inject(ProductControllerService);
  private readonly categoryService = inject(CategoryControllerService);
  private readonly supplierService = inject(SupplierControllerService);

  readonly categories = signal<CategoryOption[]>([]);
  readonly suppliers = signal<SupplierOption[]>([]);
  readonly saving = signal(false);
  readonly loading = signal(false);

  productId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    categoryId: [null as number | null, Validators.required],
    supplierId: [null as number | null],
    trackType: ['A' as 'A' | 'B', Validators.required],
    cost: [null as number | null],
    suggestedPrice: [null as number | null],
    moq: [null as number | null],
    shelfLifeDays: [null as number | null],
    logisticsCondition: [''],
  });

  get isBTrack(): boolean {
    return this.form.controls.trackType.value === 'B';
  }

  ngOnInit(): void {
    this.categoryService.listCategories().subscribe((res) => this.categories.set(res.data ?? []));
    this.supplierService.listSuppliers({}).subscribe((res) => this.suppliers.set(res.data ?? []));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.productId = Number(idParam);
      this.loading.set(true);
      this.productService.get({ id: this.productId }).subscribe({
        next: (res) => {
          const p = res.data;
          if (p) {
            this.form.patchValue({
              name: p.name,
              categoryId: p.categoryId ?? null,
              supplierId: p.supplierId ?? null,
              trackType: p.trackType as 'A' | 'B',
              cost: p.cost ?? null,
              suggestedPrice: p.suggestedPrice ?? null,
              moq: p.moq ?? null,
              shelfLifeDays: p.shelfLifeDays ?? null,
              logisticsCondition: p.logisticsCondition ?? '',
            });
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const request = {
      name: v.name,
      categoryId: v.categoryId!,
      supplierId: v.supplierId ?? undefined,
      trackType: v.trackType,
      cost: v.cost ?? undefined,
      suggestedPrice: v.suggestedPrice ?? undefined,
      moq: v.moq ?? undefined,
      shelfLifeDays: v.shelfLifeDays ?? undefined,
      logisticsCondition: v.logisticsCondition || undefined,
    };

    this.saving.set(true);
    const obs = this.productId
      ? this.productService.updateProduct({ id: this.productId, productRequest: request })
      : this.productService.createProduct({ productRequest: request });

    obs.subscribe({
      next: (res) => {
        this.saving.set(false);
        if (res.data?.duplicateNameWarning) {
          this.snackBar.open('已儲存，但同類別已有相同名稱的品項，請留意', '關閉', { duration: 4000 });
        }
        this.router.navigateByUrl(`/products/${res.data?.product?.id}`);
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(err?.error?.error?.message ?? '儲存失敗，請重新輸入', '關閉', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/products');
  }
}
