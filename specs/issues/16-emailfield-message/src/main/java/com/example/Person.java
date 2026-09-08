package com.example;

import jakarta.validation.constraints.Email;

/** One property, one constraint. */
public class Person {

    @Email
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
