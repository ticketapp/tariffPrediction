package application

import javax.inject.Inject

import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import org.apache.spark.rdd.RDD

class TrainModel @Inject() (extractData: ExtractData) extends SparkCommons {
  def trainModel(labeledPoints: RDD[LabeledPoint]): DecisionTreeModel = {
    val impurity = "variance"
    val maxDepth = 10
    val maxBins = 32
    val categoricalFeaturesInfo = Map[Int, Int]()

    DecisionTree.trainRegressor(labeledPoints, categoricalFeaturesInfo, impurity, maxDepth, maxBins)
  }
}
