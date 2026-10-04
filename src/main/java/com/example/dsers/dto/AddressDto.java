package com.example.dsers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Shopify 地址对象。shipping_address 和 billing_address 结构一样，共用一个类。
 * 只接寄快递的最小集，latitude/longitude/company 这些闭环用不到的不接。
 */
@Data
public class AddressDto {

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    /** 完整姓名。实测可能是空串，用的时候要兜底 */
    private String name;

    /** 电话，国际件必需 */
    private String phone;

    private String address1;

    private String address2;

    private String city;

    private String zip;

    private String province;

    /** 国家代码，如 CA。用代码不用全名，更可靠 */
    @JsonProperty("country_code")
    private String countryCode;
}
