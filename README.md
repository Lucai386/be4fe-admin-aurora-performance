# be4fe-admin-aurora-performance

BFF (Backend For Frontend) per il frontend admin — proxy autenticazione e chiamate al core.

## Start (locale)
```bash
docker compose up -d
```

## Deploy su GCP Cloud

### Prerequisiti
```bash
gcloud auth configure-docker europe-west8-docker.pkg.dev
gcloud container clusters get-credentials aurora-cluster --region europe-west8 --project aurora-perf-prod
```

### 1. Build JAR
```bash
./mvnw package -DskipTests -B
```

### 2. Build immagine Docker
```bash
docker build -t europe-west8-docker.pkg.dev/aurora-perf-prod/aurora-docker/bff-admin:latest .
```

### 3. Push su Artifact Registry
```bash
docker push europe-west8-docker.pkg.dev/aurora-perf-prod/aurora-docker/bff-admin:latest
```

### 4. Deploy su GKE
```bash
kubectl rollout restart deployment/bff-admin -n aurora
kubectl rollout status deployment/bff-admin -n aurora
```

> **Manifest K8s**: `k8s/30-bff-admin.yaml` — porta 8083
