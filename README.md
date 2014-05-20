OAuth2 Provider for Finagle
---------------------------------------

How to use?

#### Define a DataHandler implementation
```scala
import com.twitter.finagle.oauth2._

// This DatatHandler is used for Client Credentials Flow Scheme,
// so we don't need to implement all of these methods.
// ''Long'' is our user (his ID)
object LocalDataHandler extends DataHandler[Long] {
  /**
   *  We map (a) users (ther IDs) aloong with granted tokens
   *         (b) tokens along with auth information
   */
  val forwardStorage = collection.mutable.Map[Long, AccessToken]()
  val backwardStorage = collection.mutable.Map[String, AuthInfo[Long]]()

  /**
   * Emulates the database.
   */
  val db = Map(1l -> ("Ivan", "12345"), 2l -> ("John", "password"))

  // Client Credentials Flow Scheme Implementation goes here
  def validateClient(clientId: String, clientSecret: String, grantType: String) =
    Future.value(true)

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]) = {
    val user = db.find {
      case (_, (id, secret)) => (id == clientId) && (secret == clientSecret)
    } flatMap {
      case (id, _) => Some(id)
    }

    Future.value(user)
  }

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
  // unsupported scheme.
  def findAuthInfoByCode(code: String) = Future.None
  def findUser(username: String, password: String) = Future.None
  def getStoredAccessToken(authInfo: AuthInfo[Long]) = Future.None
  def findAuthInfoByRefreshToken(refreshToken: String) = Future.None
  def refreshAccessToken(authInfo: AuthInfo[Long], refreshToken: String) = Future.never
}

```

#### Define a service that issues tokens
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

#### Define a protected serivice
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

#### Test services
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

