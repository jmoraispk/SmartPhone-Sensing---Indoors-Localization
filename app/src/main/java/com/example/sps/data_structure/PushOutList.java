package com.example.sps.data_structure;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.concurrent.CopyOnWriteArrayList;

public class PushOutList<T> implements List<T>, RandomAccess {

    CopyOnWriteArrayList<T> list = new CopyOnWriteArrayList<>();
    private int maxsize;

    public PushOutList(int maxsize) {
        this.maxsize = maxsize;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] t1s) {
        return list.toArray(t1s);
    }

    @Override
    public boolean add(T t) {
        list.add(t);
        if(list.size() >= maxsize+1)
            list.remove(0);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return list.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        int toRemove = list.size() + collection.size() - maxsize;
        for(int i = 0; i < toRemove; i++)
            list.remove(0);
        return list.addAll(collection);
    }

    @Override
    public boolean addAll(int i, @NonNull Collection<? extends T> collection) {
        int toRemove = list.size() + collection.size() - maxsize;
        for(int j = 0; j < toRemove; j++)
            list.remove(0);
        return list.addAll(i, collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return list.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return list.retainAll(collection);
    }

    @Override
    public void clear() {
        list = new CopyOnWriteArrayList<>();
    }

    @Override
    public T get(int i) {
        return list.get(i);
    }

    @Override
    public T set(int i, T t) {
        return list.set(i, t);
    }

    @Override
    public void add(int i, T t) {
        if(list.size() >= maxsize)
            list.remove(0);
        list.add(i, t);
    }

    @Override
    public T remove(int i) {
        return list.remove(i);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator(int i) {
        return list.listIterator(i);
    }

    @NonNull
    @Override
    public List<T> subList(int i, int i1) {
        return list.subList(i, i1);
    }
}
