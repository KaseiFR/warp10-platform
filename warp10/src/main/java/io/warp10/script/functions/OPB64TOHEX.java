//
//   Copyright 2018  SenX S.A.S.
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

import io.warp10.crypto.OrderPreservingBase64;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

import org.bouncycastle.util.encoders.Hex;

import com.google.common.base.Charsets;

/**
 * Decode a String in order preserving b64 and immediately encode it as hexadecimal
 */
public class OPB64TOHEX extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public OPB64TOHEX(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object o = stack.pop();
    
    if (!(o instanceof String)) {
      throw new WarpScriptException(getName() + " operates on a String.");
    }

    stack.push(new String(Hex.encode(OrderPreservingBase64.decode(o.toString().getBytes(Charsets.US_ASCII))), Charsets.US_ASCII));

    return stack;
  }
}
