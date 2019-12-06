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

package org.apache.storm.topology.base;

import org.apache.storm.topology.IRichBolt;

public abstract class BaseRichBolt extends BaseComponent implements IRichBolt {
  private static final long serialVersionUID = 5749013017107995933L;

  @Override
  public void cleanup() {
  }
}