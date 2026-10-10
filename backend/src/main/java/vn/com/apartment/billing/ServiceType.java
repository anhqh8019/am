package vn.com.apartment.billing;
import jakarta.persistence.*;
@Entity @Table(name="service_types")
public class ServiceType{@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;@Column(nullable=false,unique=true,length=30) private String code;@Column(nullable=false,length=150) private String name;@Column(name="billing_method",nullable=false,length=30) private String billingMethod;@Column(nullable=false) private boolean active=true;protected ServiceType(){}public Long getId(){return id;}public String getCode(){return code;}public String getName(){return name;}}

