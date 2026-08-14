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
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
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
    MatPaginatorModule,
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
  readonly pageSizeOptions = input<number[]>([5, 10, 25, 100]);

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
  readonly visibleRows = computed(() => {
    if (!this.showPaginator()) {
      return this.data();
    }

    const start = this.pageIndex() * this.pageSize();
    return this.data().slice(start, start + this.pageSize());
  });

  handlePageChange(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
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
