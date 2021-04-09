![Img](public/images/logo.png)

#### Visual Search Application | PoC | Yet another photo stock 

This application demonstrate a Visual Search fronted app. 

###### Vespa install

First download vespa container 
```
docker run -m 10G --detach --name vespa --hostname vespa-db \
    --volume `pwd`:/app --publish 8080:8080 --publish 19092:19092 vespaengine/vespa
```

And install [application package](https://docs.vespa.ai/documentation/cloudconfig/application-packages.html) 

```
$ cd ./db
$ ./mvnw clean package && \
docker exec vespa bash -c '/opt/vespa/bin/vespa-deploy prepare /app/target/application.zip && \
    /opt/vespa/bin/vespa-deploy activate'
```

###### Vespa schema

| field | usage |
|---|---|
| imgEmbedding | Image embedding with hnsw index |
| keywords | map of image tag to struc  |
| guid | id of image  |
| metadata | read only metadata as encoded json  |

Example:

```
document stash {

        field guid type string {
            indexing: summary | attribute
            rank: filter
        }

        struct keyword {
            field tag type string {}
            field suggestedByUser type int {}
            field aiServiceConfidence1 type float {}
        }

        field keywords type map<string, keyword> {
            indexing: summary
            struct-field key { indexing: index | attribute }
        }

        field metadata type raw {
          indexing: summary
        }

        field imgEmbedding type tensor<float>(x[128]) {
          indexing: summary | attribute | index
          attribute {
            distance-metric: angular
          }
          index {
            hnsw {
              max-links-per-node: 24
              neighbors-to-explore-at-insert: 500
            }
          }
        }

    }
```

###### features supported

## [Demo Video](https://youtu.be/YkouLHEymM4)

1. Visual search by already indexed photo. During rendering a page about particular photo, fetch it's embedding and 
construct [ANN](https://blog.vespa.ai/using-approximate-nearest-neighbor-search-in-real-world-applications/) query for similar images 

1.  Visual search by example that doesn't exists in the db. By provided url download image, preprocess, evaluate inference to obtain the embedding and perform the 
same search as for feature #1
