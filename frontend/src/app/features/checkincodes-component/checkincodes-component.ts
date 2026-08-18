import { Input, Component, inject, input, computed, signal, effect, untracked } from '@angular/core';
import { EventService } from '../../core/services/event.service';
import { ToastService } from '../../core/services/toast.service';
import { toSignal } from '@angular/core/rxjs-interop';


@Component({
  selector: 'app-checkincodes-component',
  imports: [],
  templateUrl: './checkincodes-component.html',
  styleUrl: './checkincodes-component.css',
})
export class CheckincodesComponent {
  private eventService = inject(EventService);
  private toast = inject(ToastService);

  eventId = input.required<string>();
  eventStatus = input.required<string>();

  initialQrCodeContent = input<string | null>(null);
  initialEventCode = input<string | null>(null);
  qrCodeContent = signal<string | null>(null);
  eventCode = signal<string | null>(null);

  protected readonly isPublished = computed(() => this.eventStatus() === 'PUBLISHED');
  protected readonly hasCodes = computed(() => !!this.qrCodeContent() && !!this.eventCode());

  ngOnInit() {
    this.qrCodeContent.set(this.initialQrCodeContent() || null);
    this.eventCode.set(this.initialEventCode() || null);
  }

  generateCodes(): void {
    this.eventService.generateCheckInCodes(this.eventId()).subscribe({
      next: (res) => {
        this.qrCodeContent.set(res?.qrCodeContent ?? null);
        this.eventCode.set(res?.eventCode ?? null);
      },
      error: (err) => {
        this.toast.show('error', 'Failed to generate codes');
      },
    });
  }
}
