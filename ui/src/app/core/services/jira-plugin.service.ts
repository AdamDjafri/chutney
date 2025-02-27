import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { environment } from '@env/environment';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export class JiraDto {
    constructor(
        public id: string,
        public chutneyId: string) {
    }
}

@Injectable({
    providedIn: 'root'
})
export class JiraPluginService {

    private url = '/api/ui/jira/v1/';
    private scenarioUrl = this.url + 'scenario';
    private campaignUrl = this.url + 'campaign';
    private testExecUrl = this.url + 'testexec';

    constructor(private http: HttpClient) {
    }

    public findScenarios(): Observable<Map<string, string>> {
        return this.http.get<any>(environment.backend + this.scenarioUrl )
        .pipe(map((res: Object) => new Map(Object.entries(res))));
    }

    public findCampaigns(): Observable<Map<string, string>> {
        return this.http.get<any>(environment.backend + this.campaignUrl )
        .pipe(map((res: Object) => new Map(Object.entries(res))));
    }

    public findTestExecScenarios(testExecId: string): Observable<string[]> {
        return this.http.get<any>(environment.backend + this.testExecUrl + '/' + testExecId)
        .pipe(map((res: string[]) => res));
    }

    public findByScenarioId(scenarioId: string): Observable<string> {
        return this.http.get<JiraDto>(environment.backend + this.scenarioUrl + '/' + scenarioId)
            .pipe(map((jiraDto: JiraDto) => {
                return jiraDto.id;
            }));
    }

    public saveForScenario(scenarioId: string, jiraId: string): Observable<JiraDto> {
        return this.http.post<JiraDto>(environment.backend + this.scenarioUrl, new JiraDto(jiraId, scenarioId));
    }

    public removeForScenario(scenarioId: string) {
        return this.http.delete<HttpResponse<any>>(environment.backend + this.scenarioUrl  + '/' + scenarioId);
    }

    public findByCampaignId(campaignId: number): Observable<string> {
        return this.http.get<JiraDto>(environment.backend + this.campaignUrl + '/' + campaignId)
            .pipe(map((jiraDto: JiraDto) => {
                return jiraDto.id;
            }));
    }

    public saveForCampaign(campaignId: number, jiraId: string): Observable<JiraDto> {
        return this.http.post<JiraDto>(environment.backend + this.campaignUrl, new JiraDto(jiraId, campaignId.toString()));
    }

    public removeForCampaign(campaignId: number) {
        return this.http.delete<HttpResponse<any>>(environment.backend + this.campaignUrl + '/' + campaignId);
    }
}
