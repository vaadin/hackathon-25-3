package com.vaadin.bakery.ordering;

/** What the cart holds. Product identity plus intent, never a detached entity. */
public record CartLine(Long productId, int quantity, String comment) {

    public CartLine withQuantity(int newQuantity) {
        return new CartLine(productId, newQuantity, comment);
    }

    public CartLine withComment(String newComment) {
        return new CartLine(productId, quantity, newComment);
    }
}
