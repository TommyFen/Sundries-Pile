package me.tommy.auto_compiler;

import javax.lang.model.element.Name;

public class ViewInfo {
    private Name name;
    private int id;

    public ViewInfo(Name name, int id) {
        this.name = name;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(Name name) {
        this.name = name;
    }
}