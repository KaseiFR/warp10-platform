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

package io.warp10.script.processing.math;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.processing.ProcessingUtil;

import java.util.List;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Call dist
 */
public class Pdist extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public Pdist(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    List<Object> params = ProcessingUtil.parseParams(stack, 4, 6);
        
    PGraphics pg = (PGraphics) params.get(0);

    double dist = 0.0D;
    
    if (5 == params.size()) {
      dist = (double) PApplet.dist(
          ((Number) params.get(1)).floatValue(),
          ((Number) params.get(2)).floatValue(),
          ((Number) params.get(3)).floatValue(),
          ((Number) params.get(4)).floatValue()
          );
    } else if (6 == params.size()) {
      dist = (double) PApplet.dist(
          ((Number) params.get(1)).floatValue(),
          ((Number) params.get(2)).floatValue(),
          ((Number) params.get(3)).floatValue(),
          ((Number) params.get(4)).floatValue(),
          ((Number) params.get(5)).floatValue(),
          ((Number) params.get(6)).floatValue()
          );      
    }

    stack.push(pg);
    
    stack.push(dist);    
    
    return stack;
  }
}
