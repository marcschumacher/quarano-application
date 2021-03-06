import { ClientType } from './report-case';

export interface ClientDto {
  clientCode?: string;
  clientId: number;
  lastName: string;
  firstName: string;
  phone?: string;
  zipCode: string;
  street?: string;
  city?: string;
  email?: string;
  mobilePhone?: string;
  infected: boolean;
  quarantineStartDateTime?: Date;
  quarantineEndDateTime?: Date;
  houseNumber: string;
  dateOfBirth: Date;
  type: ClientType;
}

