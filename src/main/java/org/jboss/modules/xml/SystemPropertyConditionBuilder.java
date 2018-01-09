package org.jboss.modules.xml;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

class SystemPropertyConditionBuilder {

    private final List<Condition> conditions = new ArrayList<>();

    public boolean resolve() {
        for(Condition condition : conditions) {
            if(!condition.get()) {
                return false;
            }
        }
        return true;
    }

    public SystemPropertyConditionBuilder add(String name, String value, boolean equal) {
        conditions.add(new Condition(name, value, equal));
        return this;
    }

    private static class Condition {
        private final String name;
        private final String value;
        private final boolean equal;

        private Condition(String name, String value, boolean equal) {
            this.name = name;
            this.value = value;
            this.equal = equal;
        }

        public boolean get() {
            String pval = System.getProperty(name);
            boolean equalValue;
            if(pval == null) {
                equalValue = value.isEmpty(); //treat the empty string as equal to null
            } else {
                equalValue = value.equals(pval);
            }
            return equal == equalValue;
        }
    }
}
