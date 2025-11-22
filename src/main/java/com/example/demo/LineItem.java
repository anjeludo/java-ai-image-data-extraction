package com.example.demo;

import java.math.BigDecimal;

public record LineItem(String name, int quantity, BigDecimal price) {
}
