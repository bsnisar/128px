

## Docker

```
docker run -m 10G --detach --name vespa --hostname vespa-db \
    --volume `pwd`:/app --publish 8080:8080 --publish 19092:19092 vespaengine/vespa
```


## Install

```
docker exec vespa bash -c '/opt/vespa/bin/vespa-deploy prepare /app/target/application.zip && \
    /opt/vespa/bin/vespa-deploy activate'
```


## Status

```
docker exec vespa bash -c 'curl -s --head http://localhost:19071/ApplicationStatus'
curl -L 'http://localhost:8080/ApplicationStatus'
```