package au.com.dmg.terminalposdemo;

import java.util.List;

import au.com.dmg.fusion.data.CustomFieldType;
import au.com.dmg.fusion.request.paymentrequest.CustomField;

public class CustomData {
    String GroupName;
    Integer Quantity;
    List<String> Items;

    public String getGroupName(){
        return GroupName;
    }

    public void setGroupName(String groupName){
        this.GroupName = groupName;
    }

    public Integer getQuantity(){
        return Quantity;
    }

    public void setQuantity(Integer quantity){
        this.Quantity = quantity;
    }

    public List<String> getItems() {
        return Items;
    }

    public void setMessages(List<String> items) {
        this.Items = items;
    }

    public static class Builder {
        String GroupName;
        Integer Quantity;
        List<String> Items;

        public Builder() {
        }

        Builder(String GroupName, Integer Quantity, List<String> Items){
            this.GroupName = GroupName;
            this.Quantity = Quantity;
            this.Items = Items;
        }

        public CustomData.Builder GroupName(String GroupName) {
            this.GroupName = GroupName;
            return CustomData.Builder.this;
        }

        public CustomData.Builder Quantity(Integer Quantity) {
            this.Quantity = Quantity;
            return CustomData.Builder.this;
        }

        public CustomData.Builder Items(List<String> Items) {
            this.Items = Items;
            return CustomData.Builder.this;
        }

        public CustomData build() {
            return new CustomData(this);
        }
    }
    private CustomData(CustomData.Builder builder) {
        this.GroupName = builder.GroupName;
        this.Quantity = builder.Quantity;
        this.Items = builder.Items;
    }

    @Override
    public String toString() {
        return "{\"CustomData\":{" +
                "\"GroupName\":\"" + GroupName + "\"" +
                ", \"Quantity\":" + Quantity +
                ", \"Items\":" + Items +
                "}}";
    }
}
