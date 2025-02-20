package com.r3.developers.bonds;

import net.corda.v5.base.types.MemberX500Name;

import java.util.UUID;

public class TransferBondFlowRequest {
    private MemberX500Name newOwner;
    private UUID bondId;

    public TransferBondFlowRequest(){};

    public  TransferBondFlowRequest(MemberX500Name newOwner, UUID bondId){
        this.newOwner = newOwner;
        this.bondId = bondId;
    }

    public MemberX500Name getNewOwner() {
        return newOwner;
    }

    public UUID getBondId() {
        return bondId;
    }
}
