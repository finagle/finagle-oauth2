OAuth2 Provider for Finagle
---------------------------------------

This is a [finagled](https://github.com/twitter/finagle) **OAuth2** _server-side_ provider based on the source code from a [scala-oauth2-provider](https://github.com/nulab/scala-oauth2-provider) library. Since [scala-oauth2-provider](https://github.com/nulab/scala-oauth2-provider) it involves 

 - an asynchronus version of a `DataHandler` interface
 - an asynchronus version of both `TokenEndpoint` and `ProtectedResource` handlers
 - an asyncrhonus version of tests
 - three new finagled entities: `OAuth2`, `OAuth2Request`, `OAuth2Filter`

This makes the usage of this library very sleek from a finagled environment. The brief usage instruction looks as follows:

 - define an implementation of a `DataHandler` interface
 - define a service that emmits access tokens
 - define a protected service using either
  - simple http service with `OAuth2` trait mixed or
  - type-safe service `Service[OAuth2Request[U], Response]` and `OAuth2Filter` applied

#### SBT Artifacts
```scala
resolvers += "Finagle-OAuth2" at "http://repo.konfettin.ru"

libraryDependencies ++= Seq(
  "com.twitter" %% "oauth2" % "0.1.0"
)
```

#### Sample
The sample bellow implements one of the OAuth2 supported schemas: _Client Credentials Flow_. Thus some of the methods in `LocalDataHandler` are not implemented. In order to provide a full-support of all avaiable OAuth2 schemas all the methods should be implemented. The high-level description of a Client Credentials Flow schema is following:

_Emitting Tokens_
 - `validateClient`
 * `findClientUser`
 * `getStoredAccessToken`
 * `isAccessTokenExpired`
 * `refreshAccessToken`
 * `createAccessToken`

_Accessing a Protected Resource_
 - `findAccessToken`
 * `isAccessTokenExpired`
 * `findAuthInfoByAccessToken`
```scala
import com.twitter.finagle.oauth2._
// ''Long'' is our user (his ID)
object LocalDataHandler extends DataHandler[Long] {
  /**
   *  We map (a) users (their IDs) along with granted tokens
   *         (b) tokens along with auth information
   */
  val forwardStorage = collection.mutable.Map[Long, AccessToken]()
  val backwardStorage = collection.mutable.Map[String, AuthInfo[Long]]()
  /**
   * Our database.
   */
  val db = Map(1l -> ("Ivan", "12345"), 2l -> ("John", "password"))

  // Everyone is a valid user. 
  def validateClient(clientId: String, clientSecret: String, grantType: String) = 
    Future.value(true)

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]) =
    Future.value(db.find {
      case (_, (id, secret)) => (id == clientId) && (secret == clientSecret)
    } flatMap {
      case (id, _) => Some(id)
    })

  def createAccessToken(authInfo: AuthInfo[Long]) = {
    val token = AccessToken(Random.alphanumeric.take(20).mkString, 
                            None, Some("all"), Some(3600), new java.util.Date())
    forwardStorage += (authInfo.user -> token)
    backwardStorage += (token.token -> authInfo)
    Future.value(token)
  }

  def findAccessToken(token: String) =
    Future.value(forwardStorage.values.find { t => t.token == token })

  def findAuthInfoByAccessToken(accessToken: AccessToken) =
    Future.value(backwardStorage.get(accessToken.token))

  // We don't need these methods for this example, we can just return 
  // future of none. This will lead to a bad-request response for any
  // unsupported schema.
  def findAuthInfoByCode(code: String) = Future.None
  def findUser(username: String, password: String) = Future.None
  def getStoredAccessToken(authInfo: AuthInfo[Long]) = Future.None
  def findAuthInfoByRefreshToken(refreshToken: String) = Future.None
  def refreshAccessToken(authInfo: AuthInfo[Long], refreshToken: String) = Future.never
}
```
A service that issues tokens is nothing different from any other finagled service.
```scala
import com.twitter.finagle.OAuth2
import com.twitter.finagle.oauth2._

object TokenService extends Service[Request, Response] with OAuth2 {
  def apply(req: Request) =
    issueAccessToken(req, LocalDataHandler) flatMap { token =>
      val rep = Response(Version.Http11, Status.Ok)
      rep.setContentString(token.accessToken)
      Future.value(rep)
    } handle {
      case e: OAuthError => e.toHttpResponse
    }
}
```
This protected service is implemented atop of regular http service with `OAuth2` trait mixed.
```scala
import com.twitter.finagle.OAuth2
import com.twitter.finagle.oauth2._

object ProtectedService extends Service[Request, Response] with OAuth2 {
  def apply(req: Request) =
    authorize(req, LocalDataHandler) flatMap { i =>
      val rep = Response(Version.Http11, Status.Ok)
      rep.setContentString(s"Hello ${i.user}")
      Future.value(rep)
    } handle {
      case e: OAuthError => e.toHttpResponse
    }
}
```
In order to combine services togetger `RoutingService` may be used.
```scala
object Main extends App {
  val backend = RoutingService.byPathObject {
    case Root / "authorize" => TokenService
    case Root / "hello" => ProtectedService
  }

  ServerBuilder()
    .codec(RichHttp[Request](Http()))
    .bindTo(new InetSocketAddress(8080))
    .name("backend")
    .build(backend)
}
```
Testing services.
```bash
$ curl -D - "localhost:8080/authorize?grant_type=client_credentials&client_id=Ivan&client_secret=12345"
HTTP/1.1 200 OK
Content-Length: 20

IhPJKquZT6eLYHfPvA40

$ curl -D - "localhost:8080/hello?oauth_token=IhPJKquZT6eLYHfPvA40"
HTTP/1.1 200 OK
Content-Length: 7

Hello 1
```
#### A type-safe OAuth2 Filter & Request
```scala
import com.twitter.finagle.oauth2._
import com.twitter.finagle.{OAuth2Request, OAuth2Filter}

object TypeSafeProtectedService extends Service[OAuth2Request[Long], Response] {
  def apply(req: OAuth2Request[Long]) = {
    val rep = Response(Version.Http11, Status.Ok)
    rep.setContentString(s"Hello ${req.authInfo.user}")

    Future.value(rep)
  }
}

object Main extends App {

  val auth = new OAuth2Filter(LocalDataHandler)

  val backend = RoutingService.byPathObject {
    case Root / "authorize" => TokenService
    case Root / "hello" => auth andThen TypeSafeProtectedService
  }

  ServerBuilder()
    .codec(RichHttp[Request](Http()))
    .bindTo(new InetSocketAddress(8080))
    .name("backend")
    .build(backend)
}
```

----
By Vladimir Kostyukov, http://vkostyukov.ru

