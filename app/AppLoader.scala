import conf.ClusterConfig
import play.api.{ApplicationLoader, Configuration}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}

class AppLoader extends GuiceApplicationLoader {

  override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
    val classLoader = context.environment.classLoader
    val configuration = Configuration(ClusterConfig.loadConfig(classLoader))

    initialBuilder
      .in(context.environment)
      .loadConfig(context.initialConfiguration ++ configuration)
      .overrides(overrides(context): _*)
  }

}