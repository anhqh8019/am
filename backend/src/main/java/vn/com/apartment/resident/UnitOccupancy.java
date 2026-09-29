package vn.com.apartment.resident;

import jakarta.persistence.*;
import java.time.LocalDate;
import vn.com.apartment.building.Unit;

@Entity @Table(name="unit_occupancies")
public class UnitOccupancy {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="unit_id") private Unit unit;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id") private Customer customer;
    @Enumerated(EnumType.STRING) @Column(name="occupancy_role",nullable=false,length=20) private OccupancyRole occupancyRole;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date") private LocalDate endDate;
    @Column(name="is_primary",nullable=false) private boolean primary;
    protected UnitOccupancy() {}
    public UnitOccupancy(Unit unit,Customer customer,OccupancyRole role,LocalDate startDate,LocalDate endDate,boolean primary){this.unit=unit;this.customer=customer;occupancyRole=role;this.startDate=startDate;this.endDate=endDate;this.primary=primary;}
    public void removePrimary(){primary=false;} public void end(LocalDate date){endDate=date;primary=false;}
    public Long getId(){return id;} public Unit getUnit(){return unit;} public Customer getCustomer(){return customer;} public OccupancyRole getOccupancyRole(){return occupancyRole;}
    public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public boolean isPrimary(){return primary;}
}
