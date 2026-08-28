import { Component, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TrendControllerService } from '../../core/api/api/trendController.service';
import { TrendSignalProjection } from '../../core/api/model/trendSignalProjection';
import { TrendChartProjection } from '../../core/api/model/trendChartProjection';
import { TrendDetailResponse } from '../../core/api/model/trendDetailResponse';

@Component({
  selector: 'app-trends',
  imports: [JsonPipe, MatTableModule, MatProgressSpinnerModule, MatIconModule, MatButtonModule],
  templateUrl: './trends.component.html',
  styleUrl: './trends.component.scss',
})
export class TrendsComponent {
  private readonly trendController = inject(TrendControllerService);

  readonly displayedColumns = ['keyword', 'heatToday', 'slope7d', 'slope30d', 'aiSignal'];
  readonly loading = signal(true);
  readonly trends = signal<TrendSignalProjection[]>([]);

  readonly expandedKeywordId = signal<number | null>(null);
  readonly chartLoading = signal(false);
  readonly chart = signal<TrendChartProjection[]>([]);
  readonly detail = signal<TrendDetailResponse | null>(null);

  constructor() {
    this.trendController.getTrends().subscribe({
      next: (data) => {
        this.trends.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggleRow(row: TrendSignalProjection): void {
    const keywordId = row.keywordId;
    if (keywordId == null) return;

    if (this.expandedKeywordId() === keywordId) {
      this.expandedKeywordId.set(null);
      return;
    }

    this.expandedKeywordId.set(keywordId);
    this.chart.set([]);
    this.detail.set(null);
    this.chartLoading.set(true);

    this.trendController.getTrendChart({ keywordId }).subscribe({
      next: (data) => this.chart.set(data),
      error: () => this.chart.set([]),
    });

    this.trendController.getTrendDetail({ keywordId }).subscribe({
      next: (data) => {
        this.detail.set(data);
        this.chartLoading.set(false);
      },
      error: () => this.chartLoading.set(false),
    });
  }

  /** 90 天熱度折線圖的 SVG polyline 座標字串（無外部圖表套件時的最小實作）。 */
  sparklinePoints(data: TrendChartProjection[]): string {
    if (data.length === 0) return '';
    const values = data.map((d) => d.heatScore ?? 0);
    const max = Math.max(...values, 1);
    const min = Math.min(...values, 0);
    const range = max - min || 1;
    const stepX = 100 / Math.max(data.length - 1, 1);

    return values
      .map((v, i) => `${(i * stepX).toFixed(2)},${(100 - ((v - min) / range) * 100).toFixed(2)}`)
      .join(' ');
  }
}
