package me.iib.PintosLeiloes.models;

import java.util.UUID;

/**
 * Written by iib <iib@realmclub.com>, 16:38 27/10/2026
 */
public class ListState {
    public Mode mode = Mode.MARKET;
    public int page = 0;
    public Category category = Category.ALL;
    public Sort sort = Sort.ENDING;
    public String search = "";
    public UUID target;
    public String targetName;

    public ListState copy() {
        ListState s = new ListState();
        s.mode = mode;
        s.page = page;
        s.category = category;
        s.sort = sort;
        s.search = search;
        s.target = target;
        s.targetName = targetName;
        return s;
    }
}
