package org.jboss.modules.management;
//Extract class refactoring 
public class Property {
    private final String key;
    private final String value;

    public Property(final String key, final String value) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
