import {
  Component,
  TemplateRef,
  computed,
  contentChildren,
  input,
  linkedSignal,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslocoModule } from '@jsverse/transloco';
import { DataTableColumn } from './data-table.model';
import { DataTableCellDefDirective } from './data-table-cell-def.directive';
import { DataTableFilterDefDirective } from './data-table-filter-def.directive';

@Component({
  selector: 'app-data-table',
  imports: [
    CommonModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    TranslocoModule,
  ],
  templateUrl: './data-table.html',
  styleUrl: './data-table.css',
})
export class DataTableComponent<T> {
  readonly columns = input.required<readonly DataTableColumn[]>();
  readonly data = input<readonly T[]>([]);
  readonly tableTitle = input('');
  readonly noDataLabel = input('');
  readonly noDataDetail = input('');
  readonly showPaginator = input(false);
  readonly pageSizeOptions = input<number[]>([10, 25, 50]);

  readonly sortChange = output<Sort>();

  readonly displayedColumns = computed(() => this.columns().map((column) => column.key));

  readonly pageSize = linkedSignal(() => this.pageSizeOptions()[0] ?? 10);

  readonly pageIndex = linkedSignal<readonly T[], number>({
    source: () => this.data(),
    computation: (_data, previous) => {
      const lastPageIndex = Math.max(Math.ceil(this.data().length / this.pageSize()) - 1, 0);
      return Math.min(previous?.value ?? 0, lastPageIndex);
    },
  });

  readonly totalPages = computed(() =>
    Math.max(Math.ceil(this.data().length / this.pageSize()), 1),
  );

  readonly visiblePages = computed(() => {
    const total = this.totalPages();
    const current = this.pageIndex() + 1;

    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i + 1);
    }

    if (current <= 4) {
      return [1, 2, 3, 4, 5, '...', total];
    }

    if (current >= total - 3) {
      return [1, '...', total - 4, total - 3, total - 2, total - 1, total];
    }

    return [1, '...', current - 1, current, current + 1, '...', total];
  });

  readonly visibleRows = computed(() => {
    if (!this.showPaginator()) {
      return this.data();
    }
    const start = this.pageIndex() * this.pageSize();
    return this.data().slice(start, start + this.pageSize());
  });


  goToPage(page: number | string): void {
    if (page === '...' || typeof page === 'string') return;
    this.pageIndex.set(page - 1);
  }

  goToFirstPage(): void {
    this.pageIndex.set(0);
  }

  goToLastPage(): void {
    this.pageIndex.set(this.totalPages() - 1);
  }

  handlePageSizeChange(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.pageSize.set(Number(selectElement.value));
    this.pageIndex.set(0);
  }


  private readonly cellDefs = contentChildren(DataTableCellDefDirective, { descendants: true });
  private readonly filterDefs = contentChildren(DataTableFilterDefDirective, { descendants: true });

  cellTemplate(columnKey: string): TemplateRef<{ $implicit: T }> | undefined {
    return this.cellDefs().find((cellDef) => cellDef.columnKey() === columnKey)?.template as
      TemplateRef<{ $implicit: T }> | undefined;
  }

  filterTemplate(columnKey: string): TemplateRef<unknown> | undefined {
    return this.filterDefs().find((filterDef) => filterDef.columnKey() === columnKey)?.template;
  }

  getCellValue(row: T, columnKey: string): unknown {
    return (row as Record<string, unknown>)[columnKey];
  }
}
