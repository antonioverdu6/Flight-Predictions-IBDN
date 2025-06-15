# resources/web/config.py

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS = 'kafka:9092'  # 'kafka' es el nombre del servicio en docker-compose
KAFKA_REQUEST_TOPIC = 'flight-delay-ml-request'
KAFKA_RESPONSE_TOPIC = 'flight-predictions-output'

# Añadir ELASTIC_URL a config.py
ELASTIC_URL = 'http://localhost:9200/agile_data_science'  # URL correcta de Elasticsearch

# MongoDB Configuration
MONGO_URI = 'mongodb://mongo:27017/'  # 'mongo' es el nombre del servicio en docker-compose
MONGO_DB_NAME = 'flight_prediction'
MONGO_COLLECTION_RESULTS = 'flight_delay_results'  # Donde Spark guardaría los resultados
MONGO_COLLECTION_DISTANCES = 'distances'  # Donde se cargarían las distancias iniciales

# Spark Configuration (para referencias si la app Flask necesita algo de Spark UI)
SPARK_MASTER_UI_URL = 'http://spark-master:8080'  # Acceso desde el contenedor Spark
# Para acceder desde el host sería http://localhost:8080

# Flask App Configuration
FLASK_HOST = '0.0.0.0'
FLASK_PORT = 5001

