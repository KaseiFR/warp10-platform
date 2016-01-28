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

package io.warp10.script.filter;

import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptFilterFunction;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterByLabels extends NamedWarpScriptFunction implements WarpScriptFilterFunction {
  
  private final Map<String,Pattern> selectors;
  
  public static class Builder extends NamedWarpScriptFunction implements WarpScriptStackFunction {
    
    public Builder(String name) {
      super(name);
    }
    
    @Override
    public Object apply(WarpScriptStack stack) throws WarpScriptException {
      Object arg = stack.pop();
      if (!(arg instanceof Map<?,?>)) {
        throw new WarpScriptException("Invalid labels selector");
      }
      stack.push(new FilterByLabels(getName(), (Map<String,String>) arg));
      return stack;
    }
  }

  public FilterByLabels(String name, Map<String,String> selectors) {
    
    super(name);
    
    this.selectors = new HashMap<String, Pattern>();
    
    for (Entry<String,String> entry: selectors.entrySet()) {
      String lname = entry.getKey();
      String selector = entry.getValue();
      Pattern pattern;
      
      if (selector.startsWith("=")) {
        pattern = Pattern.compile(Pattern.quote(selector.substring(1)));
      } else if (selector.startsWith("~")) {
        pattern = Pattern.compile(selector.substring(1));
      } else {
        pattern = Pattern.compile(Pattern.quote(selector));
      }
      
      this.selectors.put(lname, pattern);
    }
  }
  
  @Override
  public List<GeoTimeSerie> filter(Map<String,String> commonlabels, List<GeoTimeSerie>... series) throws WarpScriptException {
    
    List<GeoTimeSerie> retained = new ArrayList<GeoTimeSerie>();
    
    Map<String,Matcher> matchers = new HashMap<String,Matcher>();
    
    for (Entry<String,Pattern> entry: this.selectors.entrySet()) {
      matchers.put(entry.getKey(), entry.getValue().matcher(""));
    }
    
    for (List<GeoTimeSerie> serie: series) {
      for (GeoTimeSerie gts: serie) {
        Map<String,String> labels = gts.getLabels();
        
        boolean matched = true;
        
        for (String label: matchers.keySet()) {
          // Skip GTS if one of the required labels is not there.
          if (!labels.containsKey(label)) {
            matched = false;
            break;
          }
          // If the label value does not match its selector, skip the 
          if (!matchers.get(label).reset(labels.get(label)).matches()) {
            matched = false;
            break;
          }
        }
        
        if (matched) {
          retained.add(gts);
        }        
      }
    }
    
    return retained;
  }
}
