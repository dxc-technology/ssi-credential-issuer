# Self-Sovereign Identity: Credential Issuer

## Introduction

Credential Issuer is an utility and a web application used to issue
cryptographically-valid Schemas for systems using 
[Hyperledger Indy](https://www.hyperledger.org/projects/hyperledger-indy)
*Self-Sovereign Identity* framework.

**Credential Schema** is the base semantic structure 
that describes the list of attributes which a particular issuing party 
can certify in a Credential Definition.

**Self-Sovereign Identity** is an identity system architecture 
based on the core principle that Identity Owners have the right 
to permanently control one or more Identifiers 
together with the usage of the associated Identity Data.

**Hyperledger Indy** is an open source project 
under the Hyperledger umbrella for decentralized Self-Sovereign Identity.

To learn more about *Hyperledger Indy*, please refer to 
[the official documentation](https://wiki.hyperledger.org/display/indy/Hyperledger+Indy).

## Contributing

We really want Credential Issuer to be simple to contribute to, 
and to ensure that you can get started quickly. 
A big part of that is being available to help you figure out the right way to solve a problem, 
and to make sure you get up to speed quickly.

If you want to help improve Credential Issuer directly 
we have a fairly detailed [CONTRIBUTING guide](CONTRIBUTING.md) in the repository.

We welcome contributions at all levels, including working strictly on our documentation, tests, or code contributions. 

## Installation

1. Install Docker and docker-compose on your system

1. Install Java 1.8, preferably OpenJDK 8.
Make sure `JAVA_HOME` and `java` are pointing to the correct JDK.

    `sudo apt install openjdk-8-jdk`

1. Install `libindy` native package version `1.8.2`

    `sudo apt install libindy=1.8.2`
    
1. Assemble the project and startup the network locally

    `./start.sh`
    
1. Run the integration tests

    `./gradlew test`
    
    
The instructions above were tested with Ubuntu 19.04. For other OSs please use their corresponding commands.

## Usage

Credential-Issuer can be used in 2 modes:
 
- Manually: by specifying the required parameters and running `CredentialIssuerIntegrationTest`
- As a Webapp: by running all sub-sytems and using Web UI to issue schemes 

### Manual Operation

Edit `src/test/resources/application.yaml` and specify variables:

 - indy.user.did - DID of the Issuer
 - indy.genesis.path - path to genesis file of the indy-pool in use 
   - default="genesis/docker_localhost.txt"

Use this syntax:

    indy:
      user:
        did: <Issuer's DID>
      genesis:
        path: <path to genesis file>
        
After specifying the external parameters run `src/test/kotlin/com/luxoft/ssi/web/CredentialIssuerIntegrationTest.kt`

It will generate a new Indy wallet in your home directory `~/.indy_client/wallet/<role specific directory>/*`.
The wallet is an SqLite database and consists of 3 files: `sqlite.db`, `sqlite.db-shm`, `sqlite.db-wal`.
You can use this wallet in your application if you copy those files 
into the corresponding `.indy_client/wallet/<role specific directory>` folder on the target device.

### Webapp setup

To deploy the system make sure that the following sub-systems are running in order:

1. *Indy-Pool*

    Ether use an existing indy-pool or deploy one locally with `docker-compose up -d indypool`

1. *Indy Agents*

    Deploy two Indy agents ether locally with `docker-compose up -d agent94 agent95` or somewhere in your network. 
    It is recommended using agents from docker image `teamblockchain/indy-agent-python`
    of the same version as it is used in `docker-compose.yml`
    By default agents are expected to be at `localhost:8094` and `localhost:8095`.

1. *Agent-Initiator* 

    Execute a GET method on each of the agents to initialise them.
    
    For the local setup it can be done with `docker-compose up agentInitiator`.
    
    See the content of `start.sh` for an example of a local setup.

1. *WebApp*

    Build and start the WebApp which is a Java Spring-based server with user interface. 
    
    Do it locally with 
    
        ./gradlew clean assemble
        docker-compose build webbaseimage
        docker-compose up -d webapp
        
    By default it is deployed to `localhost:8080`
        
        
For more details please refer to the content of `start.sh` and `docker-compose.yml` files.


## Licence

Credential Issuer is distributed under the Apache 2.0 license. See the LICENSE file for full details.
