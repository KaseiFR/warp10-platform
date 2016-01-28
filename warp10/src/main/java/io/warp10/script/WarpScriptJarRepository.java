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

package io.warp10.script;

import io.warp10.continuum.Configuration;
import io.warp10.continuum.sensision.SensisionConstants;
import io.warp10.sensision.Sensision;
import io.warp10.warp.sdk.WarpScriptJavaFunction;
import io.warp10.warp.sdk.WarpScriptJavaFunctionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.bouncycastle.util.encoders.Hex;

import com.google.common.base.Charsets;

/**
 * Class which manages Einstein functions stored in jar files from a directory
 */
public class WarpScriptJarRepository extends Thread {
  
  /**
   * Default refresh delay is 60 minutes
   */
  private static final long DEFAULT_DELAY = 3600000L;

  private static final String JAR_EXTENSION = ".jar";
  
  /**
   * Directory where the '.jar' files are
   */
  private final String directory;
  
  /**
   * How often to check for changes
   */
  private final long delay;
  
  /**
   * Active Class Loaders and their associated fingerprint.
   */
  private final static Map<ClassLoader,String> classLoadersFingerprints = new LinkedHashMap<ClassLoader,String>();
  
  private static ClassLoader classPathClassLoader = null;
  
  private final static Map<String,WarpScriptJavaFunction> cachedUDFs = new HashMap<String, WarpScriptJavaFunction>();
  
  public WarpScriptJarRepository(String directory, long delay) {
    this.directory = directory;
    this.delay = delay;
    
    this.setName("[Warp Jar Repository (" + directory + ")");
    this.setDaemon(true);
    this.start();
  }
    
