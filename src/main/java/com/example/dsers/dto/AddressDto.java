package com.example.dsers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Shopify 的地址对象。
 * <p>
 * shipping_address 和 billing_address 结构完全相同，共用一个类。
 * <p>
 * 注意：这里只接「能寄快递的最小集」，其余字段（latitude/longitude/company 等）
 * 闭环用不到，不接 —— 够用原则。
 */
@Data
public class AddressDto {

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    /** 完整姓名。⚠️ 实测可能为空字符串，需要兜底 */
    private String name;

    /** 电话。国际件必需 */
    private String phone;

    /** 地址1，如街道 */
    private String address1;

    /** 地址2，如门牌/单元 */
    private String address2;

    /** 城市 */
    private String city;

    /** 邮编 */
    private String zip;

    /** 省/州 */
    private String province;

    /** 国家代码，如 CA。用代码不用全名，更可靠 */
    @JsonProperty("country_code")
    private String countryCode;
}
