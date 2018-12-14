//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.api.htgjob;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.JobConfig;
import edu.iu.dsc.tws.api.Twister2Submitter;
import edu.iu.dsc.tws.api.job.Twister2Job;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.resource.NodeInfoUtils;
import edu.iu.dsc.tws.master.server.JobMaster;
import edu.iu.dsc.tws.master.worker.JMWorkerController;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.proto.system.job.HTGJobAPI;
import edu.iu.dsc.tws.proto.system.job.JobAPI;

public final class Twister2HTGSubmitter {

  private static final Logger LOG = Logger.getLogger(Twister2HTGSubmitter.class.getName());

  private Config config;
  private Twister2HTGClient client;
  private JobMaster jobMaster;

  public Twister2HTGSubmitter(Config cfg) {
    this.config = cfg;
  }

  /**
   * The execute method first call the schedule method to get the schedule list of the HTG. Then,
   * it invokes the build HTG Job object to build the htg job object for the scheduled graphs.
   */
  public void execute(Twister2Metagraph twister2Metagraph,
                      JobConfig jobConfig,
                      String workerclassName) {

    LOG.fine("HTG Sub Graph Requirements:" + twister2Metagraph.getSubGraph()
        + "\nHTG Relationship Values:" + twister2Metagraph.getRelation());

    //Call the schedule method to identify the order of graphs to be executed
    List<String> scheduleGraphs = schedule(twister2Metagraph);
    buildHTGJob(scheduleGraphs, twister2Metagraph, workerclassName, jobConfig);
  }

  /**
   * This method is responsible for building the htg job object which is based on the outcome of
   * the scheduled graphs list.
   */
  private void buildHTGJob(List<String> scheduleGraphs, Twister2Metagraph twister2Metagraph,
                           String workerclassName, JobConfig jobConfig) {

    Twister2Job twister2Job;
    Twister2Metagraph.SubGraph subGraph;
    HTGJobAPI.ExecuteMessage executeMessage;

    //Construct the HTGJob object to be sent to Job Master
    HTGJobAPI.HTGJob htgJob = HTGJobAPI.HTGJob.newBuilder()
        .setHtgJobname(twister2Metagraph.getHTGName())
        .addAllGraphs(twister2Metagraph.getSubGraph())
        .addAllRelations(twister2Metagraph.getRelation())
        .build();

    for (int i = 0; i < scheduleGraphs.size(); i++) {

      String subgraphName = scheduleGraphs.get(i);

      //Retrieve the resource value for the graph to be scheduled.
      subGraph = twister2Metagraph.getMetaGraphMap(subgraphName);

      //Set the subgraph to be executed from the metagraph
      executeMessage = HTGJobAPI.ExecuteMessage.newBuilder()
          .setSubgraphName(subgraphName)
          .build();

      twister2Job = Twister2Job.newBuilder()
          .setJobName(htgJob.getHtgJobname())
          .setWorkerClass(workerclassName)
          .addComputeResource(subGraph.getCpu(), subGraph.getRamMegaBytes(),
              subGraph.getDiskGigaBytes(), subGraph.getNumberOfInstances())
          .setConfig(jobConfig)
          .build();

      //Now submit the job
      Twister2Submitter.submitJob(twister2Job, config);

      //This is for validation to start the job master
      //TODO: Discuss with Ahmet for the order of execution.
      if (i == 0) {
        //startJobMaster(twister2Job);

        //Create the connection between the HTGClient and JobMaster
        createHTGMasterClient(htgJob);
      }

      //Submit the HTG Job and the execute message
      //submitToJobMaster(htgJob, executeMessage);

      client.setExecuteMessage(executeMessage);
      client.sendHTGClientRequestMessage();

      //client.getHTGClientResponseMessage();

      sleep((long) (Math.random() * 2000));
    }
    client.close();
  }


  /**
   * This method initializes the HTGClient and send the generated HTG Job object and the name of
   * the graph to be executed (executeMessage).
   */
  public Twister2HTGClient createHTGMasterClient(HTGJobAPI.HTGJob htgJob) {

    //Send the HTG Job information to execute the part of the HTG
    LOG.fine("HTG Job Objects:" + htgJob);

    InetAddress htgClientIP = JMWorkerController.convertStringToIP("localhost");
    int htgClientPort = 10000 + (int) (Math.random() * 10000);
    int htgClientTempID = 0;

    JobMasterAPI.NodeInfo nodeInfo = NodeInfoUtils.createNodeInfo(
        "htg.client.ip", "rack01", null);
    JobMasterAPI.HTGClientInfo htgClientInfo = HTGClientInfoUtils.createHTGClientInfo(
        htgClientTempID, htgClientIP.getHostAddress(), htgClientPort, nodeInfo);

    //client = new Twister2HTGClient(config, htgClientInfo, htgJob, executeMessage);

    client = new Twister2HTGClient(config, htgClientInfo, htgJob);
    Thread clientThread = client.startThreaded();

    if (clientThread == null) {
      LOG.severe("HTG Client can not initialize. Exiting ...");
      return null;
    }

    //IWorkerController workerController = client.getJMWorkerController();
    //client.sendHTGClientRequestMessage();

    // wait up to 4sec
    //sleep((long) (Math.random() * 4000));
    return client;
  }


