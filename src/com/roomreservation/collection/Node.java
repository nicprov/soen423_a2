package com.roomreservation.collection;

public class Node<K,V> implements Entry<K,V>{
    private K k;
    private V v;

    public Node(K key, V value){
        k = key;
        v = value;
    }

    public K getKey(){
        return k;
    }

    public V getValue(){
        return v;
    }

    protected void setKey(K key){
        k = key;
    }

    protected V setValue(V value) {
        V old = v;
        v = value;
        return old;
    }
}
