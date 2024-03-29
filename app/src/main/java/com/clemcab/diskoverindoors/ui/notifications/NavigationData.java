package com.clemcab.diskoverindoors.ui.notifications;

public class NavigationData {
    public String building;
    public float start_x;
    public float start_y;
    public int start_floor;
    public float dest_x;
    public float dest_y;
    public int dest_floor;
    public String dest_room;

    NavigationData (String building, float start_x, float start_y, int start_floor, float dest_x, float dest_y, int dest_floor, String dest_room) {
        this.building = building;
        this.start_x = start_x;
        this.start_y = start_y;
        this.start_floor = start_floor;
        this.dest_x = dest_x;
        this.dest_y = dest_y;
        this.dest_floor = dest_floor;
        this.dest_room = dest_room;
    }
}
