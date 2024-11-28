package io.xspec.maven.xspecMavenPlugin.resources.impl;

import io.xspec.maven.xspecMavenPlugin.resolver.XSpecResourceResolver;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.utils.CatalogWriter;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.StandardURIResolver;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import java.io.File;

public class DefaultXSpecImplResourceResolveTest {

  @Test
  public void given_default_xspec_impl_res_and_standard_catalog_compile_xsl_should_be_resolved() throws Exception {
    // Given
    Configuration configuration = new Configuration();
    CatalogWriter catalogWriter = new CatalogWriter(DefaultXSpecImplResources.class.getClassLoader());
    File catalogFile = catalogWriter.writeCatalog(null, null, false);
    XSpecResourceResolver xSpecResourceResolver = new XSpecResourceResolver(configuration, catalogFile, log);
    XSpecImplResources resources = new DefaultXSpecImplResources();
    ResourceRequest resourceRequest = new ResourceRequest();
    resourceRequest.uri = resources.getXSpecXslCompilerUri();

    // When
    Source ret = xSpecResourceResolver.resolve(resourceRequest);

    // Then
    SoftAssertions softAssertions = new SoftAssertions();
    softAssertions.assertThat(ret).isNotNull();
  }


  private URIResolver saxonResolver;
  private Log log = new SystemStreamLog();

  @Before
  public void before() {
    saxonResolver = new StandardURIResolver();
  }

  @After
  public void after() {
    saxonResolver = null;
  }
}
