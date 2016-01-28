//
//   Copyright 2016  Cityzen Data
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.op;

import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptNAryFunction;
import io.warp10.script.WarpScriptException;

import java.util.Map;

/**
 * Operate on two geo time series, the first one is a boolean
 * GTS and serves as the mask. If a value is TRUE, then output
 * the location/elevation/value of the second serie, otherwise
 * output null (i.e. no value)
 */
public class OpMask extends NamedWarpScriptFunction implements WarpScriptNAryFunction {
  
  public OpMask(String name) {
    super(name);
  }
  
  
  @Override
  public Object apply(Object[] args) throws WarpScriptException {
    long tick = (long) args[0];
    String[] names = (String[]) args[1];
    Map<String,String>[] labels = (Map<String,String>[]) args[2];
    long[] ticks = (long[]) args[3];
    long[] locations = (long[]) args[4];
    long[] elevations = (long[]) args[5];
    Object[] values = (Object[]) args[6];
    
    if (2 != ticks.length || ticks[0] == ticks[1]) {
      throw new WarpScriptException("op.mask can only be applied to pairs of geo time series, the first one being boolean.");
    }
    
    Object value = null;
    long location = GeoTimeSerie.NO_LOCATION;
    long elevation = GeoTimeSerie.NO_ELEVATION;
    
    
    //
    // If the mask value is true, output the value of the second GTS
    //
    if (Boolean.TRUE.equals(values[0])) {
      location = locations[1];
      elevation = elevations[1];
      value = values[1];
    }
    
    return new Object[] { tick, location, elevation, value };
  }
}
