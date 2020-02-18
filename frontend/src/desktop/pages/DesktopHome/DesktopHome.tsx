import * as React from 'react'
import {
    CredentialProposal,
    getSchemas,
    IndySchemaWithHash,
    Invite,
    issueCredentials,
    IssueCredentialsRequest,
    SchemaByVersion,
    SchemaList,
    SchemasByNameByVersion, StringMap
} from "../../../requests";
import QRCode from 'qrcode.react';
import * as styles from './DesktopHome.scss';

interface DesktopHomeState {
    schemas: SchemasByNameByVersion;
    proposal: CredentialProposal;
    activeSchemaName: string;
    activeSchemaVersion: string;
    attributeValues: StringMap;
    invite: Invite | null;
}

export default class DesktopHome extends React.Component<any, DesktopHomeState> {
    state = {
        schemas: {},
        proposal: {},

        activeSchemaName: "",
        activeSchemaVersion: "",
        attributeValues: {},

        invite: null
    };

    componentDidMount(): void {
        getSchemas()
            .then((schemas: SchemaList) => {
                const schemasByNameByVersion = Object.entries(schemas).reduce<SchemasByNameByVersion>(
                    (acc, [schemaHash, schema]) => {
                        acc[schema.name]
                            ? acc[schema.name][schema.version] = {...schema, hash: parseInt(schemaHash)}
                            : acc[schema.name] = {[schema.version]: {...schema, hash: parseInt(schemaHash)}};
                        return acc
                    },
                    {}
                );

                const name = Object.keys(schemasByNameByVersion)[0];
                const versions = Object.keys(schemasByNameByVersion[name] as SchemaByVersion);
                const version = versions[versions.length-1];

                const attributeValues = {};
                Object.entries(schemasByNameByVersion[name][version].attributes)
                    .forEach(([attrName, attrDefault]) => attributeValues[attrName] = attrDefault);

                this.setState({
                    schemas: schemasByNameByVersion,
                    activeSchemaName: name,
                    activeSchemaVersion: version,
                    attributeValues
                })
            })
    }

    handleNameChange(e) {
        const {schemas} = this.state;

        const newSchemaName: string = e.target.value || "";
        const schemaVersions = Object.keys(schemas[newSchemaName] as SchemaByVersion);
        const newSchemaVersion = schemaVersions[schemaVersions.length-1];

        const attributeValues = {};
        Object.entries(schemas[newSchemaName][newSchemaVersion].attributes)
            .forEach(([attrName, attrDefault]) => attributeValues[attrName] = attrDefault);

        this.setState({
            activeSchemaName: newSchemaName,
            activeSchemaVersion: newSchemaVersion,
            attributeValues,
            invite: null
        })
    }

    handleVersionChange(e) {
        const newSchemaVersion = e.target.value || "";
        const {schemas, activeSchemaName} = this.state;

        const attributeValues = {};
        Object.entries(schemas[activeSchemaName][newSchemaVersion].attributes)
            .forEach(([attrName, attrDefault]) => attributeValues[attrName] = attrDefault);

        this.setState({
            activeSchemaVersion: newSchemaVersion,
            attributeValues,
            invite: null
        })
    }

    handleAttrChange(e, attribute: string) {
        const newAttrValue = e.target.value || "default-value";

        this.setState({
            ...this.state,
            [attribute]: newAttrValue,
            invite: null
        })
    }

    handleIssueClick() {
        this.setState({invite: null}, () => {
            const {schemas, activeSchemaName, activeSchemaVersion, attributeValues} = this.state;
            const activeSchemaHash = (schemas[activeSchemaName][activeSchemaVersion] as IndySchemaWithHash).hash;

            const credentialsRequest: IssueCredentialsRequest = {
                listOfCredentials: {
                    [activeSchemaHash]: attributeValues
                }
            };

            issueCredentials(credentialsRequest)
                .then(invite => this.setState({invite}))
        });
    }

    render() {
        const {schemas, activeSchemaName, activeSchemaVersion, attributeValues, invite} = this.state;

        const schemaNames: JSX.Element[] = Object.keys(schemas)
            .map((schemaName, index) => <option key={index} value={schemaName}>{schemaName}</option>);

        const schemaVersions: JSX.Element[] = schemaNames.length > 0
            ? Object.keys(schemas[activeSchemaName])
                .map((schemaVersion, index) => <option key={index} value={schemaVersion}>{schemaVersion}</option>)
            : [];

        const schemaAttrs: JSX.Element[] = Object.entries(attributeValues as StringMap)
            .map(([key, value], index) =>
                <div className={styles.attrWrapper} key={index}>
                    <span className={styles.attrKey}>{key}</span>
                    <input className={styles.attrValue} onChange={(e) => this.handleAttrChange(e, key)} value={value} type="text"/>
                </div>
            );

        return (
            <div className={styles.root}>
                <div className={styles.left}>
                    <div className={styles.schemaWrapper}>
                        <h2 className={styles.instruction}>Select a schema and fill up parameters</h2>
                        <div className={styles.schemaTune}>
                            <div className={styles.selectWrapper}>
                                <h3 className={styles.selectLabel}>Name: </h3>
                                <select
                                    value={activeSchemaName}
                                    onChange={(e) => this.handleNameChange(e)}
                                    name="schemaName"
                                    id="schemaName"
                                    className={styles.select}
                                >
                                    {schemaNames}
                                </select>
                            </div>

                            <div className={styles.selectWrapper}>
                                <h3 className={styles.selectLabel}>Version: </h3>
                                <select
                                    value={activeSchemaVersion}
                                    onChange={(e) => this.handleVersionChange(e)}
                                    name="schemaVersion"
                                    id="schemaVersion"
                                    className={styles.select}
                                >
                                    {schemaVersions}
                                </select>
                            </div>
                        </div>
                        <div className={styles.schemaAttrs}>
                            <h3 className={styles.attrsLabel}>Attributes: </h3>
                            {schemaAttrs}
                        </div>
                        <button className={styles.button} onClick={() => this.handleIssueClick()}>Issue</button>
                    </div>
                </div>

                <div className={styles.right}>
                    {invite &&
                    <div className={styles.qrWrapper}><QRCode fgColor="#161616" value={JSON.stringify(invite)} size={400} level='H'/>
                    </div>}
                </div>
            </div>
        )
    }
}

