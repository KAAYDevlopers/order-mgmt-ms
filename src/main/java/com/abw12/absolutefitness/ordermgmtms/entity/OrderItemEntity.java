package com.abw12.absolutefitness.ordermgmtms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orderitem" , schema="ordermgmt")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id")
    private String orderItemId; //(primary key)
    @Column(name = "order_id")
    private String orderId; //(foreign key to OrderEntity)
    @Column(name = "product_name")
    private String productName;
    @Column(name = "variant_id")
    private String variantId;
    @Column(name = "variant_inventory_id")
    private String variantInventoryId;
    private Long quantity;
    @Column(name = "price_per_unit")
    private BigDecimal pricePerUnit;
    @Column(name="order_item_created_at")
    private OffsetDateTime orderItemCreatedAt;
}
