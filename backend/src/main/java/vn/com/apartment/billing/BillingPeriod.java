package vn.com.apartment.billing;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="billing_periods")
public class BillingPeriod {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=20) private String code;
    @Column(name="period_start",nullable=false) private LocalDate periodStart;
    @Column(name="period_end",nullable=false) private LocalDate periodEnd;
    @Column(name="due_date",nullable=false) private LocalDate dueDate;
    @Column(nullable=false,length=20) private String status="OPEN";
    protected BillingPeriod(){}
    public BillingPeriod(String code,LocalDate start,LocalDate end,LocalDate due){this.code=code;periodStart=start;periodEnd=end;dueDate=due;}
    public Long getId(){return id;} public String getCode(){return code;} public LocalDate getPeriodStart(){return periodStart;} public LocalDate getPeriodEnd(){return periodEnd;} public LocalDate getDueDate(){return dueDate;} public String getStatus(){return status;}
}

