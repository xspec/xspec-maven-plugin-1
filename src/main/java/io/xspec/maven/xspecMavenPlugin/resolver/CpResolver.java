package io.xspec.maven.xspecMavenPlugin.resolver;

import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.trans.XPathException;
import org.apache.maven.plugin.logging.Log;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A <a href="https://www.saxonica.com/documentation12/index.html#!javadoc/net.sf.saxon.lib/ResourceResolver">ResourceResolver</a> that resolves
 * {@literal cp:/package/resource} URI from classpath
 */
public class CpResolver implements ResourceResolver {
  private final Pattern protocolPattern;
  private final Log log;

  public CpResolver(Log log) {
    super();
    this.log = log;
    this.protocolPattern = Pattern.compile("^[\\w\\d]+:/.*");
  }

  @Override
  public Source resolve(ResourceRequest resourceRequest) throws XPathException {
    String href = resourceRequest.uri;
    String base = resourceRequest.baseUri;
    Source ret = (isCpProtocol(href, base)) ? resolveToClasspath(href, base) : null;
    return ret;
  }

  private boolean isCpProtocol(String href, String base) {
    return (href != null && href.startsWith("cp:/")) ||
        (base != null && base.startsWith("cp:/"));
  }

  private Source resolveToClasspath(String href, String base) {
    String fullUrl = isAbsolute(href) ? href : base + href;
    String path = removeCpPrefix(fullUrl);
    InputStream is = getClass().getResourceAsStream(path);
    if (is == null) {
      return null;
    }
    StreamSource ret = new StreamSource(is);
    String systemId = getClass().getResource(path).toExternalForm();
    //log.debug(systemId);
    ret.setSystemId(normalizeUrl(systemId));
    return ret;
  }

  private boolean isAbsolute(String href) {
    return protocolPattern.matcher(href).matches();
  }

  private String removeCpPrefix(String fullUrl) {
    return fullUrl.substring(3);
  }

  private String normalizeUrl(String systemId) {
    try {
      URL url = new URL(systemId);
      String protocol = url.getProtocol();
      String path = url.getPath();
      String host = url.getHost();
      return new URL(protocol, host, path.replaceAll("\\/\\/", "/")).toExternalForm();
    } catch (MalformedURLException ex) {
      // impossible !
      return systemId;
    }
  }

}
