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

package __redirected;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.function.Supplier;

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
    private static final Supplier<DatatypeFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformDatatypeFactorySupplier();
    private static volatile Supplier<DatatypeFactory> DEFAULT_FACTORY = PLATFORM_FACTORY;

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<DatatypeFactory> supplier = __RedirectedUtils.loadProvider(id, DatatypeFactory.class, loader);
        if (supplier != null) {
            DEFAULT_FACTORY = supplier;
        }
    }

    public static void restorePlatformFactory() {
        DEFAULT_FACTORY = PLATFORM_FACTORY;
    }

    /**
     * Init method.
     */
    @Deprecated
    public static void init() {}

    /**
     * Construct a new instance.
     */
    public __DatatypeFactory() {
        actual = DEFAULT_FACTORY.get();
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
