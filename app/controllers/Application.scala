package controllers

import javax.inject.Inject

import play.api.mvc._
import services.CounterComponent

class Application @Inject() (counterComponent: CounterComponent) extends Controller {

  def index() = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}