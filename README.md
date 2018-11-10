[![Build Status](https://img.shields.io/travis/finagle/finagle-oauth2/master.svg)](https://travis-ci.org/finagle/finagle-oauth2)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.finagle/finagle-oauth2_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.finagle/finagle-oauth2_2.12)

OAuth2 Provider for Finagle
---------------------------

This is a [Finagle]-friendly version of [scala-oauth2-provider].

## User Guide

 1. Implement `com.twitter.finagle.oauth2.DataHandler` using your own data store (in-memory, DB, etc).
 2. Use `com.twitter.finagle.OAuth2` API to authorize requests and issue access tokens.

> A service that emits OAuth2 access tokens based on request's credentials.

```scala
import com.twitter.finagle.OAuth2
import com.twitter.finagle.oauth2.{OAuthError, DataHandler}

import com.twitter.finagle.http.{Request, Response, Version, Status}
import com.twitter.finagle.Service
import com.twitter.util.Future

val dataHandler: DataHandler[?] = ???

object TokenService extends Service[Request, Response] with OAuth2 {
  def apply(req: Request): Future[Response] =
    issueAccessToken(req, dataHandler).flatMap { token =>
      val rep = Response(Version.Http11, Status.Ok)
      rep.setContentString(token.accessToken)
      Future.value(rep)
    } handle {
      case e: OAuthError => e.toResponse
    }
}
```

> A service that checks whether the request contains a valid token.

```scala
import com.twitter.finagle.OAuth2
import com.twitter.finagle.oauth2.{OAuthError, DataHandler}

import com.twitter.finagle.http.{Request, Response, Version, Status}
import com.twitter.finagle.Service
import com.twitter.util.Future

object ProtectedService extends Service[Request, Response] with OAuth2 {
  def apply(req: Request): Future[Response] =
    authorize(req, dataHandler).flatMap { authInfo =>
      val rep = Response(Version.Http11, Status.Ok)
      rep.setContentString(s"Hello ${authInfo.user}!")
      Future.value(rep)
    } handle {
      case e: OAuthError => e.toResponse
    }
}
```

[Finagle]: https://github.com/twitter/finagle
[scala-oauth2-provider]: https://github.com/nulab/scala-oauth2-provider

