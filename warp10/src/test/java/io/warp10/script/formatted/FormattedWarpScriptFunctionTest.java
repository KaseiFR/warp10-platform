//
//   Copyright 2019  SenX S.A.S.
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

package io.warp10.script.formatted;

import io.warp10.WarpConfig;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.script.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.warp10.script.formatted.DocumentationGenerator.generateWarpScriptDoc;

public class FormattedWarpScriptFunctionTest extends FormattedWarpScriptFunction {

  private final Arguments args;

  protected Arguments getArguments() {
    return args;
  }

  public FormattedWarpScriptFunctionTest() {
    super("EXAMPLE");

    //
    // Arguments
    //

    args = new ArgumentsBuilder()
      .addArgument(GeoTimeSerie.class, "1st arg","A Geo Time Series™.")
      .addArgument(Long.class, "2nd arg","A LONG.")
      .addArgument(Double.class, "3rd arg","A DOUBLE.")
      .addOptionalArgument(String.class, "1st opt arg", "A STRING.", "The default value.")
      .build();

    //
    // Optional doc
    //

    StringBuilder docstring = getDocstring();
    docstring.append("This is an example implementation of FormattedWarpScriptFunction. " + getName() + " returns a map of its arguments.");

    //
    // Optional unit tests
    //

    List<String> unitTests = getUnitTests();

    // test positional arguments
    StringBuilder test1 = new StringBuilder();
    test1.append("NEWGTS 3 0.5 EXAMPLE 'res' STORE" + System.lineSeparator());
    test1.append("$res '1st arg' GET TYPEOF 'GTS' == ASSERT" + System.lineSeparator());
    test1.append("$res '2nd arg' GET 3 == ASSERT" + System.lineSeparator());
    test1.append("$res '3rd arg' GET 0.5 == ASSERT" + System.lineSeparator());
    test1.append("$res SIZE 4 == ASSERT" + System.lineSeparator());
    test1.append("$res '1st opt arg' GET 'The default value.' == ASSERT");
    unitTests.add(test1.toString());

    // test map arguments
    StringBuilder test2 = new StringBuilder();
    test2.append("{ '1st arg' NEWGTS '2nd arg' 3 '3rd arg' 0.5 } EXAMPLE 'res' STORE" + System.lineSeparator());
    test2.append("$res '1st arg' GET TYPEOF 'GTS' == ASSERT" + System.lineSeparator());
    test2.append("$res '2nd arg' GET 3 == ASSERT" + System.lineSeparator());
    test2.append("$res '3rd arg' GET 0.5 == ASSERT" + System.lineSeparator());
    test2.append("$res SIZE 4 == ASSERT" + System.lineSeparator());
    test2.append("$res '1st opt arg' GET 'The default value.' == ASSERT");
    unitTests.add(test2.toString());

    // test map arguments with optional ones
    StringBuilder test3 = new StringBuilder();
    test3.append("{ '1st arg' NEWGTS '2nd arg' 3 '3rd arg' 0.5 '1st opt arg' 'hi' } EXAMPLE 'res' STORE" + System.lineSeparator());
    test3.append("$res '1st arg' GET TYPEOF 'GTS' == ASSERT" + System.lineSeparator());
    test3.append("$res '2nd arg' GET 3 == ASSERT" + System.lineSeparator());
    test3.append("$res '3rd arg' GET 0.5 == ASSERT" + System.lineSeparator());
    test3.append("$res '1st opt arg' GET 'hi' == ASSERT" + System.lineSeparator());
    test3.append("$res SIZE 4 == ASSERT");
    unitTests.add(test3.toString());

  }

  //
  // The application of this function on the stack
  // formattedArgs contains positional and optional arguments
  //

  protected WarpScriptStack apply(Map<String, Object> formattedArgs, WarpScriptStack stack) throws WarpScriptException {

    stack.push(formattedArgs);
    return stack;
  }

  //
  // Running unit tests
  //

  @BeforeClass
  public static void beforeClass() throws Exception {
    StringBuilder props = new StringBuilder();

    props.append("warp.timeunits=us");

    WarpConfig.safeSetProperties(new StringReader(props.toString()));

    WarpScriptLib.addNamedWarpScriptFunction(new FormattedWarpScriptFunctionTest());
  }

  @Test
  public void testPositionalArguments() throws Exception {
    MemoryWarpScriptStack stack = new MemoryWarpScriptStack(null, null);

    stack.execMulti(getUnitTests().get(0));
  }

  @Test
  public void testMapArguments() throws Exception {
    MemoryWarpScriptStack stack = new MemoryWarpScriptStack(null, null);

    stack.execMulti(getUnitTests().get(1));
  }

  @Test
  public void testMapArgumentsWithOptionalOnes() throws Exception {
    MemoryWarpScriptStack stack = new MemoryWarpScriptStack(null, null);

    stack.execMulti(getUnitTests().get(2));
  }

  @Test
  public void testDocGeneration() throws Exception {
    MemoryWarpScriptStack stack = new MemoryWarpScriptStack(null, null);

    List<ArgumentSpecification> output = new ArrayList<>();
    output.add(new ArgumentSpecification(Map.class, "result", "A map containing the input arguments."));

    stack.execMulti(generateWarpScriptDoc(this, output));
    stack.execMulti("'EXAMPLE' DEF");

    try {
      stack.execMulti("INFOMODE EXAMPLE");
    } catch (WarpScriptStopException wse) {
      // do nothing
    }

    // manual check that INFO is correct
    System.out.println(stack.dump(10));
  }

}