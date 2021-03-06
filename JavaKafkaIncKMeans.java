/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.examples.streaming;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.Date;

import scala.Tuple2;

import com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.examples.streaming.StreamingExamples;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 *
 * Usage: JavaKafkaIncKMeans <zkQuorum> <group> <topics> <numThreads>
 *   <zkQuorum> is a list of one or more zookeeper servers that make quorum
 *   <group> is the name of kafka consumer group
 *   <topics> is a list of one or more kafka topics to consume from
 *   <numThreads> is the number of threads the kafka consumer should use
 *
 * To run this example:
 *   `$ bin/run-example org.apache.spark.examples.streaming.JavaKafkaIncKMeans zoo01,zoo02, \
 *    zoo03 my-consumer-group topic1,topic2 1`
 */

public final class JavaKafkaIncKMeans {
  private static final Pattern SPACE = Pattern.compile(" ");
  private static final double threshHold = .70;
  private static int numberOfPoints = 0;
  private static int TRAINING_POINTS = 10000;
  private static int benign = 0;
  private static int outlier = 0;
  //private static long lStartTime = 0;
 // private static final int totalClusterPoint = 10;
  private static IncKMean incKMean = new IncKMean("IncKMean", threshHold);
  
  private JavaKafkaIncKMeans() {
  }
  
 	public static void main(String[] args) {
    
	if (args.length < 4) {
      System.err.println("Usage: JavaKafkaIncKMeans <zkQuorum> <group> <topics> <numThreads>");
      System.exit(1);
    }

    StreamingExamples.setStreamingLogLevels();
    SparkConf sparkConf = new SparkConf().setAppName("JavaKafkaIncKMeans");
    // Create the context with a 1 second batch size
    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(2000));
    int numThreads = Integer.parseInt(args[3]);	
	
    Map<String, Integer> topicMap = new HashMap<String, Integer>();
    String[] topics = args[2].split(",");
    for (String topic: topics) {
      topicMap.put(topic, numThreads);
    }

   //final long lStartTime = new Date().getTime(); // start time
 
   JavaPairReceiverInputDStream<String, String> messages =
            KafkaUtils.createStream(jssc, args[0], args[1], topicMap);

    JavaDStream<String> lines = messages.map(new Function<Tuple2<String, String>, String>() {
      @Override
      public String call(Tuple2<String, String> tuple2) {
		return tuple2._2();
      }
    });

    JavaPairDStream<String, String> clusters = lines.mapToPair(new PairFunction<String, String, String>() {
      @Override
      public Tuple2<String, String> call(String x) {

               //long lStartTime = new Date().getTime(); // start time
		long lStartTime = System.nanoTime();

                // String value = word.substring(word.indexOf(",") + 1);
		String[] points = x.split(",");
                               
               if (numberOfPoints < TRAINING_POINTS) 
               {

                	Cluster cluster = incKMean.getNearestCluster(points);
                	String center = cluster.centroid.toString();
                	String pointWithCOunt = x + ";1";                             
                        //numberOfPoints++;
                        //long lEndTime = new Date().getTime(); // end time
                        long lEndTime = System.nanoTime();
                        long difference = lEndTime - lStartTime; // check different
                	return new Tuple2<String, String>(center, pointWithCOunt + ":" + difference );
                }
               else{
                       //numberOfPoints++;
                       //long lEndTime = new Date().getTime(); // end time
                       long lEndTime = System.nanoTime();
                       long difference = lEndTime - lStartTime; // check different 
                       return new Tuple2<String, String>(incKMean.isFitInCluser(points)? "<benign>" : "<outlier>", 1 + ":" + difference);              
                 }
               }
    }).reduceByKey(new Function2<String, String, String>() {
        @Override
        public String call(String s11, String s22) {
             
             String [] s1_array = s11.split(":");    
             
             String s1 = s1_array[0];
             long s1_time = Long.parseLong(s1_array[1]);
 
             String [] s2_array = s22.split(":");

             String s2 = s2_array[0];
             long s2_time = Long.parseLong(s2_array[1]);

             s2_time += s1_time;


             if (numberOfPoints < TRAINING_POINTS)
             {
               String [] s1_collection = s1.split(";");
               String [] s1_point = s1_collection[0].split(",");
               String s1_numOfPoint = s1_collection[1];
                 
               String [] s2_collection = s2.split(";");
               String [] s2_point = s2_collection[0].split(",");
               String s2_numOfPoint = s2_collection[1];
               
               String sumOfString = ""; 
               for (int i = 0; i < s1_point.length; i++)
               {
                 double p = Double.parseDouble(s1_point[i]) + Double.parseDouble(s2_point[i]);
                              
                 if(i == s1_point.length - 1)
                    sumOfString += p + ";";
                 else
                   sumOfString += p + ",";                 
               }  

               int numberOfPoint = Integer.parseInt(s1_numOfPoint) + Integer.parseInt(s2_numOfPoint);
               sumOfString += (numberOfPoint); 
               return sumOfString + ":" + s2_time;        
              }
              else{
                   int count = Integer.parseInt(s1) + Integer.parseInt(s2);
                   return count + ":" + s2_time; 
              }

               //return "Input from s1: " + s1 + " :: input from s2: " + s2; 
              }
      });

    

     JavaDStream<String> updateClusterInfo = clusters.map(new Function<Tuple2<String, String>, String>() {
        @Override
        public String call(Tuple2<String, String> tuple2) {
               //return tuple2._1() + "::" + tuple2._2();
               
              //long lStartTime = new Date().getTime(); // start time 
              long lStartTime = System.nanoTime(); 
              
              if (numberOfPoints < TRAINING_POINTS)
               {
             	   String [] key_collection = tuple2._1().replaceAll("\\[", "").replaceAll("\\]", "").split(","); 
              
                   String [] s_array = tuple2._2().split(":");
 
                   long s_time = Long.parseLong(s_array[1]);

                   String [] s_collection = s_array[0].split(";");
                   String  s_point = s_collection[0];
                   String s_numOfPoint = s_collection[1]; 
                   int count = Integer.parseInt(s_numOfPoint);
                   s_time = s_time / count;
                   incKMean.updateCluster(key_collection, s_point.split(","), count);
                   numberOfPoints += count;               
                   //long lEndTime = new Date().getTime(); // end time
                   long lEndTime = System.nanoTime();
                   long difference = lEndTime - lStartTime; // check different
                   long totalTime = s_time + difference;
                   return incKMean.toString() + "\n Elapsed milliseconds for training: " + totalTime; 
               }
               else
               {
                  String [] s_array = tuple2._2().split(":");
                  long s_time = Long.parseLong(s_array[1]);

                  int count = Integer.parseInt(s_array[0]);
                  s_time = s_time / count;
                  String key = tuple2._1();
                  numberOfPoints += count;
                  
                  String message = "";
                  if (key.equalsIgnoreCase("<benign>"))  
                  {
                    benign = benign + count;
                    message = "benign: " + benign;
                  } 
                  else
                  {
                    outlier = outlier + count;
                    message = "outlier: " + outlier;
                  }

                  //long lEndTime = new Date().getTime(); // end time
                  long lEndTime = System.nanoTime();
                  long difference = lEndTime - lStartTime; // check different 

                  long totalTime = s_time + difference;
                  return incKMean.toString() + "\n" +  message +  "\n Elapsed milliseconds for predicting: " + totalTime;
               }  
        }
      });   
 
    clusters.print();
    updateClusterInfo.print();
    jssc.start();
    jssc.awaitTermination();
  }
}
