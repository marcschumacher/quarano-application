import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, share } from 'rxjs/operators';
import {environment} from '../../environments/environment';
import {SymptomDto} from '../models/symptom';
import {ContactPersonDto, ContactPersonModifyDto} from '../models/contact-person';
import {DiaryDto, DiaryEntryDto, DiaryEntryModifyDto} from '../models/diary-entry';
import {Register} from '../models/register';
import {UserDto} from '../models/user';
import {CaseDetailDto} from '../models/case-detail';
import {CaseActionDto} from '../models/case-action';
import {DeleteLink, Link} from '../models/general';
import {ClientType, ReportCaseDto} from '../models/report-case';
import {HalResponse} from '../models/hal-response';
import {ActionListItemDto} from '../models/action';
import {AccountDto, AccountListDto} from '../models/account';


@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.api.baseUrl;

  constructor(protected httpClient: HttpClient) {
  }

  getSymptoms(): Observable<SymptomDto[]> {
    return this.httpClient.get<SymptomDto[]>(`${this.baseUrl}/api/symptoms`).pipe(share());
  }

  getContactPersons(): Observable<ContactPersonDto[]> {
    return this.httpClient.get<ContactPersonDto[]>(`${this.baseUrl}/api/contacts`).pipe(share());
  }

  getContactPerson(id: string): Observable<ContactPersonDto> {
    return this.httpClient.get<ContactPersonDto>(`${this.baseUrl}/api/contacts/${id}`).pipe(share());
  }

  getDiaryEntry(id: string): Observable<DiaryEntryDto> {
    return this.httpClient.get<DiaryEntryDto>(`${this.baseUrl}/api/diary/${id}`)
      .pipe(
        share(),
        map(entry => {
          entry.characteristicSymptoms = entry.symptoms.filter(s => s.characteristic);
          entry.nonCharacteristicSymptoms = entry.symptoms.filter(s => !s.characteristic);
          entry.date = this.getDate(entry.date);
          return entry;
        }),
      );
  }

  private getDate(date: Date): Date {
    return new Date(date + 'Z');
  }

  createDiaryEntry(diaryEntry: DiaryEntryModifyDto): Observable<DiaryEntryDto> {
    return this.httpClient.post<DiaryEntryDto>(`${this.baseUrl}/api/diary`, diaryEntry)
      .pipe(map(entry => {
        entry.date = this.getDate(entry.date);
        return entry;
      }));
  }

  modifyDiaryEntry(diaryEntry: DiaryEntryModifyDto) {
    return this.httpClient.put(`${this.baseUrl}/api/diary/${diaryEntry.id}`, diaryEntry);
  }

  registerClient(registerClient: Register): Observable<string> {
    return this.httpClient.post(`${this.baseUrl}/api/registration`, registerClient, { responseType: 'text' });
  }

  createContactPerson(contactPerson: ContactPersonModifyDto): Observable<ContactPersonDto> {
    return this.httpClient.post<ContactPersonDto>(`${this.baseUrl}/api/contacts`, contactPerson);
  }

  modifyContactPerson(contactPerson: ContactPersonModifyDto, id: string) {
    return this.httpClient.put(`${this.baseUrl}/api/contacts/${id}`, contactPerson);
  }

  login(username: string, password: string): Observable<{ token: string }> {
    return this.httpClient.post<{ token: string }>(`${this.baseUrl}/login`, { username, password });
  }

  getMe(): Observable<UserDto> {
    return this.httpClient.get<UserDto>(`${this.baseUrl}/api/user/me`);
  }

  checkClientCode(code: string): Observable<any> {
    return this.httpClient.get(`${this.baseUrl}/api/registration/checkcode/${code}`);
  }

  checkUsername(username: string): Observable<any> {
    return this.httpClient.get(`${this.baseUrl}/api/registration/checkusername/${username}`);
  }

  getCase(caseId: string): Observable<CaseDetailDto> {
    return this.httpClient.get<CaseDetailDto>(`${this.baseUrl}/api/hd/cases/${caseId}`);
  }

  getCaseActions(caseId: string): Observable<CaseActionDto> {
    return this.httpClient.get<CaseActionDto>(`${this.baseUrl}/api/hd/actions/${caseId}`)
      .pipe(share());
  }

  resolveAnomalies(link: Link, comment: string) {
    return this.httpClient.put(link.href, { comment });
  }

  createCase(caseDetail: CaseDetailDto, type: ClientType): Observable<any> {
    return this.httpClient.post<any>(`${this.baseUrl}/api/hd/cases?type=${type}`, caseDetail);
  }

  updateCase(caseDetail: CaseDetailDto): Observable<any> {
    return this.httpClient.put<any>(`${this.baseUrl}/api/hd/cases/${caseDetail.caseId}`, caseDetail);
  }

  addComment(caseId: string, comment: string): Observable<any> {
    return this.httpClient.post(`${this.baseUrl}/api/hd/cases/${caseId}/comments`, { comment });
  }

  getApiCall<T>(halResponse: HalResponse, key): Observable<T> {
    if (halResponse._links?.hasOwnProperty(key)) {
      let url = halResponse._links[key].href;
      if (Array.isArray(halResponse._links[key])) {
        url = halResponse._links[key][0].href;
      }
      return this.httpClient.get<T>(url);
    }
    return of();
  }

  putApiCall<T>(halResponse: HalResponse, key: string, body: any = {}): Observable<T> {
    if (halResponse._links?.hasOwnProperty(key)) {
      let url = halResponse._links[key].href;
      if (Array.isArray(halResponse._links[key])) {
        url = halResponse._links[key][0].href;
      }
      return this.httpClient.put<T>(url, body);
    }
    return of();
  }

  deleteApiCall<T>(halResponse: HalResponse, key: string): Observable<T> {
    if (halResponse._links?.hasOwnProperty(key)) {
      let url = halResponse._links[key].href;
      if (Array.isArray(halResponse._links[key])) {
        url = halResponse._links[key][0].href;
      }
      return this.httpClient.delete<T>(url);
    }
    return of();
  }

  getCases(): Observable<Array<ReportCaseDto>> {
    return this.httpClient.get<any>(`${this.baseUrl}/api/hd/cases`)
      .pipe(share(), map(result => result._embedded?.cases?.map(item => this.mapReportCase(item)) || []));
  }

  getDiary(): Observable<DiaryDto> {
    return this.httpClient.get<DiaryDto>(`${this.baseUrl}/api/diary`);
  }

  getAllActions(): Observable<ActionListItemDto[]> {
    return this.httpClient.get<any[]>(`${this.baseUrl}/api/hd/actions`)
      .pipe(share(), map(result => result.map(item => this.mapActionListItem(item))));
  }

  getHealthDepartmentUsers(): Observable<AccountListDto> {
    return this.httpClient.get<any>(`${this.baseUrl}/api/hd/accounts`)
      .pipe(share(), map(result => result._embedded));
  }

  getHealthDepartmentUser(id: string): Observable<AccountDto> {
    return this.httpClient.get<AccountDto>(`${this.baseUrl}/api/hd/accounts/${id}`)
      .pipe(share());
  }

  delete(deleteLink: DeleteLink) {
    return this.httpClient.delete(deleteLink.delete.href);
  }

  createHealthDepartmentUser(account: AccountDto): Observable<AccountDto> {
    return this.httpClient.post<AccountDto>(`${this.baseUrl}/api/hd/accounts`, account);
  }

  editHealthDepartmentUser(account: AccountDto) {
    return this.httpClient.put<AccountDto>(`${this.baseUrl}/api/hd/accounts/${account.accountId}`, account);
  }

  private mapReportCase(item: any): ReportCaseDto {
    return {
      dateOfBirth: item.dateOfBirth ? new Date(item.dateOfBirth) : null,
      status: item.status,
      email: item.email,
      phone: item.primaryPhoneNumber,
      firstName: item.firstName,
      lastName: item.lastName,
      medicalStaff: item.medicalStaff,
      enrollmentCompleted: item.enrollmentCompleted,
      quarantineEnd: item.quarantine?.to ? new Date(item.quarantine.to) : null,
      quarantineStart: item.quarantine?.from ? new Date(item.quarantine.from) : null,
      caseType: item.caseType,
      zipCode: item.zipCode,
      caseId: item.caseId,
      caseTypeLabel: item.caseTypeLabel
    };
  }

  private mapActionListItem(item: any): ActionListItemDto {
    return {
      dateOfBirth: item.dateOfBirth ? new Date(item.dateOfBirth) : null,
      caseId: item.caseId,
      caseType: item.caseType,
      name: item.name,
      priority: item.priority,
      firstName: item.firstName,
      lastName: item.lastName,
      phone: item.primaryPhoneNumber,
      email: item.email,
      quarantineEnd: item.quarantine?.to ? new Date(item.quarantine.to) : null,
      quarantineStart: item.quarantine?.from ? new Date(item.quarantine.from) : null,
      _links: item._links,
      alerts: item.healthSummary.concat(item.processSummary),
      status: item.status,
      caseTypeLabel: item.caseTypeLabel
    };
  }
}
