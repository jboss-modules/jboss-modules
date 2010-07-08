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

    public String getSpecTitle() {
        return specTitle;
    }

    public void setSpecTitle(final String specTitle) {
        this.specTitle = specTitle;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(final String specVersion) {
        this.specVersion = specVersion;
    }

    public String getSpecVendor() {
        return specVendor;
    }

    public void setSpecVendor(final String specVendor) {
        this.specVendor = specVendor;
    }

    public String getImplTitle() {
        return implTitle;
    }

    public void setImplTitle(final String implTitle) {
        this.implTitle = implTitle;
    }

    public String getImplVersion() {
        return implVersion;
    }

    public void setImplVersion(final String implVersion) {
        this.implVersion = implVersion;
    }

    public String getImplVendor() {
        return implVendor;
    }

    public void setImplVendor(final String implVendor) {
        this.implVendor = implVendor;
    }

    public URL getSealBase() {
        return sealBase;
    }

    public void setSealBase(final URL sealBase) {
        this.sealBase = sealBase;
    }

    public AssertionSetting getAssertionSetting() {
        return assertionSetting;
    }

    public void setAssertionSetting(final AssertionSetting assertionSetting) {
        if (assertionSetting == null) {
            throw new IllegalArgumentException("assertionSetting is null");
        }
        this.assertionSetting = assertionSetting;
    }
}