import { Directive, TemplateRef, inject, input } from '@angular/core';

@Directive({
  selector: '[appDataTableCellDef]',
})
export class DataTableCellDefDirective<T = unknown> {
  readonly columnKey = input.required<string>({ alias: 'appDataTableCellDef' });
  readonly template = inject<TemplateRef<{ $implicit: T }>>(TemplateRef);
}
