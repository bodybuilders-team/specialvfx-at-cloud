docker kill local-dynamodb || true
docker rm local-dynamodb || true
docker run -d -p 8010:8000 --name local-dynamodb amazon/dynamodb-local