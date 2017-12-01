/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.modules;

import java.net.URL;

/**
 * A specification for a package to define.
 *
 * @apiviz.exclude
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PackageSpec {

    private String specTitle;
    private String specVersion;
    private String specVendor;
    private String implTitle;
    private String implVersion;
    private String implVendor;
    private URL sealBase;
    private AssertionSetting assertionSetting = AssertionSetting.INHERIT;

    /**
     * Get the package specification title.
     *
     * @return the specification title
     * @see java.util.jar.Attributes.Name#SPECIFICATION_TITLE
     */
    public String getSpecTitle() {
        return specTitle;
    }

    /**
     * Set the package specification title.
     *
     * @param specTitle the specification title
     * @see java.util.jar.Attributes.Name#SPECIFICATION_TITLE
     */
    public PackageSpec setSpecTitle(String specTitle) {
        this.specTitle = specTitle;
        return this;
    }

    public void setSpecTitle$$bridge(String specTitle) {
        setSpecTitle(specTitle);
    }

    /**
     * Get the package specification version.
     *
     * @return the specification version
     * @see java.util.jar.Attributes.Name#SPECIFICATION_VERSION
     */
    public String getSpecVersion() {
        return specVersion;
    }

    /**
     * Set the package specification version.
     *
     * @param specVersion the specification version
     * @see java.util.jar.Attributes.Name#SPECIFICATION_VERSION
     */
    public PackageSpec setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
        return this;
    }

    public void setSpecVersion$$bridge(String specVersion) {
        setSpecVersion(specVersion);
    }

    /**
     * Set the package specification vendor.
     *
     * @return the specification vendor
     * @see java.util.jar.Attributes.Name#SPECIFICATION_VENDOR
     */
    public String getSpecVendor() {
        return specVendor;
    }

    /**
     * Set the package specification vendor.
     *
     * @param specVendor the specification vendor
     * @see java.util.jar.Attributes.Name#SPECIFICATION_VENDOR
     */
    public PackageSpec setSpecVendor(String specVendor) {
        this.specVendor = specVendor;
        return this;
    }

    public void setSpecVendor$$bridge(String specVendor) {
        setSpecVendor(specVendor);
    }

    /**
     * Get the implementation title.
     *
     * @return the implementation title
     * @see java.util.jar.Attributes.Name#IMPLEMENTATION_TITLE
     */
    public String getImplTitle() {
        return implTitle;
    }

    /**
     * Set the implementation title.
     *
     * @param implTitle the implementation title
     * @see java.util.jar.Attributes.Name#IMPLEMENTATION_TITLE
     */
    public PackageSpec setImplTitle(String implTitle) {
        this.implTitle = implTitle;
        return this;
    }

    public void setImplTitle$$bridge(String implTitle) {
        setImplTitle(implTitle);
    }

    /**
     * Get the implementation version.
     *
     * @return the implementation version
     * @see java.util.jar.Attributes.Name#IMPLEMENTATION_VERSION
     */
    public String getImplVersion() {
        return implVersion;
    }

    /**
     * Set the implementation version.
     *
     * @param implVersion the implementation version
     * @see java.util.jar.Attributes.Name#IMPLEMENTATION_VENDOR
     */
    public PackageSpec setImplVersion(String implVersion) {
        this.implVersion = implVersion;
        return this;
    }

    public void setImplVersion$$bridge(String implVersion) {
        setImplVersion(implVersion);
    }

    /**
     * Get the implementation vendor.
     *
     * @return the implementation vendor
     * @see java.util.jar.Attributes.Name#IMPLEMENTATION_VENDOR
     */
    public String getImplVendor() {
        return implVendor;
    }

    /**
     * Set the implementation vendor.
     *
     * @param implVendor the implementation vendor
     * @see java.util.jar.Attributes.Name#IMPLEMENTATION_VENDOR
     */
    public PackageSpec setImplVendor(String implVendor) {
        this.implVendor = implVendor;
        return this;
    }

    public void setImplVendor$$bridge(String implVendor) {
        setImplVendor(implVendor);
    }

    /**
     * Get the URL against which this package is sealed.
     *
     * @return the seal base URL
     * @see java.util.jar.Attributes.Name#SEALED
     */
    public URL getSealBase() {
        return sealBase;
    }

    /**
     * Set the URL against which this package is sealed.
     *
     * @param sealBase the seal base URL
     * @see java.util.jar.Attributes.Name#SEALED
     */
    public void setSealBase(URL sealBase) {
        this.sealBase = sealBase;
    }

    /**
     * Get the package assertion setting.
     *
     * @return the package assertion setting
     */
    public AssertionSetting getAssertionSetting() {
        return assertionSetting;
    }

    /**
     * Set the package assertion setting.
     *
     * @param assertionSetting the package assertion setting
     */
    public PackageSpec setAssertionSetting(AssertionSetting assertionSetting) {
        if (assertionSetting == null) {
            throw new IllegalArgumentException("assertionSetting is null");
        }
        this.assertionSetting = assertionSetting;
        return this;
    }

    public void setAssertionSetting$$bridge(AssertionSetting assertionSetting) {
        setAssertionSetting(assertionSetting);
    }
}