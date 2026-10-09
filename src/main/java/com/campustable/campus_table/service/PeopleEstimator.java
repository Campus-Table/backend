package com.campustable.campus_table.service;

import org.springframework.web.multipart.MultipartFile;

/** 이미지 분석 구현체를 교체할 수 있는 경계. */
public interface PeopleEstimator {
    int estimate(MultipartFile image);
}
