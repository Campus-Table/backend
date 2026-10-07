package com.campustable.campus_table;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusTableApplication {

	public static void main(String[] args) {
		// 서버(UTC 등)와 무관하게 주문 날짜/시간 계산은 한국 시간 기준
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(CampusTableApplication.class, args);
	}

}
