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

package edu.iu.dsc.tws.api.tset.link.streaming;

import edu.iu.dsc.tws.api.comms.structs.Tuple;
import edu.iu.dsc.tws.api.compute.OperationNames;
import edu.iu.dsc.tws.api.compute.graph.Edge;
import edu.iu.dsc.tws.api.tset.TSetUtils;
import edu.iu.dsc.tws.api.tset.env.StreamingTSetEnvironment;
import edu.iu.dsc.tws.api.tset.fn.PartitionFunc;

public class SKeyedPartitionTLink<K, V> extends SSingleLink<Tuple<K, V>> {
  private PartitionFunc<K> partitionFunction;

  public SKeyedPartitionTLink(StreamingTSetEnvironment tSetEnv, PartitionFunc<K> parFn,
                              int sourceParallelism) {
    super(tSetEnv, TSetUtils.generateName("skpartition"), sourceParallelism);
    this.partitionFunction = parFn;
  }

  @Override
  public Edge getEdge() {
    Edge e = new Edge(getId(), OperationNames.KEYED_PARTITION, getMessageType());
    e.setKeyed(true);
    e.setPartitioner(partitionFunction);
    return e;
  }

  @Override
  public SKeyedPartitionTLink<K, V> setName(String n) {
    rename(n);
    return this;
  }
}