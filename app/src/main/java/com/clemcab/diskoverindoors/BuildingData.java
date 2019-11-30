package com.clemcab.diskoverindoors;

// Data Access Object for querying the Building table
public class BuildingData {
    public String alias;
    public String name;
    public int totalFloors;
    public boolean hasLGF; // has Lower Ground Floor
    public float delta; // meter distance between floors
    public float xscale;
    public float yscale;
    public float compassDegreeOffset;

    BuildingData (String alias, String name, int totalFloors, boolean hasLGF, float delta, float xscale, float yscale, float compassDegreeOffset) {
        this.alias = alias;
        this.name = name;
        this.totalFloors = totalFloors;
        this.hasLGF = hasLGF;
        this.delta = delta;
        this.xscale = xscale;
        this.yscale = yscale;
        this.compassDegreeOffset = compassDegreeOffset;
    }

    public String floorNameFromLevel(int level) {
        final String[] names = {
                "Lower Ground Floor",
                "Ground Floor",
                "Second Floor",
                "Third Floor",
                "Fourth Floor",
                "Fifth Floor",
                "Sixth Floor"
        };
        if (this.hasLGF)
            return names[level-1];
        return names[level];
    }
}
