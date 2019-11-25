package com.clemcab.diskoverindoors;

public class IndoorLocation {
    public String building;
    public int level;
    public String title;
    public float x_coord;
    public float y_coord;

    IndoorLocation (String building, int level, String title, float x_coord, float y_coord) {
        this.building = building;
        this.level = level;
        this.title = title;
        this.x_coord = x_coord;
        this.y_coord = y_coord;
    }
}
