package application

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

trait SparkCommons {
  lazy val conf = {
    new SparkConf(false)
      .setMaster("local[*]")
      .setAppName("tariffPrediction")
      .set("spark.logConf", "true")
  }

  lazy val sparkContext = SparkContext.getOrCreate(conf)
  lazy val sqlContext = new SQLContext(sparkContext)
}
