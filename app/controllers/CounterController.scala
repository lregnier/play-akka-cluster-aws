package controllers

import javax.inject.Inject

import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.CounterComponent

import scala.concurrent.duration._

class CounterController @Inject()(counterComponent: CounterComponent) extends Controller {
  import services.Counter._

  val counter = counterComponent.counter

  def count() = Action.async {
    implicit val timeout = Timeout(5 seconds)
    (counter ? Count).mapTo[Int].map { result =>
      Ok(Json.toJson(result))
    }
  }

}
