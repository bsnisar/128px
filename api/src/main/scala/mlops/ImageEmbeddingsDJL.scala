package mlops


import java.nio.file.Path

import ai.djl._
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.transform.{CenterCrop, Normalize, Resize, ToTensor}
import ai.djl.modality.cv.translator.BaseImageTranslator
import ai.djl.ndarray.NDList
import ai.djl.translate.{Pipeline, TranslatorContext}

import scala.util.Try

case class ImageEmbeddingsDJL(modelPath: Path) extends ImageEmbeddings {

  private lazy val aModel = {
    val model = Model.newInstance("parastash", Device.defaultDevice())
    model.load(modelPath)
    model
  }
  private lazy val translator = new ImgToTensorTranslator

  override def image(r: CalculateEmbeddingRequest): Try[CalculateEmbeddingResponse] = Try {
    val predictor = aModel.newPredictor(translator)
    val img = ImageFactory.getInstance.fromUrl(r.downloadUrl)
    val output = predictor.predict(img)
    output
  }
}


class ImgToTensorTranslator extends BaseImageTranslator[CalculateEmbeddingResponse](new ImgToTensorTranslator.Builder()) {
  override def processOutput(ctx: TranslatorContext, list: NDList): CalculateEmbeddingResponse = {
    CalculateEmbeddingResponse(list.get(0).toFloatArray)
  }

}

object ImgToTensorTranslator {

  // Use same mean and std normalization as
  // https://github.com/bsnisar/parastash/blob/b717626500eb4bafa202cc2c83616d4257901457/parastash/transforms.py#L11
  val MEAN: Array[Float] = Array(0.5347586f, 0.49634728f, 0.48577875f)
  val STD: Array[Float] = Array(0.34097806f, 0.3337877f, 0.33717933f)

  class Builder extends BaseImageTranslator.BaseBuilder[ImgToTensorTranslator.Builder] {
    if (pipeline == null) {
      pipeline = new Pipeline();
    }

    this.addTransform(new CenterCrop())
    this.addTransform(new Resize(width, height))
    this.addTransform(new ToTensor)

    this.addTransform(new Normalize(MEAN, STD))

    override def self(): ImgToTensorTranslator.Builder = this
  }

}