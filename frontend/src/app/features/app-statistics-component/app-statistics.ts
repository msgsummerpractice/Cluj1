import {
  ChangeDetectorRef,
  Component,
  OnInit,
  OnDestroy,
  inject,
  input,
  Injector,
  DestroyRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { toObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap, Subscription } from 'rxjs';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { MatCardModule } from '@angular/material/card';
import { EventStatistics } from '../../core/models/event-statistics.model';
import { EventService } from '../../core/services/event.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { MatTableModule } from '@angular/material/table';

@Component({
  selector: 'app-statistics',
  imports: [CommonModule, MatCardModule, BaseChartDirective, TranslocoModule, MatTableModule],
  templateUrl: './app-statistics.html',
})
export class StatisticsViewComponent implements OnInit, OnDestroy {
  readonly id = input.required<string>();

  private readonly eventService = inject(EventService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly translocoService = inject(TranslocoService);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);

  stats: EventStatistics | null = null;
  displayedColumns: string[] = ['name', 'email', 'status', 'checkInTime'];

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

  private langSubscription?: Subscription;

  ngOnInit(): void {
    toObservable(this.id, { injector: this.injector })
      .pipe(
        switchMap((id) => this.eventService.getEventStatistics(id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((data) => {
        this.stats = data;
        this.updateChart(data.registrationTimeDistribution);
        this.cdr.detectChanges();
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
