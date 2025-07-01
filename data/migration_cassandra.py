from cassandra.cluster import Cluster
from pymongo import MongoClient
import socket
import time
import sys

print("Esperando a que Cassandra esté disponible en cassandra:9042...")
for i in range(30):  # 30 intentos con 2s de espera = 60s total
    try:
        with socket.create_connection(("cassandra", 9042), timeout=2):
            print("Cassandra está disponible.")
            break
    except OSError:
        print(f"Intento {i+1}/30 fallido. Reintentando en 2s...")
        time.sleep(2)
else:
    print("No se pudo conectar a Cassandra después de varios intentos. Abortando.")
    sys.exit(1)

# Conexión a Cassandra
cluster = Cluster(['cassandra'])
session = cluster.connect()

print("Creando keyspace 'flights' si no existe...")
session.execute("""
    CREATE KEYSPACE IF NOT EXISTS flights 
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
""")
session.set_keyspace("flights")

print("Creando tabla 'origin_dest_distances' si no existe...")
session.execute("""
    CREATE TABLE IF NOT EXISTS origin_dest_distances (
        origin TEXT,
        dest TEXT,
        miles DOUBLE,
        PRIMARY KEY (origin, dest)
    );
""")

# Conexión a MongoDB
print("Conectando a MongoDB...")
client = MongoClient("mongodb://mongo:27017")
mongo_collection = client["agile_data_science"]["origin_dest_distances"]

print("Migrando documentos desde Mongo a Cassandra...")
migrated = 0
for doc in mongo_collection.find():
    origin = doc.get("Origin")
    dest = doc.get("Dest")
    miles = float(doc.get("Distance"))
    session.execute(
        "INSERT INTO origin_dest_distances (origin, dest, miles) VALUES (%s, %s, %s)",
        (origin, dest, miles)
    )
    migrated += 1

print(f"Migración completada: {migrated} documentos insertados.")
