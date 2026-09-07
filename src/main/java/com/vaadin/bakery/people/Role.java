package com.vaadin.bakery.people;

/**
 * Staff roles. An enum, not the free string of the old Bakery, so a typo cannot
 * invent a role nobody has.
 */
public enum Role {
    ADMIN, BAKER, BARISTA;

    public static final String ADMIN_NAME = "ADMIN";
    public static final String BAKER_NAME = "BAKER";
    public static final String BARISTA_NAME = "BARISTA";

    public String authority() {
        return "ROLE_" + name();
    }

    public String translationKey() {
        return "people.role." + name();
    }
}
