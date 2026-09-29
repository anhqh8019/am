package vn.com.apartment.commercial;

import jakarta.persistence.*;

@Entity @Table(name="commercial_tenants")
public class CommercialTenant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=30) private String code;
    @Column(nullable=false,length=200) private String name;
    @Column(name="tax_code",length=30) private String taxCode;
    @Column(length=150) private String representative;
    @Column(length=30) private String phone;
    @Column(length=150) private String email;
    @Column(name="billing_address",length=300) private String billingAddress;
    @Column(nullable=false) private boolean active=true;
    protected CommercialTenant() {}
    public CommercialTenant(String code,String name,String taxCode,String representative,String phone,String email,String billingAddress){
        this.code=code;this.name=name;this.taxCode=taxCode;this.representative=representative;this.phone=phone;this.email=email;this.billingAddress=billingAddress;
    }
    public Long getId(){return id;} public String getCode(){return code;} public String getName(){return name;}
    public String getTaxCode(){return taxCode;} public String getRepresentative(){return representative;}
    public String getPhone(){return phone;} public String getEmail(){return email;} public String getBillingAddress(){return billingAddress;} public boolean isActive(){return active;}
}
