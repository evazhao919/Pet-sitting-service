package com.nashss.se.musicplaylistservice.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter implements DynamoDBTypeConverter<String, LocalDate> {

    @Override
    public String convert(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_INSTANT);
    }

    @Override
        public LocalDate unconvert(String dateRepresentation) {
            return LocalDate.parse(dateRepresentation);
        }

}