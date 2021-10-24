package com.zackmurry.rtree;

import java.awt.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello, world");
        RTree<String> rtree = new RTree<>();
        for (int i = 0; i < 100; i++) {
            rtree.insert(new Rectangle((int) Math.floor(Math.random() * 100), (int) Math.floor(Math.random() * 100), 20, 10), "Object" + i);
            System.out.println(i);
            if (rtree.size() != i + 1) {
                System.out.println("i: " + i + "; size: " + rtree.size());
            }
        }
        System.out.println(rtree.search(new Rectangle(10, 10, 50, 60)).size());
        System.out.println(rtree);
    }

}
