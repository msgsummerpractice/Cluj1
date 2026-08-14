import { Directive, TemplateRef, inject, input } from '@angular/core';

@Directive({
  selector: '[appDataTableFilterDef]',
})
export class DataTableFilterDefDirective {
  readonly columnKey = input.required<string>({ alias: 'appDataTableFilterDef' });
  readonly template = inject<TemplateRef<unknown>>(TemplateRef);
}
