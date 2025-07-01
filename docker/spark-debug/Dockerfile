FROM bitnami/spark:3.5.3

USER root

RUN apt-get update && apt-get install -y iputils-ping netcat && apt-get clean

USER 1001
