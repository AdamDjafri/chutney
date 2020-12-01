import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EnvironmentsComponent } from './containers/environments/environments.component';
import { TargetsListComponent } from './components/targets-list/targets-list.component';
import { UiCommonsModule } from '@chutney/ui-commons';
import { RouterModule } from '@angular/router';
import { UiMaterialModule } from '@chutney/ui-material';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { EnvironmentsEditComponent } from './containers/environments-edit/environments-edit.component';
import { TargetEditComponent } from './containers/target-edit/target-edit.component';
import { EnvironmentsListComponent } from './components/environments-list/environments-list.component';
import { UtilsModule } from '@chutney/utils';
import { EnvironmentComponent } from './containers/environment/environment.component';

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild([
      {path: '', component: EnvironmentsComponent},
      {path: ':envname', component: EnvironmentComponent},
      {path: ':envname/edit', component: EnvironmentsEditComponent},
      {path: 'edit', component: EnvironmentsEditComponent},
      {path: ':envname/target/:targetName', component: TargetEditComponent},
      {path: ':envname/target/', component: TargetEditComponent},
    ]),
    UiCommonsModule,
    UiMaterialModule,
    UtilsModule,
    ReactiveFormsModule,
    FormsModule
  ],
  declarations: [EnvironmentsComponent, TargetsListComponent, EnvironmentsEditComponent, TargetEditComponent, EnvironmentsListComponent, EnvironmentComponent],
  exports: [EnvironmentsComponent, EnvironmentsEditComponent, TargetEditComponent],
})
export class FeatureEnvironmentsModule {
}
