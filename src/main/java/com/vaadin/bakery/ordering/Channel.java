package com.vaadin.bakery.ordering;

public enum Channel {
    ONLINE, PHONE, COUNTER;

    public String translationKey() {
        return "ordering.channel." + name();
    }
}
