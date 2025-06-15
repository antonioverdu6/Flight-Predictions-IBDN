#!/bin/bash
echo "Esperando a que MongoDB inicie..."
sleep 5

echo "Importando datos..."
mongoimport --host mongo \
  --db agile_data_science \
  --collection origin_dest_distances \
  --file /init/origin_dest_distances.jsonl \
  --type json

