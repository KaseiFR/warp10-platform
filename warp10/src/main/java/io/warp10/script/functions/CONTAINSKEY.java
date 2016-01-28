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

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

import java.util.Map;

/**
 * Checks if a map contains an element as key
 */
public class CONTAINSKEY extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public CONTAINSKEY(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object elt = stack.pop();    
    Object map = stack.peek();

    if (!(map instanceof Map)) {
      throw new WarpScriptException(getName() + " operates on a map.");
    }
    
    stack.push(((Map) map).containsKey(elt));

    return stack;
  }
}
