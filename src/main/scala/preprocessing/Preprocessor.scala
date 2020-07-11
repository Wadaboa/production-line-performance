package preprocessing

import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorIndexer}
import org.apache.spark.ml.feature.{QuantileDiscretizer, Bucketizer}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{trim, when, length, col}


object Preprocessor {

  /** Returns the dataset schema, to be used in reading operations */
  def getSchema(): String = {
    return """
      `age` INT,
      `workclass` STRING,
      `fnlwgt` INT,
      `education` STRING,
      `education-num` INT,
      `marital-status` STRING,
      `occupation` STRING,
      `relationship` STRING,
      `race` STRING,
      `sex` STRING,
      `capital-gain` INT,
      `capital-loss` INT,
      `hours-per-week` INT,
      `native-country` STRING,
      `high-income` STRING
    """
  }

  /** Applies each and every pre-processing function to the given DataFrame */
  def preprocess(data: DataFrame): DataFrame = {
    val funcs = Seq(
      trimValues(_: DataFrame),
      dropColumns(_: DataFrame, "fnlwgt", "education-num"),
      valuesToNull(_: DataFrame, "?"),
      valuesToNull(_: DataFrame, ""),
      dropNullRows(_: DataFrame),
      dropDuplicates(_: DataFrame),
      binning(_: DataFrame, "age", Array(0, 18, 30, 60, 100))
      //binning(_: DataFrame, "hours-per-week", Array(0, 25, 40, 60, 100)),
      //quantileDiscretizer(_: DataFrame, "capital-gain", 10),
      //quantileDiscretizer(_: DataFrame, "capital-loss", 5)
    )
    return funcs.foldLeft(data){ (r, f) => f(r) }
  }

  /** Drops duplicated rows in the DataFrame */
  def dropDuplicates(data: DataFrame): DataFrame = {
    return data.dropDuplicates()
  }

  /** Drops the given list of columns */
  def dropColumns(data: DataFrame, toDrop: String*): DataFrame = {
    return data.drop(toDrop: _*)
  }
  
  /** Drops rows that contain at least one null value */
  def dropNullRows(data: DataFrame): DataFrame = {
    return data.na.drop
  }

  /** Apply the given functions over each and every DataFrame Column */
  def applyOverColumns(data: DataFrame, f: (Column) => Column): DataFrame = {
    return data.columns.foldLeft(data) { 
      (df, c) => df.withColumn(c, f(col(c))) 
    }
  }

  /** Drops rows that contain the given value */
  def removeRowsWithValue(data: DataFrame, value: String): DataFrame = {
    val dfs = data.columns.map(c => {
      data.filter( col(c) !== value )
    })
    return dfs.reduceRight(_ intersect _)
  }

  /** Substitutes values matching the given one to null values in the DataFrame */
  def valuesToNull(data: DataFrame, value: String): DataFrame = {
    return applyOverColumns(
      data, 
      c => when(c.equalTo(value), null).otherwise(c) 
    )
  }

  /** Trims column names and DataFrame values */
  def trimValues(data: DataFrame): DataFrame = {
    var newData = data.columns.foldLeft(data) { 
      (df, c) => df.withColumnRenamed(c, c.replaceAll("\\s", ""))
    }
    return applyOverColumns(newData, trim)
  }
  
  /** Bins the given column values according to the defined splits */
  def binning(data: DataFrame, columnName: String, splits: Array[Double]): DataFrame = {
    val bucketizer = new Bucketizer()
      .setInputCol(columnName)
      .setOutputCol(s"transformed${columnName.toLowerCase.capitalize}")
      .setSplits(splits)

    return bucketizer.transform(data)
  }

  /** Discretizes the given column values according to the specified number of quantiles */
  def quantileDiscretizer(data: DataFrame, columnName: String, numBuckets: Int): DataFrame = {
    val discretizer = new QuantileDiscretizer()
      .setInputCol(columnName)
      .setOutputCol(s"transformed${columnName.toLowerCase.capitalize}")
      .setNumBuckets(numBuckets)

    return discretizer.fit(data).transform(data)
  }

}
