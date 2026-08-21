import {
  AfterViewInit,
  Directive,
  ElementRef,
  Input,
  NgZone,
  OnDestroy,
  Optional,
} from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';

/**
 * Automatically shows a tooltip with the full text on any element whose text
 * is truncated with CSS ellipsis (either `text-overflow: ellipsis` or
 * `-webkit-line-clamp`). The tooltip disappears when the text fits.
 *
 * Usage:
 *   <span appEllipsisTooltip>{{ someLongText }}</span>
 *   <td mat-cell appEllipsisTooltip>{{ row.name }}</td>
 *
 * Works with either Angular Material `MatTooltip` (when the module is
 * imported/available on the host component) or falls back to the native
 * `title` attribute.
 */
@Directive({
  selector: '[appEllipsisTooltip]',
  standalone: true,
  hostDirectives: [
    {
      directive: MatTooltip,
      inputs: ['matTooltipPosition', 'matTooltipClass', 'matTooltipShowDelay'],
    },
  ],
})
export class EllipsisTooltipDirective implements AfterViewInit, OnDestroy {
  /** Optional explicit text override; defaults to the element's textContent. */
  @Input('appEllipsisTooltip') explicitText: string | null | undefined;

  private resizeObserver?: ResizeObserver;
  private mutationObserver?: MutationObserver;

  constructor(
    private readonly el: ElementRef<HTMLElement>,
    private readonly zone: NgZone,
    @Optional() private readonly tooltip?: MatTooltip,
  ) {}

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      this.resizeObserver = new ResizeObserver(() => this.update());
      this.resizeObserver.observe(this.el.nativeElement);

      this.mutationObserver = new MutationObserver(() => this.update());
      this.mutationObserver.observe(this.el.nativeElement, {
        childList: true,
        characterData: true,
        subtree: true,
      });

      // Initial check after layout has stabilized.
      requestAnimationFrame(() => this.update());
    });
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.mutationObserver?.disconnect();
  }

  private update(): void {
    const host = this.el.nativeElement;
    const overflowing =
      host.scrollWidth - host.clientWidth > 1 ||
      host.scrollHeight - host.clientHeight > 1;

    const text = (this.explicitText ?? host.textContent ?? '').trim();

    this.zone.run(() => {
      if (overflowing && text.length > 0) {
        if (this.tooltip) {
          this.tooltip.message = text;
          this.tooltip.disabled = false;
        } else {
          host.setAttribute('title', text);
        }
      } else {
        if (this.tooltip) {
          this.tooltip.message = '';
          this.tooltip.disabled = true;
        } else {
          host.removeAttribute('title');
        }
      }
    });
  }
}

