package prediction

import preprocessing.{Dataset}

import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.{
  Evaluator,
  MulticlassClassificationEvaluator
}
import org.apache.spark.ml.classification.{DecisionTreeClassifier => DT}

class DecisionTreeClassifier(dataset: Dataset) extends Predictor(dataset) {

  var impurity: String = "entropy"
  var maxDepth: Int = 10
  var maxBins: Int = 64

  override type T = DT

  override def getModel(): DT = {
    return new DT()
      .setImpurity(impurity)
      .setMaxDepth(maxDepth)
      .setMaxBins(maxBins)
      .setSeed(getRandomSeed())
      .setLabelCol(labelCol)
      .setFeaturesCol(featuresCol)
      .setPredictionCol(predictionCol)
  }

  override def getEvaluator(): Evaluator = {
    return new MulticlassClassificationEvaluator()
      .setLabelCol(labelCol)
      .setPredictionCol(predictionCol)
      .setMetricName(metricName)
  }

}
