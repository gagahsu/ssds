import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { WeightVersionControllerService } from '../../core/api/api/weightVersionController.service';
import { WeightVersionSummary } from '../../core/api/model/weightVersionSummary';
import { WeightVersionDetail } from '../../core/api/model/weightVersionDetail';
import { SceneWeightSet } from '../../core/api/model/sceneWeightSet';
import { AuthService } from '../../core/auth/auth.service';

const BONUS_FACTORS = ['TREND', 'MARGIN', 'CVR', 'PRICE_FIT', 'FESTIVAL', 'CLIMATE'] as const;

/** FR-08 情境權重組設定。核准生效＋觸發全量重算（POST /{id}/approve）待評分批次排程接上後再做。 */
@Component({
  selector: 'app-weights',
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './weights.component.html',
  styleUrl: './weights.component.scss',
})
export class WeightsComponent implements OnInit {
  private readonly weightService = inject(WeightVersionControllerService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly factors = BONUS_FACTORS;
  readonly versions = signal<WeightVersionSummary[]>([]);
  readonly selected = signal<WeightVersionDetail | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly editing = signal(false);
  readonly displayedColumns = ['versionNo', 'name', 'status', 'current', 'effectiveFrom', 'actions'];

  get canManage(): boolean {
    return this.authService.hasRole('BUYER_LEAD');
  }

  ngOnInit(): void {
    this.loadVersions();
  }

  loadVersions(): void {
    this.loading.set(true);
    this.weightService.listWeightVersions().subscribe({
      next: (res) => {
        this.versions.set(res.data ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  view(version: WeightVersionSummary): void {
    this.editing.set(false);
    this.weightService.profiles({ id: version.id! }).subscribe((res) => this.selected.set(res.data ?? null));
  }

  startNewDraft(): void {
    const active = this.selected();
    const base: SceneWeightSet[] = active?.scenes
      ? active.scenes
      : (['VIRAL', 'FESTIVAL', 'REPLENISHMENT', 'SEASONAL'] as const).map((sceneType) => ({
          sceneType,
          weights: Object.fromEntries(this.factors.map((f) => [f, 0])),
          gradeAMin: 85,
          gradeBMin: 70,
        }));
    this.selected.set({
      id: undefined,
      versionNo: undefined,
      name: '新草稿版本',
      status: 'DRAFT',
      effectiveFrom: undefined,
      current: false,
      changeNote: '',
      scenes: base.map((s) => ({ ...s, weights: { ...s.weights } })),
    } as WeightVersionDetail);
    this.editing.set(true);
  }

  edit(): void {
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    const current = this.selected();
    if (current?.id) {
      this.view({ id: current.id } as WeightVersionSummary);
    } else {
      this.selected.set(null);
    }
  }

  weightSum(scene: SceneWeightSet): number {
    return this.factors.reduce((sum, f) => sum + (Number(scene.weights[f]) || 0), 0);
  }

  save(): void {
    const detail = this.selected();
    if (!detail) {
      return;
    }
    const request = {
      name: detail.name!,
      changeNote: detail.changeNote,
      effectiveFrom: detail.effectiveFrom,
      scenes: detail.scenes!,
    };
    this.saving.set(true);
    const obs = detail.id
      ? this.weightService.updateWeightVersion({ id: detail.id, weightVersionUpsertRequest: request })
      : this.weightService.createWeightVersion({ weightVersionUpsertRequest: request });

    obs.subscribe({
      next: (res) => {
        this.saving.set(false);
        this.editing.set(false);
        this.selected.set(res.data ?? null);
        this.loadVersions();
        this.snackBar.open('已儲存草稿', '關閉', { duration: 3000 });
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(err?.error?.error?.message ?? '儲存失敗', '關閉', { duration: 4000 });
      },
    });
  }
}
