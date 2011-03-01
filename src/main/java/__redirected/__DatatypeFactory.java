/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package __redirected;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A redirecting DatatypeFactory
 *
 * @author Jason T. Greene
 */
@SuppressWarnings("unchecked")
public final class __DatatypeFactory extends DatatypeFactory {
    private static final Constructor<? extends DatatypeFactory> PLATFORM_FACTORY;
    private static volatile Constructor<? extends DatatypeFactory> DEFAULT_FACTORY;

    static {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(null);
        try {
            DatatypeFactory factory = DatatypeFactory.newInstance();
            try {
                DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(DatatypeFactory.class.getName(), __DatatypeFactory.class.getName());
        } catch (DatatypeConfigurationException e) {
            throw __RedirectedUtils.wrapped(new NoClassDefFoundError(e.getMessage()), e);
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        Class<? extends DatatypeFactory> clazz = __RedirectedUtils.loadProvider(id, DatatypeFactory.class, loader);
        if (clazz != null) {
            try {
                DEFAULT_FACTORY = clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
        }
    }

    public static void restorePlatformFactory() {
        DEFAULT_FACTORY = PLATFORM_FACTORY;
    }

    /**
     * Init method.
     */
    public static void init() {}

    /**
     * Construct a new instance.
     */
    public __DatatypeFactory() {
        Constructor<? extends DatatypeFactory> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            if (loader != null) {
                Class<? extends DatatypeFactory> provider = __RedirectedUtils.loadProvider(DatatypeFactory.class, loader);
                if (provider != null)
                    factory = provider.getConstructor();
            }

            actual = factory.newInstance();
        } catch (InstantiationException e) {
            throw __RedirectedUtils.wrapped(new InstantiationError(e.getMessage()), e);
        } catch (IllegalAccessException e) {
            throw __RedirectedUtils.wrapped(new IllegalAccessError(e.getMessage()), e);
        } catch (InvocationTargetException e) {
            throw __RedirectedUtils.rethrowCause(e);
        } catch (NoSuchMethodException e) {
            throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
        }
    }

    private final DatatypeFactory actual;

    public Duration newDuration(String lexicalRepresentation) {
        return actual.newDuration(lexicalRepresentation);
    }

    public String toString() {
        return actual.toString();
    }

    public Duration newDuration(long durationInMilliSeconds) {
        return actual.newDuration(durationInMilliSeconds);
    }

    public Duration newDuration(boolean isPositive, BigInteger years, BigInteger months, BigInteger days, BigInteger hours,
            BigInteger minutes, BigDecimal seconds) {
        return actual.newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    public Duration newDuration(boolean isPositive, int years, int months, int days, int hours, int minutes, int seconds) {
        return actual.newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    public Duration newDurationDayTime(String lexicalRepresentation) {
        return actual.newDurationDayTime(lexicalRepresentation);
    }

    public Duration newDurationDayTime(long durationInMilliseconds) {
        return actual.newDurationDayTime(durationInMilliseconds);
    }

    public Duration newDurationDayTime(boolean isPositive, BigInteger day, BigInteger hour, BigInteger minute, BigInteger second) {
        return actual.newDurationDayTime(isPositive, day, hour, minute, second);
    }

    public Duration newDurationDayTime(boolean isPositive, int day, int hour, int minute, int second) {
        return actual.newDurationDayTime(isPositive, day, hour, minute, second);
    }

    public Duration newDurationYearMonth(String lexicalRepresentation) {
        return actual.newDurationYearMonth(lexicalRepresentation);
    }

    public Duration newDurationYearMonth(long durationInMilliseconds) {
        return actual.newDurationYearMonth(durationInMilliseconds);
    }

    public Duration newDurationYearMonth(boolean isPositive, BigInteger year, BigInteger month) {
        return actual.newDurationYearMonth(isPositive, year, month);
    }

    public Duration newDurationYearMonth(boolean isPositive, int year, int month) {
        return actual.newDurationYearMonth(isPositive, year, month);
    }

    public XMLGregorianCalendar newXMLGregorianCalendar() {
        return actual.newXMLGregorianCalendar();
    }

    public XMLGregorianCalendar newXMLGregorianCalendar(String lexicalRepresentation) {
        return actual.newXMLGregorianCalendar(lexicalRepresentation);
    }

    public XMLGregorianCalendar newXMLGregorianCalendar(GregorianCalendar cal) {
        return actual.newXMLGregorianCalendar(cal);
    }

    public XMLGregorianCalendar newXMLGregorianCalendar(BigInteger year, int month, int day, int hour, int minute, int second,
            BigDecimal fractionalSecond, int timezone) {
        return actual.newXMLGregorianCalendar(year, month, day, hour, minute, second, fractionalSecond, timezone);
    }

    public XMLGregorianCalendar newXMLGregorianCalendar(int year, int month, int day, int hour, int minute, int second,
            int millisecond, int timezone) {
        return actual.newXMLGregorianCalendar(year, month, day, hour, minute, second, millisecond, timezone);
    }

    public XMLGregorianCalendar newXMLGregorianCalendarDate(int year, int month, int day, int timezone) {
        return actual.newXMLGregorianCalendarDate(year, month, day, timezone);
    }

    public XMLGregorianCalendar newXMLGregorianCalendarTime(int hours, int minutes, int seconds, int timezone) {
        return actual.newXMLGregorianCalendarTime(hours, minutes, seconds, timezone);
    }

    public XMLGregorianCalendar newXMLGregorianCalendarTime(int hours, int minutes, int seconds, BigDecimal fractionalSecond,
            int timezone) {
        return actual.newXMLGregorianCalendarTime(hours, minutes, seconds, fractionalSecond, timezone);
    }

    public XMLGregorianCalendar newXMLGregorianCalendarTime(int hours, int minutes, int seconds, int milliseconds, int timezone) {
        return actual.newXMLGregorianCalendarTime(hours, minutes, seconds, milliseconds, timezone);
    }
}
