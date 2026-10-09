package com.campustable.campus_table.config;

import com.campustable.campus_table.repository.CafeteriaRepository;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/** SEED_ENABLED=true 이고 식당이 하나도 없을 때만 학식당/가게/메뉴 초기 데이터를 넣는다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    private final CafeteriaRepository cafeteriaRepository;
    private final DataSource dataSource;

    @Value("${app.seed.enabled:false}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || cafeteriaRepository.count() > 0) {
            return;
        }
        var populator = new ResourceDatabasePopulator(new ClassPathResource("seed/seed.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
        log.info("초기 데이터 입력 완료 (학식당/가게/메뉴)");
    }
}
