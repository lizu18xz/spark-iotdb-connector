./spark-shell --jars /home/rl/tsfile-0.5.0-SNAPSHOT.jar,/home/rl/iotdb-jdbc-0.5.0-SNAPSHOT.jar,/home/rl/iotdb-spark-connector-0.5.0.jar

val df = spark.read.format("cn.edu.tsinghua.tsfile").option("url","jdbc:tsfile://127.0.0.1:6667/").option("sql","select * from root").load

df.printSchema()

df.show()