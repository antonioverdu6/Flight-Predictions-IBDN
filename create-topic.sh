#!/bin/bash
set -e

echo "Esperando a que Kafka esté disponible..."

# Esperar hasta que kafka-topics.sh no falle
while ! kafka-topics.sh --bootstrap-server kafka:9092 --list &>/dev/null; do
  echo "Kafka no está listo, esperando..."
  sleep 2
done

echo "Kafka está listo. Creando tópico..."

kafka-topics.sh --create \
  --bootstrap-server kafka:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic flight-delay-ml-request
  
kafka-topics.sh --create \
  --bootstrap-server kafka:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic flight-predictions-output

echo "Tópico creado con éxito."
