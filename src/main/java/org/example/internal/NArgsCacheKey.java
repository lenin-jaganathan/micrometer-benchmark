package org.example.internal;

import java.util.Arrays;

public class NArgsCacheKey {
    private final String[] args;

    public NArgsCacheKey(String[] args) {
        this.args = args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            return Arrays.equals(this.args, ((NArgsCacheKey) o).args);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.args);
    }
}
