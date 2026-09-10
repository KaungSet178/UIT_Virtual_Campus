package com.kaung.uit_virtual_campus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    // Campus wall-clock time, not UTC: LocalDate.now(clock)/LocalTime.now(clock)
    // callers compare against Schedule/Reservation times, which are stored as
    // Myanmar-local wall-clock values (e.g. a 09:40 class means 9:40am in
    // Yangon). Clock.systemUTC() made those comparisons silently wrong by a
    // constant ~6.5hr offset all day, every day — see feature_map_room_filter
    // memory for the room-availability bug this caused. OffsetDateTime-based
    // callers (EventService) are unaffected either way since those comparisons
    // are timezone-independent.
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Yangon"));
    }
}
