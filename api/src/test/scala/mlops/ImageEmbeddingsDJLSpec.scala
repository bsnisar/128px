package mlops

import java.net.URL
import java.nio.file.Paths

import org.specs2.mutable._

class ImageEmbeddingsDJLSpec extends Specification {
  "The service " should {
    "launch a model" in {
      val sut = ImageEmbeddingsDJL(
        Paths.get("/Users/bohdans/Downloads/model.run_2021-04-05T20_03_24.273796.full.jit.zip")
      )
      val r = sut.image(CalculateEmbeddingRequest(
        new URL("file:///Users/bohdans/.stash/caches/shard_000000001/o_img-811e7090ab4f67745822284a756c4807/index.jpeg")
      ))

      println(r.get)

      r.get must_!== null
    }
  }
}
