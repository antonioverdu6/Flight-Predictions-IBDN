# Flight Predictions - IBDN
---
## Introducción
Proyecto de predicción de retrasos de vuelos utilizando diferentes tecnologías en la nube, con el objetivo de calcular dichos retrasos en un FrontEnd básico con varios valores a tener en cuenta en las predicciones.

---
## Despliegue
Para desplegar el proyecto de forma local, simplemente sigue los siguientes pasos:

### Clonar el repositorio

<pre>git clone https://github.com/antonioverdu6/Flight-Predictions-IBDN.git practica_creativa-master</pre>
Una vez clonado, nos dirigiremos a la carpeta donde tenemos el *docker-compose.yml*.
<pre>cd practica_creativa-master</pre>
Tras acceder a esta, desplegaremos nuestros contenedores utilizando el servicio de Docker tal que:
<pre>
docker compose up --build
</pre>
Ahora esperaremos un par de minutos para que se desplieguen de manera correcta todas las imágenes. Podremos ir comprobando su funcionamiento mediante el comando:
<pre>docker compose ps -a</pre>

Para frenar todo el despliegue deberemos escribir *Ctrl + C*, y si queremos bajar los contenedores realizaremos el comando:
<pre>docker compose down -v</pre>

## Servicios Desplegados
Utilizamos diferentes tecnologías, de manera que todo queda automatizado gracias a Docker, estos servicios son:

### MongoDB
Este servicio funciona como una *BBDD* sobre la cual guardamos todos los datos creados en las predicciones de *Spark*, dentro de este crearemos una base de datos llamada **agile_data_science**, que dentro de este tendrá dos tablas cuyos nombres serán **origin_dest_distances** y **flight_delay_ml_response**.
El primero almacena los datos que nos llegan desde Spark, y el segundo sirve para almacenar las predicciones.
- Para poder comprobar el corrrecto funcionamiento de este deberemos entrar a mongosh y comprobar que se añaden nuestras peticiones y predicciones.

Primero accedemos al contenedor de mongo:
<pre> docker exec -it mongo mongosh</pre>
Una vez dentro, comprobamos que se ha creado la colección y accedemos a ella:
<pre>show dbs 
use agile_data_science</pre>
Una vez dentro, comprobamos también que se han creado las colecciones:
<pre>show collections
db.flight_delay_ml_response.find()
</pre>

### Flask
Encargado de trasnformar las peticiones en predicciones mediante tópicos, los cuales son referidos como **flight-delay-ml-request** y **flight-predictions-output**.  
Para calcular las predicciones hacemos uso de *predict_flask.py*, el cual es un archivo que dado un tópico de entrada (**flight-delay-ml-request**) calcula las predicciones para crear el tópico de salida (**flight-predictions-output**).

- **Ahora es obligatorio el uso de WebSockets** en la aplicación Flask para capturar la respuesta de los tópicos de Kafka en tiempo real. Esto permite mostrar las predicciones directamente al usuario en el frontend sin necesidad de recargar la página.
- Comprobamos su funcionamiento si, al estar dentro de [http://localhost:5001/flights/delays/predict_kafka](http://localhost:5001/flights/delays/predict_kafka) y realizar un submit, recibimos la predicción en tiempo real mediante WebSockets.

### Spark
El sistema de predicciones recibe las peticiones enviadas desde *Flask* a través de *Kafka*, procesa los datos mediante un modelo entrenado con **Spark MLlib** y devuelve los resultados. Las predicciones **se almacenan en MongoDB, HDFS y en un tópico de Kafka** para ser consumidas por otros servicios. El motor de predicción está implementado en *Scala* y compilado con *sbt*.
- En esta práctica se nos pedía específicamente tener dos workers y un master, los cuales podremos ver en nuestro [http://locahost:8080](http://localhost:8080), donde nos fijaremos si estos están en funcionamiento y utilizando memoria.

### Kafka
Diseñado para mantener el flujo de datos entre los mensajes de los tópicos que utiliza *Flask*, conectándolo, como vimos antes, con Spark.
- Para comprobar el funcionamiento de Kafka, entraremos en su contenedor y **crearemos consumers** para visualizar los mensajes.

Entramos al contenedor de Kafka:
<pre>docker exec -it kafka bash
cd bin</pre>
Ejecutamos el comando para ver el tópico de **flight-delay-ml-request** y esperamos a que estos se impriman (para cerrar utilizaremos *Ctlr + C*):
<pre>kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --topic flight-delay-ml-request \
    --from-beginning</pre>
También realizaremos lo mismo para **flight-predictions-output**:
<pre>kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --topic flight-predictions-output\
    --from-beginning</pre>

### NiFi
Utilizado para la creación de un flujo de datos y orquestar nuestro proceso crear las predicciones con nuestro *consumer de Kafka*. Guarda las **predicciones** en un fihero .txt.
Contiene dos procesadores, **uno para el consumer** y el otro para **guardar las predicciones**.
- Comprobamos que funciona al entrar a [http://locahost:9443](http://localhost:9443), donde cargamos nuestro flujo guardado en las carpetas con nombre **flujo_nifi.xml** y observamos como funciona.

### HDFS
Este servicio almacena las predicciones generadas en formato CSV. Está compuesto por un **Namenode**, que gestiona la organización y el acceso a los datos, y un **Datanode**, encargado de almacenar físicamente los archivos.
- En este último servicio deberemos acceder a [http://locahost:9870](http://localhost:9870), dentro tendremos un datanode impreso en pantalla, que guardará las predicciones. Para acceder a todas las predicciones realizadas realizaremos los siguientes movimientos en la página web:
  - Arriba a la derecha clickaremos sobre **utilities** y tras eso iremos seleccionando con la ruta **user/spark/predictions**, llegando al histórico de predicciones.

### Cassandra
Se ha añadido **Cassandra** como nueva base de datos para alojar los datos de distancias entre aeropuertos.  
El *job* de predicción que antes consultaba los datos desde MongoDB, ahora lo hace desde **Cassandra**, por lo que ha sido necesario realizar una **migración de datos**.

- Los datos que estaban almacenados en la colección **origin_dest_distances** de Mongo han sido migrados a Cassandra utilizando una herramienta de transformación, partiendo de dicha colección o desde el fichero original de distancias.
- En Cassandra, los datos se alojan en una tabla con una estructura compatible con el job de predicción de Spark.
- Este cambio permite una mayor eficiencia en la consulta y escalabilidad del sistema de predicción.

Para acceder al contenedor de Cassandra y verificar los datos migrados:

<pre>docker exec -it cassandra cqlsh</pre>

Una vez dentro, puedes listar los keyspaces y consultar la tabla correspondiente:

<pre>
DESCRIBE KEYSPACES;
USE flight_data;
SELECT * FROM origin_dest_distances;
</pre>
---
**Agradecimientos**

Gracias por visitar este repositorio.

Proyecto desarrollado por:

- [Antonio Verdú Salpico](https://www.linkedin.com/in/antonio-verdu-salpico/)
