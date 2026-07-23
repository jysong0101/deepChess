package com.deepchess.service_server.service;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum SavedGameSort {
    CREATED_DESC(Sort.by(Sort.Direction.DESC, "createdAt")),
    CREATED_ASC(Sort.by(Sort.Direction.ASC, "createdAt")),
    UPDATED_DESC(Sort.by(Sort.Direction.DESC, "updatedAt")),
    TITLE_ASC(Sort.by(Sort.Direction.ASC, "title")),
    TITLE_DESC(Sort.by(Sort.Direction.DESC, "title"));

    private final Sort sort;

    SavedGameSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort.and(Sort.by(Sort.Direction.ASC, "gameId"));
    }

    public static SavedGameSort parse(String value) {
        try {
            return SavedGameSort.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 방식입니다.");
        }
    }
}
