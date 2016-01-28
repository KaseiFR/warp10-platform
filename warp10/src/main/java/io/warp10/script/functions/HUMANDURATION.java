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

package io.warp10.script.functions;

import io.warp10.continuum.store.Constants;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

/**
 * Convert a duration in time units into one suitable for humans...
 */
public class HUMANDURATION extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public HUMANDURATION(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object top = stack.pop();
    
    if (!(top instanceof Long)) {
      throw new WarpScriptException(getName() + " expects a number of time units (LONG) on top of the stack.");
    }
    
    long duration = ((Number) top).longValue();
        
    StringBuilder sb = new StringBuilder();
    
    long days = duration / (Constants.TIME_UNITS_PER_S * 86400L);
    
    if (days > 0) {
      sb.append(days);
      sb.append("d");
      duration = duration - days * (Constants.TIME_UNITS_PER_S * 86400L);
    }
    
    long hours = duration / (Constants.TIME_UNITS_PER_S * 3600L);
    
    if (hours > 0 || sb.length() > 0) {
      sb.append(hours);
      sb.append("h");
      duration = duration - hours * (Constants.TIME_UNITS_PER_S * 3600L);
    }
    
    long minutes = duration / (Constants.TIME_UNITS_PER_S * 60L);
    
    if (minutes > 0 || sb.length() > 0) {
      sb.append(minutes);
      sb.append("m");
      duration = duration - minutes * (Constants.TIME_UNITS_PER_S * 60L);      
    }
    
    long seconds = duration / Constants.TIME_UNITS_PER_S;
    
    sb.append(seconds);
    duration = duration - seconds * Constants.TIME_UNITS_PER_S;
    sb.append(".");
    sb.append(Long.toString(Constants.TIME_UNITS_PER_S + duration).substring(1));
    sb.append("s");
    
    stack.push(sb.toString());
    
    return stack;
  }
}