  @Override
  public void run() {
    while(true) {
      
      String rootdir = new File(this.directory).getAbsolutePath();
      
      //
      // Open directory
      //
      
      File[] files = new File(rootdir).listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          if (!name.endsWith(JAR_EXTENSION)) {
            return false;
          } else {
            return true;
          }
        }
      });

      //
      // Loop over the files, creating the class loaders
      //

      // Map of CL to fingerprint 
      Map<ClassLoader,String> newClassLoadersFingerprints = new LinkedHashMap<ClassLoader,String>();
      
      byte[] buf = new byte[8192];
      
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-1");

        for (File file: files) {
          
          //
          // Compute hash of content
          //
          
          FileInputStream in = new FileInputStream(file);
           
          while(true) {
            int len = in.read(buf);
            
            if (len < 0) {
              break;
            }
            
            md.update(buf, 0, len);
          }

          in.close();
          
          String hash = new String(Hex.encode(md.digest()), Charsets.US_ASCII);
            
          if (classLoadersFingerprints.containsValue(hash) && !newClassLoadersFingerprints.containsValue(hash)) {
            // Reuse existing class loader, so we keep the created objets
            for (Entry<ClassLoader,String> entry: classLoadersFingerprints.entrySet()) {
              if (entry.getValue().equals(hash)) {
                newClassLoadersFingerprints.put(entry.getKey(), entry.getValue());
              }
            }
          } else if (!newClassLoadersFingerprints.containsKey(hash)){
            // Create new classloader with filtering so caller cannot access the io.warp10 classes, except those needed
            ClassLoader filteringCL = new ClassLoader(this.getClass().getClassLoader()) {
              @Override
              protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {                
                if (name.startsWith("io.warp10") && !name.startsWith("io.warp10.warp.sdk.")) {
                  throw new ClassNotFoundException();
                } else {
                  return this.getParent().loadClass(name);
                }
              }
            };

            URL[] urls = new URL[1];
            urls[0] = file.toURI().toURL();
            
            newClassLoadersFingerprints.put(new URLClassLoader(urls, filteringCL), hash);
          }
        }  
      } catch (NoSuchAlgorithmException nsae) {
      } catch (IOException ioe) {        
      }
      
      //
      // Replace the previous classLoaders
      //
      
      synchronized(classLoadersFingerprints) {
        classLoadersFingerprints.clear();
        classLoadersFingerprints.putAll(newClassLoadersFingerprints);
        // Add the class path class loader too
        if (null != classPathClassLoader) {
          classLoadersFingerprints.put(classPathClassLoader, "");
        }
      }
      
      //
      // Update jar count
      //
      
      Sensision.set(SensisionConstants.SENSISION_CLASS_EINSTEIN_REPOSITORY_JARS, Sensision.EMPTY_LABELS, classLoadersFingerprints.size());
      
      //
      // Sleep a while
      //
      
      try {
        Thread.sleep(this.delay);
      } catch (InterruptedException ie) {        
      }
    }
  }
  
  /**
   * Load an instance of a UDF, possibly checking the cache.
   * 
   * @param name
   * @param cached
   * @return
   * @throws WarpScriptException
   */
  public static WarpScriptJavaFunction load(String name, boolean cached) throws WarpScriptException {
    
    WarpScriptJavaFunction udf = null;
    
    if (cached) {
      udf = cachedUDFs.get(name);
      
      if (null != udf && validate(udf)) {
        return udf;
      } else {
        // Clear from cache
        cachedUDFs.remove(name);
        udf = null;
      }
    }
    
    for (Entry<ClassLoader,String> entry: classLoadersFingerprints.entrySet()) {
      try {
        ClassLoader cl = entry.getKey();
        Class cls = cl.loadClass(name);
        
        Object o = cls.newInstance();
        
        if (!(o instanceof WarpScriptJavaFunction)) {
          throw new WarpScriptException(name + " does not appear to be of type " + WarpScriptJavaFunction.class.getCanonicalName());
        }
        
        udf = (WarpScriptJavaFunction) o;
        
        //
        // If the UDF was loaded from the class path class loader, wrap it so it is unprotected
        //
        
        if (cl.equals(classPathClassLoader)) {
          final WarpScriptJavaFunction innerUDF = udf;
          udf = new WarpScriptJavaFunction() {            
            @Override
            public boolean isProtected() { return false; }
            
            @Override
            public int argDepth() { return innerUDF.argDepth(); }
            
            @Override
            public List<Object> apply(List<Object> args) throws WarpScriptJavaFunctionException { return innerUDF.apply(args); }
          };
        }
        
        break;
      } catch (Exception e) {
        continue;
      }
    }
    
    if (cached && null != udf) {
      cachedUDFs.put(name, udf);
    }
    
    if (null == udf) {
      throw new WarpScriptException("Class '" + name + "' was not found in any of the current WarpScript jars.");
    }
    return udf;
  }
    
  /**
   * Validates an instance of EinsteinJavaFunction by checking that its class loader is still active 
   * @param func Instance to check
   * @return
   */
  private static boolean validate(WarpScriptJavaFunction func) {
    if (null == func) {
      return true;
    }

    return classLoadersFingerprints.containsKey(func.getClass().getClassLoader());
  }
  
  public static void init(Properties properties) {
    
    //
    // Extract root directory
    //
    
    String dir = properties.getProperty(Configuration.JARS_DIRECTORY);
    
    if (null == dir && !"true".equals(properties.getProperty(Configuration.JARS_FROMCLASSPATH))) {
      return;
    }
    
    //
    // Simply add a class loader to access the current classpath
    //
    
    if (null == dir) {
      classPathClassLoader = new ClassLoader(WarpScriptJarRepository.class.getClassLoader()) {
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {                
          if (name.startsWith("io.warp10") && !name.startsWith("io.warp10.warp.sdk.")) {
            throw new ClassNotFoundException();
          } else {
            return this.getParent().loadClass(name);
          }
        }
      };
      
      classLoadersFingerprints.put(classPathClassLoader, "");
    }
    
    //
    // Extract refresh interval
    //
    
    long delay = DEFAULT_DELAY;
    
    String refresh = properties.getProperty(Configuration.JARS_REFRESH);

    if (null != refresh) {
      try {
        delay = Long.parseLong(refresh.toString());
      } catch (Exception e) {            
      }
    }

    new WarpScriptJarRepository(dir, delay);
  }
}
