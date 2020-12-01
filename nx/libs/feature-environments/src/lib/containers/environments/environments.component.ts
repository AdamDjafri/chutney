import { Component, OnInit } from '@angular/core';
import { Environment, EnvironmentsGQL } from '@chutney/data-access';
import { pluck } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { chutneyAnimations } from '@chutney/utils';

@UntilDestroy()
@Component({
  selector: 'chutney-environments',
  templateUrl: './environments.component.html',
  styleUrls: ['./environments.component.scss'],
  animations: [chutneyAnimations],
})
export class EnvironmentsComponent implements OnInit {

  environments$: Observable<Environment[]>;

  breadcrumbs: any = [
    {title: 'Home', link: ['/']},
    {title: 'Environments', link: ['/']},
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private environmentsGQL: EnvironmentsGQL) {
  }

  ngOnInit(): void {
    this.environments$ = this.environmentsGQL
      .watch()
      .valueChanges.pipe(pluck('data', 'environments'), untilDestroyed(this));
  }


  createEnvironment() {
    this.router.navigate(['edit'], {relativeTo: this.route});
  }

  updateEnvironment(environment: Environment) {
    this.router.navigate([environment.name, 'edit'], {relativeTo: this.route});
  }

  viewEnvironment(environment: Environment) {
    this.router.navigate([environment.name], { relativeTo: this.route });
  }

  deleteEnvironment(environment: Environment) {

  }
}
