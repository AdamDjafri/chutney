import { Component, OnInit } from '@angular/core';
import { pluck } from 'rxjs/operators';
import { Campaign, Environment, EnvironmentGQL, EnvironmentsGQL } from '@chutney/data-access';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { chutneyAnimations } from '@chutney/utils';

@UntilDestroy()
@Component({
  selector: 'chutney-environment',
  templateUrl: './environment.component.html',
  styleUrls: ['./environment.component.scss'],
  animations: [chutneyAnimations],
})
export class EnvironmentComponent implements OnInit {

  environment$: Observable<Environment>;
  environment: Environment;

  breadcrumbs: any = [
    {title: 'Home', link: ['/']},
    {title: 'Environments', link: ['/']},
    {title: 'Environment', link: ['/']},
  ];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private environmentGQL: EnvironmentGQL
  ) { }

  ngOnInit(): void {
    const environmentName = this.route.snapshot.paramMap.get('envname');
    this.environment$ = this.environmentGQL
      .watch({ envName: environmentName })
      .valueChanges.pipe(pluck('data', 'environment'), untilDestroyed(this));
    this.environment$.subscribe(result => {
      this.environment = result
    });
  }
}

