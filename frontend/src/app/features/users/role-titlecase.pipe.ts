import { Pipe, PipeTransform } from '@angular/core';
import { TitleCasePipe } from '@angular/common';

@Pipe({
  name: 'roleTitlecase',
  standalone: true,
})
export class RoleTitlecasePipe implements PipeTransform {
  private readonly titleCasePipe = new TitleCasePipe();

  transform(role: string): string {
    const formattedRole = this.titleCasePipe.transform(role.replace(/_/g, ' '));
    return formattedRole.replace(/\bHr\b/g, 'HR');
  }
}
