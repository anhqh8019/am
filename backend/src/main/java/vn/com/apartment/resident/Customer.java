package vn.com.apartment.resident;

import jakarta.persistence.*;

@Entity @Table(name="customers")
public class Customer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="customer_code",nullable=false,unique=true,length=30) private String customerCode;
    @Enumerated(EnumType.STRING) @Column(name="customer_type",nullable=false,length=20) private CustomerType customerType;
    @Column(name="full_name",nullable=false,length=200) private String fullName;
    @Column(length=30) private String phone;
    @Column(length=150) private String email;
    @Column(name="tax_code",length=30) private String taxCode;
    @Column(name="birth_year") private Integer birthYear;
    @Enumerated(EnumType.STRING) @Column(length=20) private Gender gender;
    @Column(name="national_id",length=20) private String nationalId;
    @Column(name="zalo_user_id",length=100) private String zaloUserId;
    @Column(name="telegram_chat_id",length=100) private String telegramChatId;
    @Column(name="zalo_phone",length=30) private String zaloPhone;
    @Column(name="telegram_contact",length=100) private String telegramContact;
    @Column(nullable=false) private boolean active=true;
    protected Customer() {}
    public Customer(String code,CustomerType type,String name,String phone,String email,String taxCode,Integer birthYear,Gender gender,String nationalId,String zaloUserId,String telegramChatId,String zaloPhone,String telegramContact){update(code,type,name,phone,email,taxCode,birthYear,gender,nationalId,zaloUserId,telegramChatId,zaloPhone,telegramContact,true);}
    public void update(String code,CustomerType type,String name,String phone,String email,String taxCode,Integer birthYear,Gender gender,String nationalId,String zaloUserId,String telegramChatId,String zaloPhone,String telegramContact,boolean active){customerCode=code;customerType=type;fullName=name;this.phone=phone;this.email=email;this.taxCode=taxCode;this.birthYear=birthYear;this.gender=gender;this.nationalId=nationalId;this.zaloUserId=zaloUserId;this.telegramChatId=telegramChatId;this.zaloPhone=zaloPhone;this.telegramContact=telegramContact;this.active=active;}
    public void deactivate(){active=false;}
    public Long getId(){return id;} public String getCustomerCode(){return customerCode;} public CustomerType getCustomerType(){return customerType;}
    public String getFullName(){return fullName;} public String getPhone(){return phone;} public String getEmail(){return email;} public String getTaxCode(){return taxCode;}
    public Integer getBirthYear(){return birthYear;} public Gender getGender(){return gender;} public String getNationalId(){return nationalId;} public boolean isActive(){return active;}
    public String getZaloUserId(){return zaloUserId;} public String getTelegramChatId(){return telegramChatId;}
    public String getZaloPhone(){return zaloPhone;} public String getTelegramContact(){return telegramContact;}
}
