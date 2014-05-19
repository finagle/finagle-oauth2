OAuth2 Server-Side Provider for Finagle
---------------------------------------

# Usage 1

```scala
import com.twitter.finagle.OAuth2
import com.twitter.finagle.oauth2.DataHandler
import com.twitter.finagle.http._

case class User

object LocalDataHandler extends DataHandler[User] {
 ???
}

object JsonTokenSerivice extends Service[Request, Response] with OAuth2 {
  def apply(req: Request) = 
    issueAccessToken(req, LocalDataHandler) flatMap { grant =>
      val reply = Response(Version.Http11, Status.Ok)
      reply.setContentTypeJson()
      reply.setContentString(JSONObject(Map(
        "token_type" -> grant.tokenType,
        "access_token" -> grant.accessToken
      ) ++ grant.expiresIn.map {
        "expires_in" -> _
      } ++ r.refreshToken.map {
        "refresh_token" -> _
      } ++ r.scope.map {
        "scope" -> _
      })))
      
      reply
    } handle {
      case e: OAuthError => oAuthErrorToHttpResponse(e)
    }
}

```
