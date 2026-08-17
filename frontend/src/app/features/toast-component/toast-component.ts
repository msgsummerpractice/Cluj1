import { Component } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast-component',
  imports: [],
  templateUrl: './toast-component.html',
  styleUrl: './toast-component.css',
})
export class ToastComponent {
  constructor(public toastService: ToastService) {}
}
