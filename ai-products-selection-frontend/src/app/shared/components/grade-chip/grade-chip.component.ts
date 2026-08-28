import { Component, input } from '@angular/core';

/** 分級徽章（規格書 §5.6：A 主推／B 備選／C 觀察）。品項清單、排行、詳情共用。 */
@Component({
  selector: 'app-grade-chip',
  imports: [],
  templateUrl: './grade-chip.component.html',
  styleUrl: './grade-chip.component.scss'
})
export class GradeChipComponent {
  readonly grade = input<'A' | 'B' | 'C' | null | undefined>(null);
}
