package com.luxoft.ssi.web.controllers

import com.luxoft.blockchainlab.corda.hyperledger.indy.AgentConnection
import com.luxoft.blockchainlab.corda.hyperledger.indy.handle
import com.luxoft.blockchainlab.hyperledger.indy.models.CredentialValue
import com.luxoft.ssi.web.components.IndyComponent
import com.luxoft.ssi.web.data.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.web.bind.annotation.*
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("api/credential")
@CrossOrigin
class CredentialController {
    private final val logger = KotlinLogging.logger {}

    @Lazy
    @Autowired
    private lateinit var indyAgent: AgentConnection

    @Autowired
    lateinit var indy: IndyComponent

    @GetMapping("issuerDid")
    fun issuerDid(): Response {
        return Response(indy.user.walletUser.getIdentityDetails().did)
    }

    @GetMapping("schemas")
    fun getSchemas(): Map<Int, IndySchema> {
        return PossibleIndySchemas.active.associate { it.hashCode() to it }
    }

    @PostMapping("issueCredentials")
    fun issueCredentials(@RequestBody request: IssueCredentialsRequest): Invite {
        val invite = indyAgent.generateInvite().toBlocking().value()
        indyAgent.waitForInvitedParty(invite, 300000).observeOn(Schedulers.newThread())
            .handle { indyPartyConnection, ex ->
            ex?.run {
                logger.error(ex) { "Error waiting for invited party" }
                return@handle
            }
            try {
                indyPartyConnection!!.apply {
                    request.listOfCredentials.entries.forEach { credential ->
                        val credentialDefinition =
                            indy.credDefStorage.findById(credential.key).get().getObj()
                        val createCredentialOffer =
                            indy.user.createCredentialOffer(credentialDefinition.getCredentialDefinitionIdObject())
                        sendCredentialOffer(createCredentialOffer)
                        val credentialRequest =
                            receiveCredentialRequest().timeout(60, TimeUnit.SECONDS)
                                .toBlocking().value()
                        val credentialInfo =
                            indy.user.issueCredentialAndUpdateLedger(credentialRequest, createCredentialOffer, null) {
                                credential.value.entries.forEach {
                                    attributes[it.key] = CredentialValue(it.value, "42")
                                }
                            }
                        sendCredential(credentialInfo)
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error while issuing credentials" }
            }
        }
        return Invite(invite)
    }
}
