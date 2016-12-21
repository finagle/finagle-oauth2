[![Build Status](https://img.shields.io/travis/finagle/finagle-oauth2/master.svg)](https://travis-ci.org/finagle/finagle-oauth2)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.finagle/finagle-oauth2_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.finagle/finagle-oauth2_2.12)

OAuth2 Provider for Finagle
---------------------------

This is a [finagled](https://github.com/twitter/finagle) **OAuth2** _server-side_ provider based on
the source code from a [scala-oauth2-provider](https://github.com/nulab/scala-oauth2-provider)
library. Since [scala-oauth2-provider](https://github.com/nulab/scala-oauth2-provider) it involves:

 - an asynchronous version of a `DataHandler` trait
 - asynchronous versions of both `TokenEndpoint` and `ProtectedResource` handlers
 - an asynchronous version of unit tests
 - four new finagled entities: `OAuth2`, `OAuth2Request`, `OAuth2Filter`, `OAuth2Endpoint`

This makes the usage of this library very sleek from a finagled environment. The brief usage
instruction looks as follows:

 - define an implementation of a `DataHandler` interface
 - define a service that emits access tokens using either
  - simple http service with `OAuth2` trait mixed or
  - configurable `OAuth2Endpoint` instantiated
 - define a protected service using either
  - simple http service with `OAuth2` trait mixed or
  - type-safe service `Service[OAuth2Request[U], Response]` and `OAuth2Filter` applied

Finagle OAuth2 is build on top of `finagle-http` and tt compiles with both Scala 2.11 and 2.10.

### SBT Artifacts
```scala
libraryDependencies ++= Seq(
  "com.github.finagle" %% "finagle-oauth2" % "0.2.0"
)
```

### User Guide

There are _two_ possible ways of using finagle-oauth2 provider:
- via `OAuth2` trait mixed to finagled service
- via predefined type-safe classes `OAuth2Request`, `OAuth2Filter`, `OAuth2Endpoint`
  (a **preferred** way)

##### Using `OAuth2` trait

`OAuth2` trait provides two asynchronous methods: `issueAccessToken` and `authorize`. 

> A token service that emits OAuth2 access tokens.

```scala
import com.twitter.finagle.OAuth2
import com.twitter.finagle.oauth2._

object TokenService extends Service[Request, Response] with OAuth2 {
  def apply(req: Request) = 
    issueAccessToken(req, dataHandler) flatMap { token =>
      val rep = Response(Version.Http11, Status.Ok)
      rep.setContentString(token.accessToken)
      Future.value(rep)
    } handle {
      case e: OAuthError => e.toHttpResponse
    }
}
```

> A protected service that checks whether the request authorized or not.

```scala
import com.twitter.finagle.OAuth2
import com.twitter.finagle.oauth2._

object ProtectedService extends Service[Request, Response] with OAuth2 {
  def apply(req: Request) =
    authorize(req, dataHandler) flatMap { authInfo =>
      val rep = Response(Version.Http11, Status.Ok)
      rep.setContentString(s"Hello ${authInfo.user}")
      Future.value(rep)
    } handle {
      case e: OAuthError => e.toHttpResponse
    }
}
```

##### Using type-safe `OAuth2Filter` and `OAuth2Request`

It's preferred to use the power of Finagle filters along with type-safe services. 
The code below shows how to use new building blocks `OAuth2Filter` and `OAuth2Request` 
in order to build robust type-safe services.

```scala
import com.twitter.finagle.oauth2._
import com.twitter.finagle.{OAuth2Request, OAuth2Filter, OAuth2Endpoint}

// accessing a protected resource via finagled filter
val auth = new OAuth2Filter(dataHandler) with OAuthErrorInJson

// a protected resource example
val hello = new Service[OAuth2Request[User], Response] {
  def apply(req: OAuth2Request[User]) = {
    println(s"Hello, ${req.authInfo.user}!")
    Future.value(Response())
  }
}

// combines the things together
val backend = RoutingService.byPathObject {
  case Root / "hello" => auth andThen hello
}
```

##### Using `OAuth2Endpoint`

It doesn't make sense to write an http service that emits access tokens in every new project. 
A finagle-oauth2 provider contains a configurable version of `OAuth2Endpoint`.

```scala
import com.twitter.finagle.oauth2._
import com.twitter.finagle.{OAuth2Request, OAuth2Filter, OAuth2Endpoint}

// emitting access tokens via finagled service
val tokens = new OAuth2Endpoint(dataHandler) with OAuthErrorInJson with OAuthTokenInJson

// combines the things together
val backend = RoutingService.byPathObject {
  case Root / "auth" => tokens
}
```

##### `OAuthErrorHandler` and `OAuthTokenConverter`
Both classes `OAuth2Filter` and `OAuth2Endpoint` uses trait-mixing in order to override the
behaviour on handling errors and serializing tokens. The traits defines such behaviour look as
following:

```scala
trait OAuthErrorHandler {
  def handleError(e: OAuthError): Response
}

trait OAuthTokenConverter {
  def convertToken(token: GrantHandlerResult): Response
}
```

So you can define your own error handler or token converter and mix it into a concrete instance like
this:

```scala
trait MyErrorHandler extends OAuthErrorHandler {
  // we don't care at all
  def handleError(e: OAuthError) = Response(Status.Ok)
}

val tokens = new OAuth2Endpoint with MyErrorHandler
```

By default errors are serialized in `WWW-Authenticate` header while access tokens are serialized as
simple string in the response body.

There are also two predefined traits: `OAuthErrorInJson` and `OAuthTokenInJson` which serializes
everything into JSON objects.

