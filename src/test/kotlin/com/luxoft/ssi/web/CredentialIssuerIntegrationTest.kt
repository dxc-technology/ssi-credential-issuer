package com.luxoft.ssi.web

import com.luxoft.blockchainlab.corda.hyperledger.indy.PythonRefAgentConnection
import com.luxoft.blockchainlab.hyperledger.indy.IndyUser
import com.luxoft.blockchainlab.hyperledger.indy.SsiUser
import com.luxoft.blockchainlab.hyperledger.indy.helpers.GenesisHelper
import com.luxoft.blockchainlab.hyperledger.indy.helpers.PoolHelper
import com.luxoft.blockchainlab.hyperledger.indy.helpers.WalletHelper
import com.luxoft.blockchainlab.hyperledger.indy.ledger.IndyPoolLedgerUser
import com.luxoft.blockchainlab.hyperledger.indy.utils.FilterProperty
import com.luxoft.blockchainlab.hyperledger.indy.utils.proofRequest
import com.luxoft.blockchainlab.hyperledger.indy.utils.reveal
import com.luxoft.blockchainlab.hyperledger.indy.wallet.IndySDKWalletUser
import com.luxoft.ssi.web.controllers.CredentialController
import com.luxoft.ssi.web.data.IssueCredentialsRequest
import com.luxoft.ssi.web.data.PossibleIndySchemas
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.random.nextUInt

@RunWith(SpringRunner::class)
@SpringBootTest
//@ActiveProfiles("poc")
class CredentialIssuerIntegrationTest {

