package com.alexecollins.docker.orchestration.model;

public class VolumeFrom {

    private final String value;

    public VolumeFrom(String value) {
        this.value = value;
    }

    public Id getId() {
        return new Id(value.replaceFirst(":.*", ""));
    }

    public boolean isReadWrite() {
        return !value.contains(":") || "rw".equals(value.replaceFirst(".*:", ""));
    }

    public boolean isReadOnly() {
        return value.contains(":") && "ro".equals(value.replaceFirst(".*:", ""));
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        VolumeFrom volume = (VolumeFrom) o;
        return value.equals(volume.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
