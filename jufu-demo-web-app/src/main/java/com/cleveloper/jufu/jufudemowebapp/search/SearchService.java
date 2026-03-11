package com.cleveloper.jufu.jufudemowebapp.search;

import java.util.List;

public interface SearchService {

    /**
     * Returns all matching hits including full content.
     * Called for premium-tier requests only.
     */
    List<SearchHit> fullSearch(String query);

    /**
     * Returns matching hits with snippet only — no full content.
     * Called for basic-tier requests.
     */
    List<SearchHit> basicSearch(String query);
}
