package com.dehigher.biutils.activecontract.dto;

public class TopItemElement implements Comparable<TopItemElement> {

    private TopItem item;

    private int priority;

    public TopItemElement(TopItem item, int priority) {
        this.item = item;
        this.priority = priority;
    }


    public TopItem getItem(){
        return item;
    }

    @Override
    public int compareTo(TopItemElement other) {
        return Integer.compare(this.priority, other.priority);
    }

}