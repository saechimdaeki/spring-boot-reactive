package com.saechimdaeki.chap04;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Data
public class Item {

    private @Id
    String id;
    private String name;
    private String description;
    private double price;
    // end::code[]

    private Item() {
    }

    Item(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    Item(String id, String name, String description, double price) {
        this(name, description, price);
        this.id = id;
    }
}
