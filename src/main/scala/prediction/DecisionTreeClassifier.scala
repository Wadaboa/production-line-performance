package prediction

import preprocessing.{Dataset}
import utils._

import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.tuning.ParamGridBuilder
import org.apache.spark.ml.classification.{DecisionTreeClassifier => DT}

case class DecisionTreeClassifier(dataset: Dataset)
    extends Predictor[DT](dataset) {

  var impurity: String = "entropy"
  var maxDepth: Int = 30
  var maxBins: Int = 64
  var minSamplesLeaf: Int = 10

  var model = new DT()
    .setImpurity(impurity)
    .setMaxDepth(maxDepth)
    .setMaxBins(maxBins)
    .setMinInstancesPerNode(minSamplesLeaf)
    .setSeed(Utils.seed)
    .setLabelCol(labelCol)
    .setFeaturesCol(featuresCol)
    .setPredictionCol(predictionCol)

  override def paramGrid: Array[ParamMap] = {
    return new ParamGridBuilder()
      .addGrid(model.maxDepth, (1 to maxDepth by 5).toArray)
      .addGrid(model.minInstancesPerNode, (1 to minSamplesLeaf by 2).toArray)
      .build()
  }

}
