/**
 * Copyright © 2017, Christophe Marchand
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.xspec.maven.xspecMavenPlugin.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * A class to store various Saxon options
 * @author cmarchand
 */
public class SaxonOptions {
    
    /**
     * Represents the  collection-finder-class configuration property
     */
    private String  collectionFinderClass;
    /**
     * Represents the dtd configuration property
     */
    private String dtd;
    /**
     * Represents the ea configuration property
     */
    private String ea;
    /**
     * Represents the expand configuration property
     */
    private String expand;
    /**
     * Represents the ext configuration property
     */
    private String ext;
    /**
     * Represents the l configuration property
     */
    private String l;
    /**
     * Represents the m configuration property
     */
    private String m;
    /**
     * Represents the opt configuration property
     */
    private String opt;
    /**
     * Represents the or configuration property
     */
    private String or;
    /**
     * Represents the outval configuration property
     */
    private String outval;
    /**
     * Represents the strip configuration property
     */
    private String strip;
    /**
     * Represents the T configuration property
     */
    private String T;
    /**
     * Represents the TJ configuration property
     */
    private String TJ;
    /**
     * Represents the -tree configuration property
     */
    private String tree;
    /**
     * Represents the -val configuration property
     */
    private String val;
    /**
     * Represents the -warnings configuration property
     */
    private String warnings;
    /**
     * Represents the -xi configuration property
     */
    private String xi;
    
    public static final String SAXON_CONFIG_PROPERTY = " Saxon Configuration Property";

    public SaxonOptions() {
        super();
    }
    
    public void setCollectionFinderClass(final String collectionFinderClass) { this.collectionFinderClass=collectionFinderClass; }
    public void setDtd(final String dtd) { this.dtd=dtd; }
    public void setEa(final String ea) { this.ea=ea; }
    public void setExpand(final String expand) { this.expand=expand; }
    public void setExt(final String ext) { this.ext=ext; }
    public void setL(final String l) { this.l=l; }
    public void setM(final String m) { this.m=m; }
    public void setOpt(final String opt) { this.opt=opt; }
    public void setOr(final String or) { this.or=or; }
    public void setOutval(final String outval) { this.outval=outval; }
    public void setStrip(final String strip) { this.strip=strip; }
    public void setT(final String T) { this.T=T; }
    public void setTJ(final String TJ) { this.TJ=TJ; }
    public void setTree(final String tree) { this.tree=tree; }
    public void setVal(final String val) { this.val=val; }
    public void setWarnings(final String warnings) { this.warnings=warnings; }
    public void setXi(final String xi) { this.xi = xi; }
    
    protected void checkValue(final String optionName, final String value, String... vals) throws IllegalArgumentException {
        boolean isValid = false;
        for(String v: vals) {
            isValid = isValid || v.equals(value);
        }
        if(!isValid) {
            throw new IllegalArgumentException("Only "+Arrays.toString(vals)+" are valid values for "+optionName+SAXON_CONFIG_PROPERTY);
        }
    }
    
    public String getCollectionFinderClass() { return collectionFinderClass; }
    public String getDtd() throws IllegalArgumentException  { 
        if(dtd==null) return null;
        checkValue("dtd", dtd, "on", "off", "recover");
        return dtd;
    }
    public String getEa() throws IllegalArgumentException { 
        if(ea==null) return null;
        checkValue("ea", ea, "on", "off");
        return ea; 
    }
    public String getExpand() throws IllegalArgumentException {
        if(expand==null) return null;
        checkValue("expand", expand, "on", "off");
        return expand; 
    }
    public String getExt() throws IllegalArgumentException {
        if(ext==null) return null;
        checkValue("ext", ext, "on", "off");
        return ext;
    }
    public String getL() throws IllegalArgumentException {
        if(l==null) return null;
        checkValue("l", l, "on", "off");
        return l;
    }
    public String getM() { return m; }
    public String getOpt() { return opt; }
    public String getOr() { return or; }
    public String getOutval() throws IllegalArgumentException {
        if(outval==null) return null;
        checkValue("outval", outval, "recover", "fatal");
        return outval;
    }
    public String getStrip() throws IllegalArgumentException {
        if(strip==null) return null;
        checkValue("strip", strip, "all", "none", "ignorable");
        return strip;
    }
    public String getT() throws IllegalArgumentException {
        if(T==null) return null;
        checkValue("T", T, "on", "off");
        return T; 
    }
    public String getTJ() throws IllegalArgumentException {
        if(TJ==null) return null;
        checkValue("TJ", TJ, "on", "off");
        return TJ;
    }
    public String getTree() throws IllegalArgumentException {
        if(tree==null) return null;
        checkValue("tree", tree, "linked", "tiny", "tinyc");
        return tree;
    }
    public String getVal() throws IllegalArgumentException {
        if(val==null) return null;
        checkValue("val", val, "strict", "lax");
        return val;
    }
    public String getWarnings() throws IllegalArgumentException {
        if(warnings==null) return null;
        checkValue("warnings", warnings, "silent", "recover", "fatal");
        return warnings;
    }
    public String getXi() throws IllegalArgumentException {
        if(xi==null) return null;
        checkValue("xi", xi, "on", "off");
        return xi;
    }
    
    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf("SaxonOptions[\n" +
                "\tcollectionFinderClass=%s\n" +
                "\tdtd=%s\n" +
                "\tea=%s\n" +
                "\texpand=%s\n" +
                "\text=%s\n" +
                "\tL=%s\n" +
                "\tm=%s\n" +
                "\topt=%s\n" +
                "\tor=%s\n" +
                "\toutval=%s\n" +
                "\tstrip=%s\n" +
                "\tT=%s\n" +
                "\tTJ=%s\n" +
                "\ttree=%s\n" +
                "\tval=%s\n" +
                "\twarnings=%s\n" +
                "\txi=%s\n]",
            getCollectionFinderClass(),
            getDtd(),
            getEa(),
            getExpand(),
            getExt(),
            getL(),
            getM(),
            getOpt(),
            getOr(),
            getOutval(),
            getStrip(),
            getT(),
            getTJ(),
            getTree(),
            getVal(),
            getWarnings(),
            getXi()
        );
        pw.flush();
        return sw.getBuffer().toString();
    }
}
