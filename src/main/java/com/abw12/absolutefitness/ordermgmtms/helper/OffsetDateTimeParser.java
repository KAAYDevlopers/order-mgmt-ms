package com.abw12.absolutefitness.ordermgmtms.helper;

import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public interface OffsetDateTimeParser {

    @Named("offsetDateTimeToString")
    default String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
        return (offsetDateTime == null) ? null : offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Named("stringToOffsetDateTime")
    default OffsetDateTime stringToOffsetDateTime(String value) {
        return (value == null) ? null : OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
