package com.cs565project.smart.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Database entity to hold categories of apps that user wants to restrict.
 */
@Entity
public class Category {
    @PrimaryKey
    @NonNull
    public String name;

    public boolean shouldRestrict;

    public Category(@NonNull String name, boolean shouldRestrict) {
        this.name = name;
        this.shouldRestrict = shouldRestrict;
    }
}
