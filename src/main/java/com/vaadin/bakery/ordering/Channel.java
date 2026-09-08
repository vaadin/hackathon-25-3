package com.vaadin.bakery.ordering;

public enum Channel {
    ONLINE, PHONE, EMAIL, COUNTER;

    public String translationKey() {
        return "ordering.channel." + name();
    }
}
