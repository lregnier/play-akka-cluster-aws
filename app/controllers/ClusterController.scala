package controllers

import javax.inject.Inject

import akka.actor.Address
import akka.util.Timeout
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, Controller}
import services.ClusterEventListenerComponent
import akka.pattern.ask
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration._

class ClusterController @Inject() (
    clusterListenerComponent: ClusterEventListenerComponent) extends Controller {

  import services.ClusterEventListener._

  val clusterListener = clusterListenerComponent.clusterListener

  def listClusterNodes() = Action.async {
    implicit val addressWrites = new Writes[Address] {
      def writes(address: Address) = Json.obj(
        "host" -> address.host,
        "port" -> address.port,
        "protocol" -> address.protocol,
        "system" -> address.system
      )
    }

    implicit val timeout = Timeout(5 seconds)
    (clusterListener ? GetClusterNodes).mapTo[Set[Address]].map { addresses =>
      Ok(Json.toJson(addresses))
    }
  }

}
