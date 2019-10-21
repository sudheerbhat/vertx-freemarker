package org.vertx.templates.freemarker;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import java.io.File;
import java.io.PrintWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class VertxCacheTest {

  private static Vertx vertx;

  @BeforeClass
  public static void before() {
    vertx = Vertx.vertx(new VertxOptions().setFileSystemOptions(new FileSystemOptions().setFileCachingEnabled(false)));
  }

  @Test
  public void includedTemplatesAreNotCached(TestContext should) throws Exception {
    final Async test = should.async();
    TemplateEngine templateEngine = FreeMarkerTemplateEngine.create(vertx);

    File child = File.createTempFile("child", ".ftl", new File("target/classes"));
    child.deleteOnExit();
    final PrintWriter childWriter = new PrintWriter(child);
    childWriter.print("child");
    childWriter.flush();
    childWriter.close();

    PrintWriter out;
    File root = File.createTempFile("child", ".ftl", new File("target/classes"));
    root.deleteOnExit();

    out = new PrintWriter(root);
    out.print("root:");
    out.print("<#include \"" + child.getName() + "\" />");
    out.flush();
    out.close();

    templateEngine.render(new JsonObject(), root.getName(), render -> {
      assertTrue(render.succeeded());
      final String actual = render.result().toString();
      assertEquals("Expected root:child but got " + actual, "root:child", actual);

      try {
        final PrintWriter childWriter1 = new PrintWriter(child);
        childWriter1.print("modified the child");
        childWriter1.flush();
        childWriter1.close();
      } catch (Exception ex) {
        //ignore
        ex.printStackTrace();
      }

      templateEngine.render(new JsonObject(), root.getName(), render1 -> {
        assertTrue(render1.succeeded());
        final String actual1 = render1.result().toString();
        assertEquals("Expected root:child but got " + actual1, "root:child", actual1);
        test.complete();
      });
    });
    test.await();
  }

}
