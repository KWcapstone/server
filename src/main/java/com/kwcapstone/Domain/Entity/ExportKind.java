package com.kwcapstone.Domain.Entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExportKind {
    MINDMAP("KIND_MINDMAP","MindMap"),
    SUMMARY("KIND_SUMMARY","Summary"),
    RECORDING("KIND_RECORDING","Recording");

    private final String key;
    private final String title;
}
