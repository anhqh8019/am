package vn.com.apartment.commercial;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity @Table(name="electricity_readings")
public class ElectricityReading {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="meter_id") private ElectricityMeter meter;
    @Column(name="period_code",nullable=false,length=20) private String periodCode;
    @Column(name="previous_index",nullable=false,precision=18,scale=3) private BigDecimal previousIndex;
    @Column(name="current_index",nullable=false,precision=18,scale=3) private BigDecimal currentIndex;
    @Column(name="unit_price",nullable=false,precision=18,scale=2) private BigDecimal unitPrice;
    @Column(name="reading_date",nullable=false) private LocalDate readingDate;
    @Column(name="image_url",length=500) private String imageUrl;
    @Column(length=300) private String note;
    protected ElectricityReading() {}
    public ElectricityReading(ElectricityMeter meter,String period,BigDecimal previous,BigDecimal current,BigDecimal price,LocalDate date,String imageUrl,String note){
        this.meter=meter;periodCode=period;previousIndex=previous;currentIndex=current;unitPrice=price;readingDate=date;this.imageUrl=imageUrl;this.note=note;
    }
    public BigDecimal getConsumption(){return currentIndex.subtract(previousIndex);}
    public BigDecimal getAmount(){return getConsumption().multiply(unitPrice).setScale(2,RoundingMode.HALF_UP);}
    public Long getId(){return id;} public ElectricityMeter getMeter(){return meter;} public String getPeriodCode(){return periodCode;}
    public BigDecimal getPreviousIndex(){return previousIndex;} public BigDecimal getCurrentIndex(){return currentIndex;} public BigDecimal getUnitPrice(){return unitPrice;}
    public LocalDate getReadingDate(){return readingDate;} public String getImageUrl(){return imageUrl;} public String getNote(){return note;}
}
