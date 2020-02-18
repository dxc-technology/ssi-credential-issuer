package com.luxoft.ssi.web.components

import com.luxoft.blockchainlab.hyperledger.indy.IndyUser
import com.luxoft.blockchainlab.hyperledger.indy.SsiUser
import com.luxoft.blockchainlab.hyperledger.indy.helpers.GenesisHelper
import com.luxoft.blockchainlab.hyperledger.indy.helpers.PoolHelper
import com.luxoft.blockchainlab.hyperledger.indy.helpers.WalletHelper
import com.luxoft.blockchainlab.hyperledger.indy.ledger.IndyPoolLedgerUser
import com.luxoft.blockchainlab.hyperledger.indy.models.DidConfig
import com.luxoft.blockchainlab.hyperledger.indy.wallet.IndySDKWalletUser
import com.luxoft.blockchainlab.hyperledger.indy.wallet.getOwnIdentities
import com.luxoft.ssi.web.data.PossibleIndySchemas
import com.luxoft.ssi.web.jpa.CredentialDefinitionCrudRepository
import com.luxoft.ssi.web.jpa.CredentialDefinitionEntity
import mu.KotlinLogging
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.pool.Pool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import javax.annotation.PostConstruct

@Component
class IndyComponent {
    private final val logger = KotlinLogging.logger {}

    @Value("\${indy.genesis.path:genesis/docker_localhost.txn}")
    lateinit var GENESIS_PATH: String

    @Value("\${indy.pool.name:default-pool}")
    lateinit var poolName: String

    @Value("\${indy.wallet.name:credential-issuer-wallet}")
    lateinit var walletName: String

    @Value("\${indy.wallet.password:password}")
    lateinit var walletPassword: String

    @Value("\${indy.user.did:#{null}}")
    private val did: String? = null

    @Value("\${indy.user.seed:#{null}}")
    private val seed: String? = null

    @Autowired
    lateinit var credDefStorage: CredentialDefinitionCrudRepository

    lateinit var user: SsiUser

    @PostConstruct
    fun init() {
        logger.info("Indy initialization")

        val wallet = WalletHelper.openOrCreate(walletName, walletPassword)
        val genesisFile = File(GENESIS_PATH)
        if (!GenesisHelper.exists(genesisFile))
            throw RuntimeException("Genesis file $GENESIS_PATH doesn't exist")

        val pool = PoolHelper.openOrCreate(genesisFile, poolName)
        val constantDid = did ?: wallet.getOwnIdentities().firstOrNull()?.did
        val walletUser =
            constantDid?.run { wallet.getOwnIdentities().find { it.did == this } }?.run {
                logger.info { "Opening wallet with DID($did)" }
                IndySDKWalletUser(wallet, did)
            } ?: run {
                logger.info { "Creating new identity with DID(${did ?: "\$random"}) and seed(${seed ?: "\$random"}" }
                IndySDKWalletUser(wallet, DidConfig(did, seed))
            }.apply {
                logger.info { "Opened wallet with DID($did,$verkey)" }
            }
        val ledgerUser = IndyPoolLedgerUser(pool, walletUser.getIdentityDetails().did) { walletUser.sign(it) }
        user = IndyUser.with(walletUser).with(ledgerUser).build()

        val nym = user.ledgerUser.getNym(user.walletUser.getIdentityDetails())
        nym.result.getData() ?: run {
            grantTrust(pool)
        }

        PossibleIndySchemas.active.forEach {
            if (!credDefStorage.findById(it.hashCode()).isPresent) {
                user.apply {
                    val schema = createSchemaAndStoreOnLedger(it.name, it.version, it.attributes.keys.toList())
                    logger.info { "Created schema($schema)" }
                    val credentialDefinition =
                        createCredentialDefinitionAndStoreOnLedger(schema.getSchemaIdObject(), false)
                    credDefStorage.save(CredentialDefinitionEntity(it, credentialDefinition))
                }
            }
        }

        logger.info("Indy initialization passed")
    }

    private fun grantTrust(pool: Pool) {
        val TRUSTEE_SEED = "000000000000000000000000Trustee1"
        val trusteeWalletName = "Trustee"
        val trusteeWalletPassword = "123"

        WalletHelper.createOrTrunc(trusteeWalletName, trusteeWalletPassword)
        val trusteeWallet = WalletHelper.openExisting(trusteeWalletName, trusteeWalletPassword)
        val trusteeDid = Did.createAndStoreMyDid(trusteeWallet, """{"seed":"$TRUSTEE_SEED"}""").get()

        IndyPoolLedgerUser(pool, trusteeDid.did) {
            IndySDKWalletUser(trusteeWallet, trusteeDid.did).sign(it)
        }.storeNym(user.walletUser.getIdentityDetails().copy(role = "TRUSTEE"))
    }

}
