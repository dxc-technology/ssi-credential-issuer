package com.luxoft.ssi.web.components

import com.luxoft.blockchainlab.corda.hyperledger.indy.AgentConnection
import com.luxoft.blockchainlab.corda.hyperledger.indy.PythonRefAgentConnection
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Configuration
class IndyAgentClient {
    private final val logger = KotlinLogging.logger {}

    @Value("\${indy.agent.endpoint:ws://localhost:8095/ws}")
    lateinit var agentEndpoint: String

    @Value("\${indy.agent.user:credential-issuer}")
    lateinit var agentUser: String

    @Value("\${indy.wallet.password:password}")
    lateinit var agentPassword: String

    @Lazy
    @Bean
    fun agentClient(): AgentConnection = run {
        try {
            PythonRefAgentConnection().apply {
                connect(
                    url = agentEndpoint,
                    login = agentUser,
                    password = agentPassword
                ).toBlocking().value()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error while creating IndyAgentClient" }
            throw e
        }
    }
}