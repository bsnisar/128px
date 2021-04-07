package modules

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.Materializer
import backend.{BackendClient, MLOpsClient, MLOpsClientImpl, VespaBackendClient}
import com.google.inject.{AbstractModule, Provides}
import mlops.{ImageEmbeddings, ImageEmbeddingsDJL}
import play.api.Configuration
import vespa.{ImgAssets, ImgAssetsDefault, PhotoStock}

//noinspection TypeAnnotation
class AppModule extends AbstractModule {
  override def configure(): Unit = {
     bind(classOf[PhotoStock])
     bind(classOf[BackendClient]).to(classOf[VespaBackendClient])
     bind(classOf[MLOpsClient]).to(classOf[MLOpsClientImpl])
  }

  @Provides
  def assets(m: Materializer, as: ActorSystem): ImgAssets = {
    new ImgAssetsDefault()(m,as)
  }

  @Provides
  def gatewayLocal(config: Configuration): ImageEmbeddings = {
    ImageEmbeddingsDJL(Paths.get(config.get[String]("djl.model-path")))
  }

}
