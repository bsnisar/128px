package mlops

import java.nio.file.Path

import ai.djl.repository.SimpleRepository

class RepositoryZipFile(name: String, path: Path,
                        artifactId: String, modelName: String)
  extends SimpleRepository(name, path, artifactId, modelName) {

}
