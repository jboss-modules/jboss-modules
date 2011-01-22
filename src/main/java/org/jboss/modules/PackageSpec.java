/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
    public void setSpecTitle(final String specTitle) {
        this.specTitle = specTitle;
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
    public void setSpecVersion(final String specVersion) {
        this.specVersion = specVersion;
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
    public void setSpecVendor(final String specVendor) {
        this.specVendor = specVendor;
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
    public void setImplTitle(final String implTitle) {
        this.implTitle = implTitle;
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
    public void setImplVersion(final String implVersion) {
        this.implVersion = implVersion;
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
    public void setImplVendor(final String implVendor) {
        this.implVendor = implVendor;
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
    public void setAssertionSetting(final AssertionSetting assertionSetting) {
        if (assertionSetting == null) {
            throw new IllegalArgumentException("assertionSetting is null");
        }
        this.assertionSetting = assertionSetting;
    }
}