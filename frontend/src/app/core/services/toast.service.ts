import { Injectable, signal } from '@angular/core';



@Injectable({
  providedIn: 'root',
})

export class ToastService {
  message = signal<string>('');
  type = signal<'success' | 'error'>('success');
  isVisible = signal<boolean>(false);

  private timeoutId: any;

  show(type: 'success' | 'error', message: string) {
    this.type.set(type);
    this.message.set(message);
    this.isVisible.set(true);

    if(this.timeoutId){
      clearTimeout(this.timeoutId);
    }
    this.timeoutId = setTimeout(() => {
      this.isVisible.set(false);
    }, 3000);
  }

}
