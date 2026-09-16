![Kubernetes](https://img.shields.io/badge/Kubernetes-v1.34+-326CE5?logo=kubernetes&logoColor=white)
![Apache Flink](https://img.shields.io/badge/Apache%20Flink-1.20-E6526F?logo=apacheflink&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-4.x-231F20?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-29.x-2496ED?logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-v2-2496ED?logo=docker&logoColor=white)
![Kind](https://img.shields.io/badge/Kind-Local%20Kubernetes-0094F5?logo=kubernetes&logoColor=white)

## Running Flink SQL Batch as a Kubernetes Job

This project demonstrates how to run a Flink SQL batch job on Kubernetes as a scheduled Kubernetes CronJob.

The example assumes a database table contains transaction data. Every day at 1:00 AM, the Kubernetes CronJob launches a Flink application that processes the previous day's transactions and produces a daily summary to Kafka.

#### Architecture Diagram

<img width="864" height="456" alt="prod excalidraw" src="https://github.com/user-attachments/assets/49b751d8-651a-422c-a1ff-3699ebaa19aa" />



#### Verify Docker Gateway

The Flink pods need to communicate with services running on the Docker host.

First, find the Docker bridge gateway:
```sh
docker network inspect bridge --format '{{(index .IPAM.Config 0).Gateway}}'
```
Use the gateway returned by your environment in the Kubernetes network test below.

#### Test Network Connectivity from Kubernetes

Before deploying Flink, verify that a Kubernetes pod can reach PostgreSQL and Kafka on the Docker host.

Run:

```sh

kubectl run flink-network-test \
  -n flink \
  --rm -it \
  --restart=Never \
  --image=curlimages/curl \
  --overrides='
{
  "spec": {
    "hostAliases": [
      {
        "ip": "172.17.0.1",
        "hostnames": ["host.docker.internal"]
      }
    ]
  }
}' \
  -- sh

```

> Replace 172.17.0.1 with the Docker gateway returned by the previous command if it is different.

Once inside the pod, verify that the hostname resolves:

```sh
# 1. Check that hostAliases worked
cat /etc/hosts | grep host.docker.internal

# 2. Test connectivity
nc -vz host.docker.internal 54321   # Postgres
nc -vz host.docker.internal 9092    # Kafka
```
A successful connection confirms that the Kubernetes network can reach the required services.

Exit the test pod:

```sh
exit
```
#### Deploy the Kubernetes Resources

The k8s/ directory contains the Kubernetes configuration required to run the batch job.

Apply the resources:

```sh
kubectl apply -f k8s/flink-rbac.yaml
kubectl apply -f k8s/sql-configmap.yaml
kubectl apply -f k8s/pod-template-configmap.yaml
kubectl apply -f k8s/cronjob.yaml
```

#### Verify the CronJob

Check the CronJob:

```sh
kubectl get cronjob -n flink
```

#### Run the Batch Job Manually

Create a one-off Kubernetes Job from the CronJob:

```sh
kubectl create job \
  --from=cronjob/flink-sql-batch \
  flink-sql-batch-test \
  -n flink
```
This creates a normal Kubernetes Job using the same configuration that the scheduled CronJob will use.

#### Monitor the Job

Watch the pods:

```sh
kubectl get pods -n flink -w
```
You should see the launcher pod and, when the Flink application starts, the Flink application pod.

Check the CronJob:

```sh
kubectl get cronjob -n flink

```
Check ConfigMaps:
```sh
kubectl get configmaps -n flink
```
You can also inspect all resources:
```sh
kubectl get all -n flink
```
#### Check Job Logs

To view the logs from the Kubernetes Job:
```sh
kubectl logs job/flink-sql-batch-test -n flink
```
The launcher logs should show the processing date and Flink cluster ID:

#### Monitor the Flink Application
List the Flink deployments:

```sh
kubectl get deployments -n flink
```

List the Flink pods:
```sh
kubectl get pods -n flink
```

#### Verify the Output in Kafka

Kafka UI is available at:

```sh
http://localhost:8090
```

Open Kafka UI and inspect the output topic to verify that the Flink batch job produced the expected daily transaction summary.



#### Verify PostgreSQL with Adminer

Adminer is available at:

```sh
http://localhost:8080
```

Use Adminer to inspect the source transaction data and verify that the records being processed by Flink exist in PostgreSQL.


#### Access the Flink UI

First, list the services:

```sh
kubectl get svc -n flink
```

Find the REST service for the Flink application. It will typically have a name similar to: transactions-<cluster-id>-rest

Then port-forward the Flink REST service:

```sh
kubectl port-forward \
  -n flink \
  svc/transactions-<cluster-id>-rest \
  8085:8081
```

Open the Flink UI at:

```sh
http://localhost:8085
```





