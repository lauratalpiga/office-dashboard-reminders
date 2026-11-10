package com.example.api.coffee;

public enum CoffeeTask {
    MORNING_START_MACHINE("MORNING", "Switch on the coffee machine and let it rinse"),
    MORNING_FILL_WATER("MORNING", "Empty the water tank and fill it with fresh water from the water dispenser"),
    MORNING_MILK_COOLER("MORNING", "Switch on the milk cooler (button under the flap)"),
    MORNING_INSERT_MILK("MORNING", "Insert the milk container and connect it"),
    EVENING_REMOVE_MILK("EVENING", "Remove the milk container, put the lid on, and store it"),
    EVENING_MILK_COOLER("EVENING", "Switch off the milk cooler (button under the flap)"),
    EVENING_CLEAN_MILK("EVENING", "Start milk system cleaning (clock button)"),
    EVENING_STOP_MASCHINE("EVENING", "Switch off the machine (button under the front flap) and let it rinse"),
    EVENING_EMPTY_CONTAINERS("EVENING", "Empty the grounds container and residual water container and rinse them"),
    BIWEEKLY_CLEANING_TABLET("BIWEEKLY", "Clean the machine with a cleaning tablet (tap the clock icon, then tap the drop icon"),
    BIWEEKLY_EXTERNAL_CLEANING("BIWEEKLY", "Clean the outside of the machine with a damp cloth and dry with a microfiber cloth"),
    BIWEEKLY_CLEAN_MILK_SYSTEM("BIWEEKLY", "Clean the milk outlet (see instructions: “Disassemble and rinse combo outlet”)"),
    OCCASIONAL_CLEAN_BEAN_CONTAINER("OCCASIONAL", "Clean the bean container if it looks dirty (see instructions)"),
    OCCASIONAL_CHANGE_WATER_FILTER("OCCASIONAL", "Change the water filter (machine will notify)"),
    OCCASIONAL_DESCALING("OCCASIONAL", "Descale (machine will notify)"),
    OCCASIONAL_CHANGE_MILK_HOSE("OCCASIONAL", "Replace milk hose and outlet (every 3 months)");

    public final String group;
    public final String label;

    CoffeeTask(String group, String label) {
        this.group = group;
        this.label = label;
    }
}
