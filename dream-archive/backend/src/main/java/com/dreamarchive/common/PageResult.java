package com.dreamarchive.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer totalPages;
    private List<T> records;

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
        this.totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
    }
}
