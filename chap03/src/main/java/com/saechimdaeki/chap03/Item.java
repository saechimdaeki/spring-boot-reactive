package com.saechimdaeki.chap03;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Item {
    private  @Id String id;
    private String name;
    private double price;

    private Item(){}

    public Item(String name, double price) {
        this.name = name;
        this.price = price;
    }
}
