import { Input, Component, inject, input, computed, signal, effect, untracked } from '@angular/core';
import { EventService } from '../../core/services/event.service';
import { ToastService } from '../../core/services/toast.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { jsPDF } from 'jspdf';
import { TranslocoPipe } from '@jsverse/transloco';
import { MatProgressSpinner } from '@angular/material/progress-spinner';




@Component({
  selector: 'app-checkincodes-component',
  imports: [TranslocoPipe, MatProgressSpinner],
  templateUrl: './checkincodes-component.html',
  styleUrl: './checkincodes-component.css',
})
export class CheckincodesComponent {
  private eventService = inject(EventService);
  private toast = inject(ToastService);


  eventId = input.required<string>();
  eventStatus = input.required<string>();

  isGenerating = signal(false);

  initialQrCodeContent = input<string | null>(null);
  initialEventCode = input<string | null>(null);
  qrCodeContent = signal<string | null>(null);
  eventCode = signal<string | null>(null);

  protected readonly isPublished = computed(() => this.eventStatus() === 'PUBLISHED');
  protected readonly hasCodes = computed(() => !!this.qrCodeContent() && !!this.eventCode());

  protected readonly PDF_WIDTH = 100;
  protected readonly PDF_HEIGHT = 100;

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

  downloadQrAndEventCode(): void {
    this.isGenerating.set(true);
    const qrCode = this.qrCodeContent();
    const eventCode = this.eventCode();

    if (!qrCode || !eventCode) {
      this.toast.show('error', 'Error downloading codes. Please generate the codes first.');
      return;
    }

    const pdf = new jsPDF();
    const xPos = (pdf.internal.pageSize.getWidth() - this.PDF_WIDTH) / 2;



    setTimeout(() => {
      try {
        pdf.setFontSize(18);
        pdf.text(`Event Code: ${eventCode}`, pdf.internal.pageSize.getWidth() / 2, 40, {
        align: 'center',
          });
        pdf.addImage(qrCode, 'PNG', xPos, 40, this.PDF_WIDTH, this.PDF_HEIGHT);
        pdf.save(`Event-${this.eventId()}-CheckIn.pdf`);
        this.toast.show('success', 'PDF downloaded successfully');
      } catch (error) {
        this.toast.show('error', 'Error generating PDF');
      } finally {
        this.isGenerating.set(false);
      }
    }, 500);
  }
}
