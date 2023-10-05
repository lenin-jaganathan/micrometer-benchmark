package org.example.internal;

public class FixedArgsCacheKey {
    private final String arg1;
    private final String arg2;

    public FixedArgsCacheKey(String arg1, String arg2) {
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            FixedArgsCacheKey meterKey = (FixedArgsCacheKey)o;
            return this.arg1.equals(meterKey.arg1) &&
                    this.arg2.equals(meterKey.arg2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (arg1 == null ? 0 : arg1.hashCode());
        result = 31 * result + (arg2 == null ? 0 : arg2.hashCode());
        return result;
    }
}
