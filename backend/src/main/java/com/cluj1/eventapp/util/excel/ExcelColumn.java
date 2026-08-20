package com.cluj1.eventapp.util.excel;

import java.util.function.Function;

public record ExcelColumn<T>(String header, Function<T, Object> valueExtractor) {
}