  /**
   * This method initializes the HTGClient and send the generated HTG Job object and the name of
   * the graph to be executed (executeMessage).
   */
  public String submitToJobMaster(HTGJobAPI.HTGJob htgJob,
                                  HTGJobAPI.ExecuteMessage executeMessage) {

    //Send the HTG Job information to execute the part of the HTG
    LOG.fine("HTG Job Objects:" + htgJob + "\tsubgraph to be executed:" + executeMessage);

    InetAddress htgClientIP = JMWorkerController.convertStringToIP("localhost");
    int htgClientPort = 10000 + (int) (Math.random() * 10000);
    int htgClientTempID = 0;

    JobMasterAPI.NodeInfo nodeInfo = NodeInfoUtils.createNodeInfo(
        "htg.client.ip", "rack01", null);
    JobMasterAPI.HTGClientInfo htgClientInfo = HTGClientInfoUtils.createHTGClientInfo(
        htgClientTempID, htgClientIP.getHostAddress(), htgClientPort, nodeInfo);

    //client = new Twister2HTGClient(config, htgClientInfo, htgJob, executeMessage);

    client = new Twister2HTGClient(config, htgClientInfo, htgJob);
    Thread clientThread = client.startThreaded();

    if (clientThread == null) {
      LOG.severe("HTG Client can not initialize. Exiting ...");
      return null;
    }

    //IWorkerController workerController = client.getJMWorkerController();
    client.sendHTGClientRequestMessage();

    // wait up to 4sec
    sleep((long) (Math.random() * 4000));

    return "Finished Graph Execution";
  }

  //TODO:Starting Job Master for validation (It would be removed)
  public void startJobMaster(Twister2Job twister2Job) {
    JobAPI.Job job = twister2Job.serialize();
    jobMaster = new JobMaster(config, "localhost", null, job, null);
    jobMaster.startJobMasterThreaded();
  }

  public static void sleep(long duration) {
    LOG.info("Sleeping " + duration + "ms............");
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * This schedule is the base method for making decisions to run the part of the task graph which
   * will be improved further with the complex logic. Now, based on the relations(parent -> child)
   * it will initiate the execution.
   */
  private List<String> schedule(Twister2Metagraph twister2Metagraph) {

    List<String> scheduledGraph = new LinkedList<>();

    if (twister2Metagraph.getRelation().size() == 1) {
      ((LinkedList<String>) scheduledGraph).addFirst(
          twister2Metagraph.getRelation().iterator().next().getParent());
      scheduledGraph.addAll(Collections.singleton(
          twister2Metagraph.getRelation().iterator().next().getChild()));
    } else {
      for (int i = 0; i < twister2Metagraph.getRelation().size(); i++) {
        ((LinkedList<String>) scheduledGraph).addFirst(
            twister2Metagraph.getRelation().iterator().next().getParent());
        scheduledGraph.addAll(Collections.singleton(
            twister2Metagraph.getRelation().iterator().next().getChild()));
      }
    }
    LOG.info("%%%% Scheduled Graph list details: %%%%" + scheduledGraph);
    return scheduledGraph;
  }

  public static class HTGClientInfoUtils {
    public static JobMasterAPI.HTGClientInfo createHTGClientInfo(int htgClientID,
                                                                 String hostAddress,
                                                                 int htgClientPort,
                                                                 JobMasterAPI.NodeInfo nodeInfo) {

      JobMasterAPI.HTGClientInfo.Builder builder = JobMasterAPI.HTGClientInfo.newBuilder();
      builder.setClientID(htgClientID);
      builder.setClientIP(hostAddress);
      builder.setPort(htgClientPort);

      if (nodeInfo != null) {
        builder.setNodeInfo(nodeInfo);
      }
      return builder.build();
    }

    public static JobMasterAPI.HTGClientInfo updateHTGClientID(
        JobMasterAPI.HTGClientInfo htgClientInfo, int clientID) {

      return createHTGClientInfo(clientID,
          htgClientInfo.getClientIP(),
          htgClientInfo.getPort(),
          htgClientInfo.getNodeInfo());
    }
  }
}