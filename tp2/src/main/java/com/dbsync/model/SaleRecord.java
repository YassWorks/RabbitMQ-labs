package com.dbsync.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SaleRecord {
    private int id;
    private String source; // "BO1" ou "BO2"

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate saleDate;

    private String region;
    private String product;
    private int qty;
    private BigDecimal cost;
    private BigDecimal amt;
    private BigDecimal tax;
    private BigDecimal total;

    public SaleRecord() {}

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public BigDecimal getAmt() { return amt; }
    public void setAmt(BigDecimal amt) { this.amt = amt; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    @Override
    public String toString() {
        return "[" + source + "] " + saleDate + " | " + region + " | "
                + product + " | qty=" + qty + " | total=" + total;
    }
}