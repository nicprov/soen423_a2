package com.roomreservation.collection;

public interface Entry<K,V> {
    K getKey();

    V getValue();
}