package com.dehigher.biutils.activecontract.dto;

import java.util.List;

public class ActiveContractItem {

    private String updated_at;

    private String chain_id;

    private String chain_name;

    private List<Item> items;

    private Pagination pagination;

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getChain_id() {
        return chain_id;
    }

    public void setChain_id(String chain_id) {
        this.chain_id = chain_id;
    }

    public String getChain_name() {
        return chain_name;
    }

    public void setChain_name(String chain_name) {
        this.chain_name = chain_name;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static class Item {

        private String block_signed_at;

        private String sender_address;

        public String getBlock_signed_at() {
            return block_signed_at;
        }

        public void setBlock_signed_at(String block_signed_at) {
            this.block_signed_at = block_signed_at;
        }

        public String getSender_address() {
            return sender_address;
        }

        public void setSender_address(String sender_address) {
            this.sender_address = sender_address;
        }
    }

    static class Pagination{

        private String hash_more;

        private int page_number;

        private int page_size;

        public String getHash_more() {
            return hash_more;
        }

        public void setHash_more(String hash_more) {
            this.hash_more = hash_more;
        }

        public int getPage_number() {
            return page_number;
        }

        public void setPage_number(int page_number) {
            this.page_number = page_number;
        }

        public int getPage_size() {
            return page_size;
        }

        public void setPage_size(int page_size) {
            this.page_size = page_size;
        }
    }
}
