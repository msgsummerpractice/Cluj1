import { ChangeDetectorRef, Component, OnDestroy, effect, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { MatCardModule } from '@angular/material/card';
import { EventStatistics } from '../../core/models/event-statistics.model';
import { EventService } from '../../core/services/event.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { MatTableModule } from '@angular/material/table';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-statistics',
  imports: [CommonModule, MatCardModule, BaseChartDirective, TranslocoModule, MatTableModule],
  templateUrl: './app-statistics.html',
})
export class StatisticsViewComponent implements OnDestroy {
  readonly id = input.required<string>();
  private eventService = inject(EventService);
  private cdr = inject(ChangeDetectorRef);
  stats: EventStatistics | null = null;
  displayedColumns: string[] = ['name', 'email', 'status', 'checkInTime'];
  private readonly translocoService = inject(TranslocoService);
  private langSubscription?: Subscription;

  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: this.translocoService.translate('statistics.registrationsPerDay'),
        backgroundColor: '#8b5cf6',
        borderRadius: 6,
        barThickness: 32,
      },
    ],
  };

  public barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
      },
      y: {
        grid: {
          color: '#f3f4f6',
        },
        beginAtZero: true,
      },
    },
  };

  constructor() {
    effect(() => {
      this.eventService.getEventStatistics(this.id()).subscribe((data) => {
        this.stats = data;
        this.updateChart(data.registrationTimeDistribution);
        this.cdr.detectChanges();
      });
    });

    this.langSubscription = this.translocoService.langChanges$.subscribe(() => {
      if (this.stats) {
        this.updateChart(this.stats.registrationTimeDistribution);
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy() {
    this.langSubscription?.unsubscribe();
  }

  updateChart(distribution: { [key: string]: number }) {
    this.barChartData = {
      labels: Object.keys(distribution),
      datasets: [
        {
          data: Object.values(distribution),
          label: this.translocoService.translate('statistics.registrationsPerDay'),
          backgroundColor: '#8b5cf6',
          borderRadius: 6,
          barThickness: 32,
        },
      ],
    };
  }

  objectKeys(obj: any) {
    return obj ? Object.keys(obj) : [];
  }
}
