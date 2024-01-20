package com.abw12.absolutefitness.ordermgmtms.mappers;

import com.abw12.absolutefitness.ordermgmtms.dto.OrderItemDTO;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderItemEntity;
import com.abw12.absolutefitness.ordermgmtms.helper.OffsetDateTimeParser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderItemsMapper extends OffsetDateTimeParser {

    OrderItemsMapper INSTANCE = Mappers.getMapper(OrderItemsMapper.class);

    @Mapping(source = "orderItemCreatedAt", target = "orderItemCreatedAt", qualifiedByName = "stringToOffsetDateTime")
    OrderItemEntity dtoToEntity(OrderItemDTO requestDTO);
    @Mapping(source = "orderItemCreatedAt", target = "orderItemCreatedAt", qualifiedByName = "offsetDateTimeToString")
    OrderItemDTO entityToDto(OrderItemEntity dbData);

}
