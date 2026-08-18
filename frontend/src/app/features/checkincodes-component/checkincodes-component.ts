import { Input, Component, inject, input, computed, signal, effect, untracked } from '@angular/core';
import { EventService } from '../../core/services/event.service';
import { ToastService } from '../../core/services/toast.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { jsPDF } from 'jspdf';




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

  downloadQrAndEventCode(): void{
     const qrCode = this.qrCodeContent();
     const eventCode = this.eventCode();

    if(!qrCode || !eventCode){
      this.toast.show('error', 'No codes to download');
      return;
    }

    const pdf = new jsPDF();
    const width = 100;
    const height = 100;
    const xPos = (pdf.internal.pageSize.getWidth() - width) / 2;

    if(eventCode) {
      pdf.setFontSize(18);
      pdf.text(`Event Code: ${eventCode}`, pdf.internal.pageSize.getWidth() / 2, 40, { align: 'center' });
    }
    pdf.addImage(qrCode, 'PNG', xPos, 40, width, height);
    pdf.save(`Event-${this.eventId()}-CheckIn.pdf`);

  }



}
