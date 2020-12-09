# HNV Backend

Used in [Hollywood Network Visualizer](https://github.com/stevenliatti/hollywood-network-visualizer).

Hollywood Network Visualizer backend, query Neo4j database to serve JSON Cytoscape.js compatible content via API endpoints. Done in Scala with Akka HTTP. API doc available in OpenAPI format in `apidoc.yaml`. `Dockerfile` to build the backend in a `.jar` and serve it.

You have to deploy your Neo4j instance and define an `.env` file like this :

```conf
NEO4J_HOST=bolt://localhost:7687
BACKEND_INTERFACE=localhost
BACKEND_PORT=8080
```
