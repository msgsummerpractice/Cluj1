import { Component, TemplateRef, computed, contentChildren, input, output } from '@angular/core';
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
  /** Extra text rendered in quotes after the empty state label, e.g. the active search term. */
  readonly noDataDetail = input('');

  readonly sortChange = output<Sort>();

  readonly displayedColumns = computed(() => this.columns().map((column) => column.key));

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
