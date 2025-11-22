package com.example.demo;

import java.math.BigDecimal;
import java.util.List;

public record Receipt(String merchant, BigDecimal total, List<LineItem> lineItems) {
}
