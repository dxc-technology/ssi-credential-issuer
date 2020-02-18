package com.luxoft.ssi.web.jpa

import com.luxoft.blockchainlab.hyperledger.indy.models.CredentialDefinition
import com.luxoft.blockchainlab.hyperledger.indy.utils.SerializationUtils
import com.luxoft.ssi.web.data.IndySchema
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Repository
interface CredentialDefinitionCrudRepository : CrudRepository<CredentialDefinitionEntity, Int>

@Entity
class CredentialDefinitionEntity() {
    @Id
    var schema: Int = 0
    @Lob
    var json: String = ""

    constructor(schema: IndySchema, credDef: CredentialDefinition) : this() {
        this.schema = schema.hashCode()
        this.json = SerializationUtils.anyToJSON(credDef)
    }

    fun getObj() = SerializationUtils.jSONToAny<CredentialDefinition>(json)
}