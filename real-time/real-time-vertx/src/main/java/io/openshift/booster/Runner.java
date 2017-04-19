package io.openshift.booster;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class Runner {

//   private static final String DIR = "board-rest";
//   private static final String JAVA_DIR = DIR + "/src/test/java/";

   private final String javaDir;

   public Runner(String javaDir) {
      this.javaDir = javaDir;
   }

   public void runClusteredExample(Class clazz) {
      runExample(javaDir, clazz, new VertxOptions().setClustered(true), null);
   }

   public void runExample(Class clazz) {
      runExample(javaDir, clazz, new VertxOptions().setClustered(false), null);
   }

   public void runExample(Class clazz, DeploymentOptions options) {
      runExample(javaDir, clazz, new VertxOptions().setClustered(false), options);
   }

   public static void runExample(String exampleDir, Class clazz, VertxOptions options, DeploymentOptions
         deploymentOptions) {
      runExample(exampleDir + clazz.getPackage().getName().replace(".", "/"), clazz.getName(), options, deploymentOptions);
   }

   public static void runExample(String exampleDir, String verticleID, VertxOptions options, DeploymentOptions deploymentOptions) {
      if (options == null) {
         // Default parameter
         options = new VertxOptions();
      }
      // Smart cwd detection

      // Based on the current directory (.) and the desired directory (exampleDir), we try to compute the vertx.cwd
      // directory:
      try {
         // We need to use the canonical file. Without the file name is .
         File current = new File(".").getCanonicalFile();
         if (exampleDir.startsWith(current.getName()) && !exampleDir.equals(current.getName())) {
            exampleDir = exampleDir.substring(current.getName().length() + 1);
         }
      } catch (IOException e) {
         // Ignore it.
      }

      System.setProperty("vertx.cwd", exampleDir);
      Consumer<Vertx> runner = vertx -> {
         try {
            if (deploymentOptions != null) {
               vertx.deployVerticle(verticleID, deploymentOptions);
            } else {
               vertx.deployVerticle(verticleID);
            }
         } catch (Throwable t) {
            t.printStackTrace();
         }
      };
      if (options.isClustered()) {
         Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
               Vertx vertx = res.result();
               runner.accept(vertx);
            } else {
               res.cause().printStackTrace();
            }
         });
      } else {
         Vertx vertx = Vertx.vertx(options);
         runner.accept(vertx);
      }
   }
}