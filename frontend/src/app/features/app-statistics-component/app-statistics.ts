import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { MatCardModule } from '@angular/material/card';
import { EventStatistics } from '../../core/models/event-statistics.model';
import { EventService } from '../../core/services/event.service';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule, MatCardModule, BaseChartDirective, TranslocoModule],
  templateUrl: './app-statistics.html',
})
export class StatisticsViewComponent implements OnInit {
  private eventService = inject(EventService);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  stats: EventStatistics | null = null;

  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Registrations per Day',
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

  ngOnInit() {
    const eventId = this.route.snapshot.paramMap.get('id');
    if (eventId) {
      this.eventService.getEventStatistics(eventId).subscribe((data) => {
        this.stats = data;
        this.updateChart(data.registrationTimeDistribution);
        this.cdr.detectChanges();
      });
    }
  }

  updateChart(distribution: { [key: string]: number }) {
    this.barChartData = {
      labels: Object.keys(distribution),
      datasets: [
        {
          data: Object.values(distribution),
          label: 'Registrations per Day',
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
