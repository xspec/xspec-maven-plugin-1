package io.xspec.maven.xspecMavenPlugin.resources.impl;

import io.xspec.maven.xspecMavenPlugin.resolver.ResolverS9;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.utils.CatalogWriter;
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
    public void given_default_xspec_impl_res_abnd_standard_catalog_compile_xsl_should_be_resolved() throws Exception {
        // Given
        CatalogWriter catalogWriter = new CatalogWriter(DefaultXSpecImplResources.class.getClassLoader());
        File catalogFile = catalogWriter.writeCatalog(null, null, false);
        ResolverS9 resolver = new ResolverS9(saxonResolver, catalogFile, log);
        XSpecImplResources resources = new DefaultXSpecImplResources();

        // When
        Source ret = resolver.resolve(resources.getXSpecXslCompilerUri(), null);

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
