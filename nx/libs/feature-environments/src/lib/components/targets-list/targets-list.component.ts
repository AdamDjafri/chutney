import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Environment, Target } from '@chutney/data-access';
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'chutney-targets-list',
  templateUrl: './targets-list.component.html',
  styleUrls: ['./targets-list.component.scss']
})
export class TargetsListComponent {

  dataSource: MatTableDataSource<Target> = new MatTableDataSource<Target>();

  @Input()
  set environment(environment: Environment) {
    if (environment !== null && environment !== undefined) {
      this.dataSource.data = environment.targets;
    }
  }

  @Output()
  view = new EventEmitter<Environment>();
  @Output()
  edit = new EventEmitter<Environment>();
  @Output()
  delete = new EventEmitter<Environment>();

}
