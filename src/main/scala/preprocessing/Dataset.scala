package preprocessing

import utils._

import org.apache.spark.sql.{SparkSession, DataFrame, DataFrameReader}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.functions.countDistinct

trait DatasetProperty {

  /** CSV dataset delimiter */
  val delimiter = ","

  /** Specifies discrete columns */
  def getDiscreteColumnNames(): Array[String] = Array()

  /** Specifies continuos columns */
  def getContinuosColumnNames(): Array[String] = Array()

  /** Specifies target columns */
  def getTargetColumnNames(): Array[String] = Array()

  /** Get useful column names */
  def getUsefulColumnNames(): Array[String] = {
    return getDiscreteColumnNames() ++ getContinuosColumnNames() ++ getTargetColumnNames()
  }

  /** Returns the dataset schema string */
  def getSchemaString(): Option[String] = None

  /** Returns the dataset schema */
  def getSchema(): Option[StructType] = {
    getSchemaString() match {
      case Some(schemaString) => return Some(StructType.fromDDL(schemaString))
      case None               => None
    }
  }

  /** Loads the dataset */
  def load(inputPath: String): DataFrame = {
    // Prepare the DataFrameReader
    var reader: DataFrameReader = SparkSession.builder
      .getOrCreate()
      .read

    // Check if a schema is provided (otherwise try to infer it)
    getSchema() match {
      case Some(schema) => reader = reader.schema(schema)
      case None         => reader = reader.option("inferSchema", "true")
    }

    // Load the dataset
    return reader
      .format("csv")
      .option("delimiter", delimiter)
      .option("header", "true")
      .option("mode", "DROPMALFORMED")
      .load(inputPath)
  }

}

object Dataset {

  /** Dataset's factory method */
  def apply(
      name: String,
      inputPath: Option[String] = None,
      inputData: Option[DataFrame] = None
  ) =
    name.capitalize match {
      case "Adult"  => AdultDataset(inputPath, inputData)
      case "Bosch"  => BoschDataset(inputPath, inputData)
      case "Arrest" => ArrestDataset(inputPath, inputData)
      case _        => throw new IllegalArgumentException("Unsupported dataset.")
    }

}

abstract class Dataset(
    inputPath: Option[String] = None,
    inputData: Option[DataFrame] = None
) {

  // Define type variables
  type T <: Dataset

  /** Stores the companion object */
  def property: DatasetProperty

  /** Loads data */
  var data: DataFrame = _
  inputPath match {
    case Some(value) => data = property.load(value)
    case None => {
      inputData match {
        case Some(value) => data = value
        case None =>
          throw new IllegalArgumentException(
            "Either inputhPath or inputData should be given."
          )
      }
    }
  }

  /** Applies a sequence of pre-processing functions to the given DataFrame */
  def preprocess(): T

  /** Returns the DataFrame's column names */
  def getColumnNames(): Array[String] = data.columns.toArray

  /** Returns the DataFrame's rows number */
  def getNumRows(): Int = data.count.toInt

  /** Shows the DataFrame */
  def show(): Unit = data.show()

  /** Renames the given column */
  def renameColumn(before: String, after: String): Unit = {
    data = data.withColumnRenamed(before, after)
  }

  /** Returns the maximum number of distinct values over all columns */
  def maxDistinctValues: Int =
    data.columns
      .map(c => data.agg(countDistinct(c)).first.getLong(0))
      .max
      .toInt

}
