package preprocessing

import utils._

import org.apache.spark.sql.{DataFrame}
import org.apache.spark.ml.linalg.DenseMatrix

object BoschDataset extends DatasetProperty {

  override val delimiter = ";"

  override def getTargetColumnNames(): Array[String] = {
    return Array("Response")
  }

}

case class BoschDataset(
    inputPath: Option[String] = None,
    inputData: Option[DataFrame] = None
) extends Dataset(inputPath, inputData) {

  override type T = BoschDataset

  override def property = BoschDataset

  override def preprocess(): BoschDataset = {
    val funcs = Seq(
      Preprocessor.takeSubset(_: DataFrame, p = 0.6)
    )
    return new BoschDataset(inputData = Some(funcs.foldLeft(data) { (r, f) =>
      f(r)
    }))
  }

  def preprocessCommon(): BoschDataset = {
    val funcs = Seq(
      Preprocessor.takeSubset(_: DataFrame, p = 0.6),
      Preprocessor.dropColumns(_: DataFrame, "Response"),
      Preprocessor.dropNullColumns(_: DataFrame),
      Preprocessor.dropConstantColumns(_: DataFrame)
    )
    return new BoschDataset(inputData = Some(funcs.foldLeft(data) { (r, f) =>
      f(r)
    }))
  }

  def preprocessForClustering(): Tuple2[BoschDataset, DenseMatrix] = {
    val x = Preprocessor.binaryConversion(data, exclude = Array("Id"))
    return Preprocessor.pca(
      x,
      maxComponents = 50,
      assembleFeatures = true,
      explainedVariance = 0.95,
      exclude = Array("Id")
    )
    return (new BoschDataset(inputData = z), pc)
  }

}
