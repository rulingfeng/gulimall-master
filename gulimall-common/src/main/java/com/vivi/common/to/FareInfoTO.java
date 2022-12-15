package com.vivi.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author  
 * 2021/1/21 21:35
 *
 * 运费
 */
@Data
public class FareInfoTO {

    private MemberAddressTO address;

    private BigDecimal fare;
}
