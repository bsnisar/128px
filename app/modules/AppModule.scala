package modules

import akka.actor.ActorSystem
import akka.stream.Materializer
import backend.{BackendClient, VespaBackendClient}
import com.google.inject.{AbstractModule, Provides}
import vespa.{ImgAssets, ImgAssetsDefault, PhotoStock}

//noinspection TypeAnnotation
class AppModule extends AbstractModule {
  override def configure(): Unit = {
     bind(classOf[PhotoStock])
     bind(classOf[BackendClient]).to(classOf[VespaBackendClient])
  }

  @Provides
  def assets(m: Materializer, as: ActorSystem): ImgAssets = {
    new ImgAssetsDefault()(m,as)
  }
}
