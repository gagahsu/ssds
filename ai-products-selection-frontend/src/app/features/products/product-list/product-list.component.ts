import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { ProductControllerService } from '../../../core/api/api/productController.service';
import { CategoryControllerService } from '../../../core/api/api/categoryController.service';
import { SupplierControllerService } from '../../../core/api/api/supplierController.service';
import { ProductListItem } from '../../../core/api/model/productListItem';
import { CategoryOption } from '../../../core/api/model/categoryOption';
import { SupplierOption } from '../../../core/api/model/supplierOption';
import { GradeChipComponent } from '../../../shared/components/grade-chip/grade-chip.component';

/** FR-03-1 品項清單。B 軌品項不顯示成本/售價/毛利率/分數/分級（改用「—」）。 */
@Component({
  selector: 'app-product-list',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    GradeChipComponent,
  ],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductControllerService);
  private readonly categoryService = inject(CategoryControllerService);
  private readonly supplierService = inject(SupplierControllerService);

  readonly displayedColumns = [
    'name', 'categoryName', 'trackType', 'supplierName',
    'cost', 'suggestedPrice', 'marginRate', 'latestScore', 'latestGrade', 'status', 'actions',
  ];

  readonly rows = signal<ProductListItem[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly categories = signal<CategoryOption[]>([]);
  readonly suppliers = signal<SupplierOption[]>([]);

  pageIndex = 0;
  pageSize = 20;

  readonly filterForm = this.fb.nonNullable.group({
    keyword: [''],
    categoryId: [null as number | null],
    supplierId: [null as number | null],
    trackType: [null as 'A' | 'B' | null],
    status: [null as ProductListItem.StatusEnum | null],
  });

  readonly StatusEnum = ProductListItem.StatusEnum;

  ngOnInit(): void {
    this.categoryService.listCategories().subscribe((res) => this.categories.set(res.data ?? []));
    this.supplierService.listSuppliers({}).subscribe((res) => this.suppliers.set(res.data ?? []));

    this.filterForm.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => {
      this.pageIndex = 0;
      this.load();
    });

    this.load();
  }

  load(): void {
    const f = this.filterForm.getRawValue();
    this.loading.set(true);
    this.productService
      .listProducts({
        pageable: { page: this.pageIndex, size: this.pageSize },
        keyword: f.keyword || undefined,
        categoryId: f.categoryId ?? undefined,
        supplierId: f.supplierId ?? undefined,
        trackType: f.trackType ?? undefined,
        status: f.status ?? undefined,
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

  createProduct(): void {
    this.router.navigateByUrl('/products/new');
  }

  openDetail(row: ProductListItem): void {
    this.router.navigateByUrl(`/products/${row.id}`);
  }
}
