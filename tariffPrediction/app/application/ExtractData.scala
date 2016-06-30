package application

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SQLContext}

class ExtractData extends SparkCommons {
  val databaseURL = "jdbc:postgresql://dbHost:5432/ticketapp?user=simon&password=root"

  def extractRddLabeledPoints(): RDD[LabeledPoint] = {
    val labeledPoints = extractLabeledPoints(extractDataFrame().cache()).toSeq

    sparkContext.parallelize[LabeledPoint](labeledPoints)
  }

  private def extractDataFrame(): DataFrame = sqlContext.read.format("jdbc").options(
    Map("url" -> databaseURL,
      "dbtable" ->
        """(
        SELECT events.event_facebook_id, events.tariffrange,
          eventscounts.attending_count, eventscounts.declined_count, eventscounts.interested_count,
          eventscounts.maybe_count, eventscounts.noreply_count,
          organizers.organizerid, organizers.likes as organizerlikes,
          places.placeid, places.capacity, places.likes as placelikes,
          artists.facebookId, artists.likes as artistlikes
          FROM events
        LEFT JOIN eventscounts on eventscounts.event_facebook_id = events.event_facebook_id
        LEFT JOIN eventsorganizers on eventsorganizers.event_id = events.event_facebook_id
          LEFT JOIN organizers on eventsorganizers.organizerUrl = organizers.facebookUrl
        LEFT JOIN eventsartists on eventsartists.event_id = events.event_facebook_id
          LEFT JOIN artists on eventsartists.artistId = artists.facebookid
        LEFT JOIN eventsplaces on eventsplaces.event_id = events.event_facebook_id
          LEFT JOIN places on eventsplaces.placeFacebookUrl = places.facebookUrl
        WHERE events.tariffrange is not null) df""")
  ).load()

  private def extractLabeledPoints(df: DataFrame): Iterable[LabeledPoint] = groupEvents(df).flatMap { event =>
    val eventRows = event._2
    val tariffRange: String = extractTariffRange(event, eventRows)

    val maxTariff = extractMaxTariff(tariffRange)
    if (maxTariff >= 180 || maxTariff <= 2) {
      None
    } else {
      val artistLikesAverage = extractArtistLikesAverage(eventRows)
      val organizerLikesAverage = extractLikesAverage(eventRows, "organizer")
      val placeLikesAverage = extractLikesAverage(eventRows, "place")

      val firstEventRow = eventRows.head
      val placeCapacity = extractPlaceCapacity(firstEventRow)
      val attendingCount = firstEventRow.getInt(firstEventRow.fieldIndex("attending_count"))
      val declinedCount = firstEventRow.getInt(firstEventRow.fieldIndex("declined_count"))
      val interestedCount = firstEventRow.getInt(firstEventRow.fieldIndex("interested_count"))
      val maybeCount = firstEventRow.getInt(firstEventRow.fieldIndex("maybe_count"))
      val noreplyCount = firstEventRow.getInt(firstEventRow.fieldIndex("noreply_count"))

      val features = Vectors.dense(
        artistLikesAverage,
        organizerLikesAverage,
        attendingCount,
        declinedCount,
        interestedCount,
        maybeCount,
        noreplyCount,
        placeCapacity,
        placeLikesAverage)

      Option(LabeledPoint(maxTariff, features))
    }
  }

  private def extractTariffRange(event: (String, Array[Row]), eventRows: Array[Row]): String =
    eventRows.head.getString(event._2.head.fieldIndex("tariffrange"))

  private def groupEvents(df: DataFrame): Map[String, Array[Row]] = df
    .collect()
    .groupBy(row => row.getString(row.fieldIndex("event_facebook_id")))

  private def extractPlaceCapacity(firstEventRow: Row): Double = {
    val placeCapacityIndex: Int = firstEventRow.fieldIndex("capacity")
    if (firstEventRow.isNullAt(placeCapacityIndex)) 0.0
    else firstEventRow.getInt(placeCapacityIndex).toDouble
  }

  private def extractLikesAverage(eventRows: Array[Row], tableName: String): Double = {
    val idsAndSum = eventRows.foldLeft(Seq.empty[Int], 0.0)((idsAndSumTmp, row) => {
      val idIndex = row.fieldIndex(tableName + "id")
      if (row.isNullAt(idIndex)) {
        idsAndSumTmp
      } else {
        val likesIndex = row.fieldIndex(tableName + "likes")
        if (row.isNullAt(likesIndex)) idsAndSumTmp
        else (idsAndSumTmp._1 :+ row.getInt(idIndex), idsAndSumTmp._2 + row.getInt(likesIndex).toDouble)
      }
    })

    val numberOfLikes = idsAndSum._1.length

    if (numberOfLikes > 0) idsAndSum._2 / numberOfLikes
    else 0
  }

  private def extractArtistLikesAverage(eventRows: Array[Row]): Double = {
    val idsAndSum = eventRows.foldLeft(Seq.empty[String], 0.0)((idsAndSumTmp, row) => {
      val idIndex = row.fieldIndex("facebookid")
      if (row.isNullAt(idIndex)) {
        idsAndSumTmp
      } else {
        val likesIndex = row.fieldIndex("artistlikes")
        if (row.isNullAt(likesIndex)) idsAndSumTmp
        else (idsAndSumTmp._1 :+ row.getString(idIndex), idsAndSumTmp._2 + row.getInt(likesIndex).toDouble)
      }
    })

    val numberOfLikes = idsAndSum._1.length

    if (numberOfLikes > 0) idsAndSum._2 / numberOfLikes
    else 0
  }

  private def extractMaxTariff(tariff: String): Double = {
    val indexOfSeparator = tariff.indexOf("-")
    tariff.drop(indexOfSeparator + 1).toDouble
  }

  private def extractTotalLikesAverage(sQLContext: SQLContext, tableName: String): Long = sqlContext
    .read
    .format("jdbc")
    .options(Map(
      "url" -> databaseURL,
        "dbtable" -> s"""(
          select sum(likes)/(select count(*) from $tableName
            where likes is not null) from artists where likes is not null
        ) df"""))
    .load()
    .collect
    .head
    .getLong(0)
}
