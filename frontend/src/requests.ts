import fetch from 'unfetch';

const API_BASE_URL = window.location.origin;

export interface StringMap {
    [key: string]: string
}

export type SchemaList = { [schemaHash: number]: IndySchema }
export type CredentialProposal = StringMap

export interface IndySchema {
    name: string;
    attributes: StringMap;
    version: string;
}

export interface IssueCredentialsRequest {
    listOfCredentials: { [schemaHash: number]: CredentialProposal }
}

export interface Invite {
    invite: string
}

export interface IndySchemaWithHash extends IndySchema {
    hash: number
}

export interface SchemaByVersion {
    [version: string]: IndySchemaWithHash
}

export interface SchemasByNameByVersion {
    [name: string]: SchemaByVersion
}

export async function getSchemas(): Promise<SchemaList> {
    const response = await fetch(`${API_BASE_URL}/api/credential/schemas`);
    return await response.json()
}

export async function issueCredentials(request: IssueCredentialsRequest): Promise<Invite> {
    const response = await fetch(
        `${API_BASE_URL}/api/credential/issueCredentials`,
        {
            method: "POST",
            body: JSON.stringify(request),
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }
        }
    );
    return await response.json()
}
