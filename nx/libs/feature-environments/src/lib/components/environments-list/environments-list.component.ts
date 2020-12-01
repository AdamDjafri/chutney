import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { Environment } from '@chutney/data-access';

@Component({
  selector: 'chutney-environments-list',
  templateUrl: './environments-list.component.html',
  styleUrls: ['./environments-list.component.scss']
})
export class EnvironmentsListComponent {

  @Input()
  set environments(environments: Environment[]) {
    this.dataSource.data = environments;
  }

  @Output()
  view = new EventEmitter<Environment>();
  @Output()
  edit = new EventEmitter<Environment>();
  @Output()
  delete = new EventEmitter<Environment>();

  dataSource: MatTableDataSource<Environment> = new MatTableDataSource<Environment>();

  constructor() { }

  viewEnvironment(enviroment: Environment) {
    this.view.emit(enviroment);
  }

  editEnvironment(enviroment: Environment) {
    this.edit.emit(enviroment);
  }

  deleteEnvironment(enviroment: Environment) {
    this.delete.emit(enviroment);
  }
}