    companion object {
        fun randomDid() = WalletHelper.openOrCreate("did-gen-wallet", "password").run {
            IndySDKWalletUser(this).getIdentityDetails().did.apply {
                closeWallet().get()
            }
        }

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            // disable for "profile poc"
            randomDid().apply {
                println("Test DID:$this")
                System.setProperty("indy.user.did", this)
            }
        }
    }

    lateinit var pool: Pool
    var poolName: String = PoolHelper.DEFAULT_POOL_NAME

    private val walletPassword = "password"
    private val ownerWalletName = "ownerWallet"  // copy into Android
    private val verifierWalletName = "verifierWallet"

    @Value("\${indy.genesis.path:genesis/docker_localhost.txn}")
    lateinit var GENESIS_PATH: String

    private lateinit var owner: SsiUser
    private lateinit var verifier: SsiUser
    private var walletsList = mutableListOf<Wallet>()

    @Autowired
    lateinit var credentialController: CredentialController

    private val agentClient = PythonRefAgentConnection().apply {
        connect(
            url = "ws://localhost:8094/ws",
            login = "agentUser",
            password = "password"
        ).toBlocking().value()
    }

    fun createSsiUser(walletName: String, walletPassword: String) = run {
        WalletHelper.createOrTrunc(walletName, walletPassword)
        val wallet = WalletHelper.openExisting(walletName, walletPassword)

        val walletUser = IndySDKWalletUser(wallet)
        val ledgerUser = IndyPoolLedgerUser(pool, walletUser.getIdentityDetails().did) { walletUser.sign(it) }
        walletsList.add(wallet)
        IndyUser.with(walletUser).with(ledgerUser).build()
    }

    @Before
    fun before() {
        val genesisFile = File(GENESIS_PATH)
        if (!GenesisHelper.exists(genesisFile))
            throw RuntimeException("Genesis file ${genesisFile.absolutePath} doesn't exist")

        PoolHelper.createOrTrunc(genesisFile, poolName)
        pool = PoolHelper.openExisting(poolName)

        owner = createSsiUser(ownerWalletName, walletPassword)
        verifier = createSsiUser(verifierWalletName, walletPassword)
    }

    @After
    fun after() {
        walletsList.forEach { it.closeWallet().get() }

        pool.closePoolLedger().get()
        Pool.deletePoolLedgerConfig(poolName)
    }

    @Test
    fun issueSpecificCredentials() {
        val medicalCondition = "Leukemia"
        val userImage = loadImageBytes("user_images/patient-avatar.png", "png")
        val userImageStr = Base64.getEncoder().encodeToString(userImage)

        val request = IssueCredentialsRequest(
            mapOf(
                PossibleIndySchemas.Person.v1_0.hashCode() to mapOf(
                    "socialid" to "${Random.nextUInt()}",
                    "name" to "Mark Rubinstein",
                    "birthday" to "1974-01-01",
                    "gender" to "male",
                    "picture" to userImageStr
                ),
                PossibleIndySchemas.Insurance.v1_0.hashCode() to mapOf(
                    "medicalid" to "${Random.nextUInt()}",
                    "insurer" to "Techniker Krankenkasse TK",
                    "limit" to "100000$",
                    "partners" to "TC SEEHOF, Marina Bay Hospital"
                ),
                PossibleIndySchemas.Prescription.v1_0.hashCode() to mapOf(
                    "diagnosis" to medicalCondition,
                    "prescription" to "Santorium"
                )
            )
        )

        val invite = credentialController.issueCredentials(request).invite

        agentClient.acceptInvite(invite).toBlocking().value().apply {
            repeat(request.listOfCredentials.entries.size) {
                val credentialOffer = receiveCredentialOffer().toBlocking().value()
                val credentialRequest =
                    owner.createCredentialRequest(owner.walletUser.getIdentityDetails().did, credentialOffer)
                sendCredentialRequest(credentialRequest)
                val credential = receiveCredential().toBlocking().value()
                owner.checkLedgerAndReceiveCredential(credential, credentialRequest, credentialOffer)
            }
        }

        val issuerDid = credentialController.indy.user.walletUser.getIdentityDetails().did

        val proofReq = proofRequest("proof_req", "0.1") {
            reveal("socialid")
            reveal("name")
            reveal("birthday")
            reveal("gender")
            reveal("picture")
            reveal("medicalid") { FilterProperty.IssuerDid shouldBe issuerDid }
            reveal("insurer") {
                //TODO: Can not create proof, fix me
                //                FilterProperty.Value shouldBe medicalCondition
                FilterProperty.IssuerDid shouldBe issuerDid
            }
            reveal("limit")
            reveal("diagnosis")
            reveal("prescription")
        }

        val proof = owner.createProofFromLedgerData(proofReq)

        assertEquals(userImageStr, proof.get("picture")!!.raw)
        assertTrue(verifier.verifyProofWithLedgerData(proofReq, proof))
    }

    @Test
    fun issueAllActiveCredentials() {
        val schemas = credentialController.getSchemas()

        val request = IssueCredentialsRequest(
            schemas.mapValues { it.value.attributes }
        )

        val invite = credentialController.issueCredentials(request).invite

        agentClient.acceptInvite(invite).toBlocking().value().apply {
            repeat(request.listOfCredentials.entries.size) {
                val credentialOffer = receiveCredentialOffer().toBlocking().value()
                val credentialRequest =
                    owner.createCredentialRequest(owner.walletUser.getIdentityDetails().did, credentialOffer)
                sendCredentialRequest(credentialRequest)
                val credential = receiveCredential().toBlocking().value()
                owner.checkLedgerAndReceiveCredential(credential, credentialRequest, credentialOffer)
            }
        }

        val issuerDid = credentialController.indy.user.walletUser.getIdentityDetails().did

        val proofReq = proofRequest("proof_req", "0.1") {
            schemas.forEach {
                it.value.attributes.entries.forEachIndexed { index, entry ->
                    //TODO: add this filter to parent lib
                    val attr = entry.key.replace(" ", "")
                    reveal(attr) {
                        if (index.rem(2) == 0)
                            FilterProperty.IssuerDid shouldBe issuerDid
                        else
                            attr shouldBe entry.value
                    }
                }
            }
        }

        val proof = owner.createProofFromLedgerData(proofReq)
        assertTrue(verifier.verifyProofWithLedgerData(proofReq, proof))
    }
}

fun loadImageBytes(resourcePath: String, extension: String = "png"): ByteArray {
    val imageUrl: URL? = object{}::class.java.classLoader.getResource(resourcePath)
    imageUrl ?: throw IOException("Resource $resourcePath could not be found in test resources")
    val imageFile = File(imageUrl.toURI())
    val image: BufferedImage = ImageIO.read(imageFile)
    val bos = ByteArrayOutputStream()
    ImageIO.write(image, extension, bos)
    return bos.toByteArray()
}
