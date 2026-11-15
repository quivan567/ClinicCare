package com.clinic.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "counters")
public class CounterEntity {
    public static final String PRESCRIPTION = "prescription";

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false)
    private long seq;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
}
