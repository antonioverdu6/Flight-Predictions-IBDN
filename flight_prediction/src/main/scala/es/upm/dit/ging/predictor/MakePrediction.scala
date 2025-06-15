package es.upm.dit.ging.predictor

import com.mongodb.spark._
import org.apache.spark.ml.classification.RandomForestClassificationModel
import org.apache.spark.ml.feature.{Bucketizer, StringIndexerModel, VectorAssembler}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DataTypes, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery}

object MakePrediction {

  def main(args: Array[String]): Unit = {
    println("Flight predictor starting...")

    val spark = SparkSession
      .builder
      .appName("StructuredNetworkWordCount")
      .master("spark://spark-master:7077")
      .config("spark.hadoop.fs.defaultFS", "hdfs://hadoop-namenode:8020")
      .config("spark.mongodb.output.uri", "mongodb://mongo:27017/agile_data_science.flight-predictions-output")
      .getOrCreate()
    import spark.implicits._

    spark.sparkContext.setLogLevel("WARN")

    val base_path = "/app"
    val arrivalBucketizer = Bucketizer.load(s"$base_path/models/arrival_bucketizer_2.0.bin")
    val columns = Seq("Carrier", "Origin", "Dest", "Route")
    val stringIndexerModels = columns.map { col =>
      col -> StringIndexerModel.load(s"$base_path/models/string_indexer_model_${col}.bin")
    }.toMap
    val vectorAssembler = VectorAssembler.load(s"$base_path/models/numeric_vector_assembler.bin")
    val rfc = RandomForestClassificationModel.load(s"$base_path/models/spark_random_forest_classifier.flight_delays.5.0.bin")

    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:9092")
      .option("subscribe", "flight-delay-ml-request")
      .option("startingOffsets", "earliest")
      .load()

    val flightJsonDf = df.selectExpr("CAST(value AS STRING)")

    val schema = new StructType()
      .add("Origin", DataTypes.StringType)
      .add("FlightNum", DataTypes.StringType)
      .add("DayOfWeek", DataTypes.IntegerType)
      .add("DayOfYear", DataTypes.IntegerType)
      .add("DayOfMonth", DataTypes.IntegerType)
      .add("Dest", DataTypes.StringType)
      .add("DepDelay", DataTypes.DoubleType)
      .add("Timestamp", DataTypes.TimestampType)
      .add("FlightDate", DataTypes.DateType)
      .add("Carrier", DataTypes.StringType)
      .add("UUID", DataTypes.StringType)
      .add("Distance", DataTypes.DoubleType)
      .add("Carrier_index", DataTypes.DoubleType)
      .add("Origin_index", DataTypes.DoubleType)
      .add("Dest_index", DataTypes.DoubleType)
      .add("Route_index", DataTypes.DoubleType)

    val flightNestedDf = flightJsonDf.select(from_json($"value", schema).as("flight"))
    val flightFlattenedDf = flightNestedDf.selectExpr(
      "flight.Origin",
      "flight.DayOfWeek",
      "flight.DayOfYear",
      "flight.DayOfMonth",
      "flight.Dest",
      "flight.DepDelay",
      "flight.Timestamp",
      "flight.FlightDate",
      "flight.Carrier",
      "flight.UUID",
      "flight.Distance"
    )

    val predictionRequestsWithRouteMod = flightFlattenedDf.withColumn(
      "Route",
      concat($"Origin", lit("-"), $"Dest")
    )

    var indexedData = predictionRequestsWithRouteMod
    for ((colName, indexerModel) <- stringIndexerModels) {
      indexedData = indexerModel.transform(indexedData)
    }

    val dataForVectorAssembler = indexedData.select(
      $"Origin", $"DayOfWeek", $"DayOfYear", $"DayOfMonth", $"Dest",
      $"DepDelay", $"Timestamp", $"FlightDate", $"Carrier", $"UUID",
      $"Distance", $"Carrier_index", $"Origin_index", $"Dest_index", $"Route_index"
    )

    val vectorizedFeatures = vectorAssembler.setHandleInvalid("keep").transform(dataForVectorAssembler)

    val finalVectorizedFeatures = vectorizedFeatures
      .drop("Carrier_index", "Origin_index", "Dest_index", "Route_index", "Route")

    val predictions = rfc.transform(finalVectorizedFeatures)
      .drop("features", "rawPrediction", "probability")


    val finalPredictions = predictions
     .drop("indices")
     .drop("values")
     .drop("rawPrediction")
     .drop("probability")

    // Inspect the output
    finalPredictions.printSchema()

    // Define a streaming query
    val mongoOutput = finalPredictions.writeStream
     .format("mongodb")
     .option("spark.mongodb.connection.uri", "mongodb://mongo:27017")
     .option("spark.mongodb.database", "agile_data_science")
     .option("spark.mongodb.collection", "flight-predictions-output")
     .option("checkpointLocation", "/tmp/mongo_checkpoint")
     .outputMode("append")
     .start()
     
    // Define a streaming query for HDFS output (new configuration)
    val hdfsOutput = finalPredictions
     .writeStream
     .format("parquet")
     .option("path", "hdfs://hadoop-namenode:8020/user/spark/flight_prediction")  // Ruta en HDFS
     .option("checkpointLocation", "/tmp/hdfs_checkpoint")
     .outputMode("append")
     .start()

    // Define a streaming query for Kafka output
    val kafkaOutput = finalPredictions
     .selectExpr("CAST(UUID AS STRING) as key", "to_json(struct(*)) AS value")
     .writeStream
     .format("kafka")
     .option("kafka.bootstrap.servers", "kafka:9092")
     .option("topic", "flight-predictions-output")
     .option("checkpointLocation", "/tmp/kafka_checkpoint")
     .outputMode("append")
     .start()

   // Define a streaming query for console output (debugging)
    val consoleOutput = finalPredictions
     .writeStream
     .outputMode("append")
     .format("console")
     .start()

   // Await termination for all streams
    mongoOutput.awaitTermination()
    hdfsOutput.awaitTermination()
    kafkaOutput.awaitTermination()
    consoleOutput.awaitTermination()
  }
}
