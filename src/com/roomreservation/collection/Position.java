package com.roomreservation.collection;

public interface Position<E> {

    E getElement() throws IllegalStateException;
}