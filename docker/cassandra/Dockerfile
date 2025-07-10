FROM cassandra:4.1

RUN apt-get update && apt-get install -y python3 python3-pip
RUN pip3 install pymongo cassandra-driver

COPY ./data /app/data
WORKDIR /app

CMD ["python3", "/app/data/migration_cassandra.py"]

