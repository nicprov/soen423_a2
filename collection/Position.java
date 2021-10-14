package collection;

public interface Position<E> {

    E getElement() throws IllegalStateException;
}