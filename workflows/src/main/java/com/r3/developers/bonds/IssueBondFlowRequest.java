package com.r3.developers.bonds;

import net.corda.v5.base.types.MemberX500Name;

import java.util.Date;

public class IssueBondFlowRequest {
    private MemberX500Name owner;
    private int amount;
    private int couponRate;
    private Date maturityDate;

    public IssueBondFlowRequest() {}

    public IssueBondFlowRequest(MemberX500Name owner, MemberX500Name issuer, int amount, int couponRate, Date maturityDate){
        this.owner = owner;
        this.amount = amount;
        this.couponRate = couponRate;
        this.maturityDate = maturityDate;
    }

    public MemberX500Name getOwner() {
        return owner;
    }

    public int getAmount() {
        return amount;
    }

    public int getCouponRate() {
        return couponRate;
    }

    public Date getMaturityDate() {
        return maturityDate;
    }
}
