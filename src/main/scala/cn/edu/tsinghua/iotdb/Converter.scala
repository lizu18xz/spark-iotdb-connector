package cn.edu.tsinghua.iotdb

import java.sql._

import org.apache.spark.sql.types._
import org.slf4j.LoggerFactory
import java.sql.Statement

import scala.collection.mutable.ListBuffer

/**
  * Created by qjl on 16-11-3.
  */

class Converter

object Converter {
  private final val logger = LoggerFactory.getLogger(classOf[Converter])

  def toSqlData(field: StructField, value: String): Any = {
    if (value == null || value.equals(SQLConstant.NULL_STR)) return null

    val r = field.dataType match {
      case BooleanType => java.lang.Boolean.valueOf(value)
      case IntegerType => value.toInt
      case LongType => value.toLong
      case FloatType => value.toFloat
      case DoubleType => value.toDouble
      case StringType => value
      case other => throw new UnsupportedOperationException(s"Unsupported type $other")
    }
    r
  }

  def toSparkSchema(options: IoTDBOptions): StructType = {

    Class.forName("cn.edu.tsinghua.iotdb.jdbc.TsfileDriver")
    val sqlConn: Connection = DriverManager.getConnection(options.url, options.user, options.password)
    val sqlStatement: Statement = sqlConn.createStatement()
    val hasResultSet: Boolean = sqlStatement.execute(options.sql)

    val fields = new ListBuffer[StructField]()
    if(hasResultSet) {
      val resultSet: ResultSet = sqlStatement.getResultSet()
      val resultSetMetaData: ResultSetMetaData = resultSet.getMetaData()

      if (resultSetMetaData.getColumnTypeName(0) != null) {
        val printTimestamp:Boolean = !resultSetMetaData.getColumnTypeName(0).toUpperCase().equals(SQLConstant.NEED_NOT_TO_PRINT_TIMESTAMP);
        if(printTimestamp) {
          fields += StructField(SQLConstant.TIMESTAMP_STR, LongType, nullable = false)
        }
      }

      val colCount = resultSetMetaData.getColumnCount()
      val i: Int = 0
      for (i <- 2 to colCount) {
        fields += StructField(resultSetMetaData.getColumnLabel(i), resultSetMetaData.getColumnType(i) match {
          case Types.BOOLEAN => BooleanType
          case Types.INTEGER => IntegerType
          case Types.BIGINT => LongType
          case Types.FLOAT => FloatType
          case Types.DOUBLE => DoubleType
          case Types.VARCHAR => StringType
          case other => throw new UnsupportedOperationException(s"Unsupported type $other")
        }, nullable = true)
      }
      StructType(fields.toList)
    }
    else {
      StructType(fields)
    }
  }
}
