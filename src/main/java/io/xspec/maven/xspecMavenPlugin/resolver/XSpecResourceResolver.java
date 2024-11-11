package io.xspec.maven.xspecMavenPlugin.resolver;

import net.sf.saxon.lib.CatalogResourceResolver;
import net.sf.saxon.lib.ChainedResourceResolver;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.trans.XPathException;
import org.apache.maven.plugin.logging.Log;
import org.xmlresolver.ResolverFeature;

import javax.xml.transform.Source;
import java.io.File;
import java.util.Collections;

public class XSpecResourceResolver implements ResourceResolver {

  private final ChainedResourceResolver chainedResourceResolver;

  public XSpecResourceResolver(File catalog, Log logger) {
    CatalogResourceResolver catalogResourceResolver = new CatalogResourceResolver();
    catalogResourceResolver.setFeature(
        ResolverFeature.CATALOG_FILES,
        Collections.singletonList(catalog.toURI().toString())
    );
    chainedResourceResolver = new ChainedResourceResolver(new CpResolver(logger), catalogResourceResolver);
  }
  @Override
  public Source resolve(ResourceRequest resourceRequest) throws XPathException {
    return chainedResourceResolver.resolve(resourceRequest);
  }

  public static ResourceRequest buildRequest(String href, String base) {
    ResourceRequest resourceRequest = new ResourceRequest();
    resourceRequest.relativeUri = href;
    resourceRequest.baseUri = base;
    return resourceRequest;
  }

}
