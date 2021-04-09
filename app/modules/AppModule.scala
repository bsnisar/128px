package modules

import java.nio.file.Paths

import backend.{BackendClient, MLOpsClient, MLOpsClientImpl, VespaBackendClient}
import com.google.inject.{AbstractModule, Provides}
import mlops.{ImageEmbeddings, ImageEmbeddingsDJL}
import photos.assets.{IPartitioner, ImgAssets, ImgAssetsDefault, PartitionerDefault}
import play.api.Configuration

//noinspection TypeAnnotation
class AppModule extends AbstractModule {
  override def configure(): Unit = {
//     bind(classOf[PhotoStock])
     bind(classOf[BackendClient]).to(classOf[VespaBackendClient])
     bind(classOf[MLOpsClient]).to(classOf[MLOpsClientImpl])
     bind(classOf[ImgAssets]).to(classOf[ImgAssetsDefault])
     bind(classOf[IPartitioner]).to(classOf[PartitionerDefault])
  }

  @Provides
  def gatewayLocal(config: Configuration): ImageEmbeddings = {
    ImageEmbeddingsDJL(Paths.get(config.get[String]("djl.model-path")))
  }

}
