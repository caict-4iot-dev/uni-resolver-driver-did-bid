![DIF Logo](https://raw.githubusercontent.com/decentralized-identity/universal-resolver/master/docs/logo-dif.png)

# Universal Resolver Driver: did:bid

This is a [Universal Resolver](https://github.com/decentralized-identity/universal-resolver/) driver for Caict **did:bid** identifiers.

## Specifications

* [Decentralized Identifiers](https://www.w3.org/TR/did-core/)
* [DID Method Specification](https://github.com/teleinfo-bif/bid/blob/master/doc/en/readme.md)

## Example DIDs

```
did:bid:ef214PmkhKndUcArDQPgD5J4fFVwqJFPt
```

## Build and Run (Docker)

```
docker build -f ./docker/Dockerfile . -t universalresolver/driver-did-bid
docker run -p 8080:8080 universalresolver/driver-did-bid
curl -X GET http://localhost:8080/1.0/identifiers/did:bid:ef214PmkhKndUcArDQPgD5J4fFVwqJFPt
```

## Build (native Java)

Maven build:

    mvn clean install

## Driver Metadata

The driver returns the following metadata in addition to a DID document:

* `proof`: Some proof info about the DID document.
* `created`: The DID create time.
* `updated`: The DID document last update time.
