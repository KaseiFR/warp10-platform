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
import io.warp10.script.WarpScriptJarRepository;
import io.warp10.script.WarpScriptStack;
import io.warp10.warp.sdk.WarpScriptJavaFunction;

/**
 * Invoke a User Defined Function
 */
public class UDF extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  private final boolean useCache;
  
  public UDF(String name, boolean useCache) {
    super(name);
    this.useCache = useCache;
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object symbol = stack.pop();
    
    if (!(symbol instanceof String)) {
      throw new WarpScriptException(getName() + " expects UDF class name to be a string.");
    }
    
    //
    // Retrieve function from repository
    //
    
    WarpScriptJavaFunction func = WarpScriptJarRepository.load(symbol.toString(), this.useCache);
    
    //
    // Do magic
    //
    
    stack.exec(func);
    
    return stack;
  }
}
