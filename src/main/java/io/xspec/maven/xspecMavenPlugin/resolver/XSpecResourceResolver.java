package io.xspec.maven.xspecMavenPlugin.resolver;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.*;
import net.sf.saxon.trans.XPathException;
import org.apache.maven.plugin.logging.Log;
import org.xmlresolver.ResolverFeature;

import javax.xml.transform.Source;
import java.io.File;
import java.util.Collections;
import java.util.StringJoiner;

public class XSpecResourceResolver implements ResourceResolver {
  private final CpResolver first;
  private final CatalogResourceResolver second;
  private final ResourceResolver third;
  private final Log logger;

  //private final ChainedResourceResolver chainedResourceResolver;

  public XSpecResourceResolver(Configuration configuration, File catalog, Log logger) {
    this.logger=logger;
    first = new CpResolver(logger);
    second = new CatalogResourceResolver();
    second.setFeature(
        ResolverFeature.CATALOG_FILES,
        Collections.singletonList(catalog.toURI().toString())
    );
    third = new DirectResourceResolver(configuration);
    configuration.setResourceResolver(this);
    //chainedResourceResolver = new ChainedResourceResolver(first, second);
  }
  @Override
  public Source resolve(ResourceRequest resourceRequest) throws XPathException {
    logger.debug("resolve("+toString(resourceRequest)+")");
    Source ret = first.resolve(resourceRequest);
    if (ret != null) {
      logger.debug("   resolved by CP -> "+ret.getSystemId());
      return ret;
    } else {
      ret = second.resolve(resourceRequest);
    }
    if (ret != null) {
      logger.debug("   resolved by catalog -> "+ret.getSystemId());
      return ret;
    } else {
      ret = third.resolve(resourceRequest);
    }
    if (ret != null) {
      logger.debug("   resolved by direct -> "+ret.getSystemId());
      return ret;
    }
    outputRequest(resourceRequest);
    logger.error("Dans le cul !");
    return null;
  }

  private String toString(ResourceRequest resourceRequest) {
    StringJoiner j = new StringJoiner(",","{","}");
    if(resourceRequest.uri!=null) j.add("uri: " + resourceRequest.uri);
    if(resourceRequest.baseUri!=null) j.add("baseUri: " + resourceRequest.baseUri);
    if(resourceRequest.relativeUri!=null) j.add("relativeUri: " + resourceRequest.relativeUri);
    if(resourceRequest.publicId!=null) j.add("publicId: " + resourceRequest.publicId);
    if(resourceRequest.entityName!=null) j.add("entityName: " + resourceRequest.entityName);
    if(resourceRequest.nature!=null) j.add("nature: " + resourceRequest.nature);
    if(resourceRequest.purpose!=null) j.add("purpose: " + resourceRequest.purpose);
    return j.toString();
  }

  public static ResourceRequest buildRequest(String href, String base) {
    ResourceRequest resourceRequest = new ResourceRequest();
    resourceRequest.uri = href;
    resourceRequest.baseUri = base;
    return resourceRequest;
  }
  private void outputRequest(ResourceRequest resourceRequest) {
    String ret = "resourceRequest:\n";
    ret += "   uri:            " + resourceRequest.uri + "\n";
    ret += "   baseUri:        " + resourceRequest.baseUri + "\n";
    ret += "   relativeUri:    " + resourceRequest.relativeUri + "\n";
    ret += "   publicId:       " + resourceRequest.publicId + "\n";
    ret += "   entityName:     " + resourceRequest.entityName + "\n";
    ret += "   nature:         " + resourceRequest.nature + "\n";
    ret += "   purpose:        " + resourceRequest.purpose + "\n";
    ret += "   uriIsNamespace: " + resourceRequest.uriIsNamespace + "\n";
    logger.debug(ret);
  }

}
