import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { map, pluck, switchMap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { EnvironmentGQL, EnvironmentInput, UpdateEnvironmentGQL } from '@chutney/data-access';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'chutney-environments-edit',
  templateUrl: './environments-edit.component.html',
  styleUrls: ['./environments-edit.component.scss']
})
export class EnvironmentsEditComponent implements OnInit {

  environmentForm: FormGroup;
  breadcrumbs: any = [
    { title: 'Home', link: ['/'] },
    { title: 'Environments', link: ['/'] },
    { title: 'Edit', link: ['/'] },
  ];
  originalName: string;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private environmentGQL: EnvironmentGQL,
    private updateEnvironmentGQL: UpdateEnvironmentGQL,
    private snackBar: MatSnackBar,
  ) {
  }

  ngOnInit(): void {

    this.environmentForm = this.fb.group({
        name: [''],
        description: ['']
      }
    );

    this.route.params
      .pipe(
        switchMap((p) => {
          return this.environmentGQL
            .fetch({envName: p.envname})
            .pipe(pluck('data', 'environment'))
        })
      )
      .subscribe(result =>  {
        if(result) {
          this.originalName = result.name;
          this.environmentForm.controls['name'].setValue(result.name);
          this.environmentForm.controls['description'].setValue(result.description);
        }
      });
  }

  updateEnvironment() {
    const env :EnvironmentInput = {
      name: this.environmentForm.value.name,
      description: this.environmentForm.value.description
    };

    this.updateEnvironmentGQL.mutate({ input: env, originalName: this.originalName }).subscribe(
      (e) => {
        const matSnackBarRef = this.snackBar.open('Environment saved!', 'View');
        matSnackBarRef.onAction().subscribe(() => {
          this.router.navigate([`../`], { relativeTo: this.route });
        });
      },
      (err) => {
        console.log(err);
        this.snackBar.open('Erreur: ' + err.message)
      }
    );
  }
}
