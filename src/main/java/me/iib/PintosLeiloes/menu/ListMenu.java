package me.iib.PintosLeiloes.menu;

import me.iib.PintosLeiloes.models.ListState;

import java.util.HashMap;
import java.util.Map;

/**
 * Written by iib <iib@realmclub.com>, 16:23 27/10/2026
 */
public class ListMenu extends BaseMenu {
    public final ListState state;
    public final Map<Integer, Integer> ids = new HashMap<>();
    public int pages = 1;

    public ListMenu(ListState s) {
        this.state = s;
    }
}