package com.arenova.dtos;

import com.arenova.dtos.enums.Mode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {

    private Long id;
    private String title;
    private String minCapacity;
    private String maxCapacity;
    private LocalDateTime createdAt;
    private Mode mode;
    private String prizePool;
    private String entry;
    private String description;

}
