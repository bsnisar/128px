package mlops

import scala.util.Try

trait ImageEmbeddings {

  def image(r: CalculateEmbeddingRequest): Try[CalculateEmbeddingResponse]
}
