package com.dehigher.biutils.base;



import java.util.ArrayList;
import java.util.List;

public class FixedSizeQueue<T> {
    private Object[] queue;
    private int front;  // 指向队列头部
    private int rear;   // 指向队列尾部
    private int size;   // 当前队列元素个数
    private int capacity;  // 队列容量


    public <T> FixedSizeQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("队列容量必须大于0");
        }
        this.capacity = capacity;
        this.queue = new Object[capacity];
        this.front = 0;
        this.rear = 0;
        this.size = 0;
    }

    public void enqueue(List<T> items){
        for(T item: items){
            enqueue(item);
        }
    }

    public void enqueue(T item) {
        if (size == capacity) {
            // 队列已满，淘汰最先加入的数据
            dequeue();
        }
        queue[rear] = item;
        rear = (rear + 1) % capacity;
        size++;
    }

    public List<T> getAllElements() {
        List<T> elements = new ArrayList<>();
        int index = front;
        for (int i = 0; i < size; i++) {
            elements.add((T) queue[index]);
            index = (index + 1) % capacity;
        }
        return elements;
    }

    public void reset(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("队列容量必须大于0");
        }
        this.capacity = capacity;
        this.queue = new Object[capacity];
        this.front = 0;
        this.rear = 0;
        this.size = 0;
    }

    public T dequeue() {
        if (isEmpty()) {
            return null;
        }
        T item = (T) queue[front];
        queue[front] = null;  // 清空原始数据，协助垃圾回收
        front = (front + 1) % capacity;
        size--;
        return item;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }




}
